/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.currentaccount.assembler.account.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountNoParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.clientIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceMinRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.productIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.submittedOnDateParamName;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enums.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.exception.product.CurrentProductNotFoundException;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;

@RequiredArgsConstructor
@Slf4j
public class CurrentAccountAssemblerImpl implements CurrentAccountAssembler {

    private final PlatformSecurityContext context;
    private final FromJsonHelper fromApiJsonHelper;
    private final ClientRepository clientRepository;
    private final CurrentProductRepository currentProductRepository;
    private final ExternalIdFactory externalIdFactory;

    /**
     * Assembles a new {@link CurrentAccount} from JSON details passed in request inheriting details where relevant from
     * chosen {@link org.apache.fineract.currentaccount.domain.product.CurrentProduct}.
     */
    @Override
    public CurrentAccount assemble(final JsonCommand command) {

        final JsonElement element = command.parsedJson();

        String accountNo = this.fromApiJsonHelper.extractStringNamed(accountNoParamName, element);

        if (accountNo == null) {
            // TODO: enhance with the account number generator
            accountNo = UUID.randomUUID().toString();
        }

        final String externalId = this.fromApiJsonHelper.extractStringNamed(externalIdParamName, element);
        final Long productId = this.fromApiJsonHelper.extractLongNamed(productIdParamName, element);

        final CurrentProduct product = this.currentProductRepository.findById(productId)
                .orElseThrow(() -> new CurrentProductNotFoundException(productId));

        AccountType accountType;
        Client client;
        final Long clientId = this.fromApiJsonHelper.extractLongNamed(clientIdParamName, element);
        if (clientId != null) {
            client = this.clientRepository.findById(clientId).orElseThrow(() -> new ClientNotFoundException(clientId));
            accountType = AccountType.INDIVIDUAL;
            if (client.isNotActive()) {
                throw new ClientNotActiveException(clientId);
            }
        } else {
            throw new ClientNotFoundException(clientId);
        }
        LocalDate submittedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(submittedOnDateParamName, element);
        if (submittedOnDate == null) {
            submittedOnDate = DateUtils.getBusinessLocalDate();
        }

        boolean allowOverdraft;
        if (command.parameterExists(allowOverdraftParamName)) {
            allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        } else {
            allowOverdraft = product.isAllowOverdraft();
        }

        BigDecimal overdraftLimit;
        if (command.parameterExists(overdraftLimitParamName)) {
            overdraftLimit = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(overdraftLimitParamName);
        } else {
            overdraftLimit = product.getOverdraftLimit();
        }

        boolean enforceMinRequiredBalance;
        if (command.parameterExists(enforceMinRequiredBalanceParamName)) {
            enforceMinRequiredBalance = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
        } else {
            enforceMinRequiredBalance = product.isEnforceMinRequiredBalance();
        }

        BigDecimal minRequiredBalance;
        if (command.parameterExists(minRequiredBalanceParamName)) {
            minRequiredBalance = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredBalanceParamName);
        } else {
            minRequiredBalance = product.getMinRequiredBalance();
        }

        final CurrentAccount account = CurrentAccount.newInstanceForSubmit(client.getId(), product.getId(), accountNo,
                product.getCurrency(), externalIdFactory.create(externalId), accountType, submittedOnDate,
                context.authenticatedUser().getId(), allowOverdraft, overdraftLimit, enforceMinRequiredBalance, minRequiredBalance);

        validateDates(client, account);
        validateAccountValuesWithProduct(product, account);

        return account;
    }

    @Override
    public Map<String, Object> update(CurrentAccount account, JsonCommand command) {
        Map<String, Object> actualChanges = new HashMap<>();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.modifyApplicationAction);

        final String localeAsInput = command.locale();
        final String dateFormat = command.dateFormat();

        if (command.isChangeInLocalDateParameterNamed(submittedOnDateParamName, account.getSubmittedOnDate())) {
            final String newValueAsString = command.stringValueOfParameterNamed(submittedOnDateParamName);
            actualChanges.put(submittedOnDateParamName, newValueAsString);
            actualChanges.put(CurrentAccountApiConstants.localeParamName, localeAsInput);
            actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, dateFormat);
            account.setSubmittedOnDate(command.localDateValueOfParameterNamed(submittedOnDateParamName));
        }
        if (command.isChangeInStringParameterNamed(accountNoParamName, account.getAccountNo())) {
            final String newValue = command.stringValueOfParameterNamed(accountNoParamName);
            actualChanges.put(accountNoParamName, newValue);
            account.setAccountNo(newValue);
        }
        if (command.isChangeInStringParameterNamed(externalIdParamName, account.getExternalId().getValue())) {
            final String newValue = command.stringValueOfParameterNamed(externalIdParamName);
            actualChanges.put(externalIdParamName, newValue);
            account.setExternalId(externalIdFactory.create(newValue));
        }

        if (command.isChangeInBooleanParameterNamed(allowOverdraftParamName, account.isAllowOverdraft())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            actualChanges.put(allowOverdraftParamName, newValue);
            account.setAllowOverdraft(newValue);
            if (!newValue) {
                account.setOverdraftLimit(null);
            }
        }
        if (account.isAllowOverdraft()
                && command.isChangeInBigDecimalParameterNamed(overdraftLimitParamName, account.getOverdraftLimit())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
            actualChanges.put(overdraftLimitParamName, newValue);
            actualChanges.put(CurrentAccountApiConstants.localeParamName, localeAsInput);
            account.setOverdraftLimit(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(enforceMinRequiredBalanceParamName, account.isEnforceMinRequiredBalance())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
            actualChanges.put(enforceMinRequiredBalanceParamName, newValue);
            account.setEnforceMinRequiredBalance(newValue);
        }
        if (account.isEnforceMinRequiredBalance() && command
                .isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minRequiredBalanceParamName, account.getMinRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredBalanceParamName);
            actualChanges.put(minRequiredBalanceParamName, newValue);
            actualChanges.put(CurrentAccountApiConstants.localeParamName, localeAsInput);
            account.setMinRequiredBalance(newValue);
        }

        if (!actualChanges.isEmpty()) {
            if (!account.getStatus().isSubmittedAndPendingApproval()) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");
            }
            actualChanges.put("locale", localeAsInput);
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        final Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(account.getClientId()));
        validateDates(client, account);
        final CurrentProduct product = currentProductRepository.findById(account.getProductId())
                .orElseThrow(() -> new CurrentProductNotFoundException(account.getProductId()));
        validateAccountValuesWithProduct(product, account);

        return actualChanges;
    }

    @Override
    public Map<String, Object> approve(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.approveAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL.hasStateOf(currentStatus)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.approvedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        account.setStatus(CurrentAccountStatus.APPROVED);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());

        // only do below if status has changed in the 'approval' case
        LocalDate approvedOn = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.approvedOnDateParamName);
        if (approvedOn == null) {
            approvedOn = DateUtils.getBusinessLocalDate();
        }
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());

        account.setApprovedOnDate(approvedOn);
        account.setApprovedByUserId(context.authenticatedUser().getId());
        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.approvedOnDateParamName, fmt.format(approvedOn));

        final LocalDate submittalDate = account.getSubmittedOnDate();
        if (DateUtils.isBefore(approvedOn, submittalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String submittalDateAsString = formatter.format(submittalDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.approvedOnDateParamName).value(submittalDateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submittal.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(approvedOn)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.approvedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return actualChanges;
    }

    @Override
    public Map<String, Object> undoApplicationApproval(CurrentAccount account) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.undoApprovalAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.APPROVED.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.approvedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.approved.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        account.setStatus(CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL);
        account.setApprovedByUserId(null);
        account.setApprovedOnDate(null);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.approvedOnDateParamName, "");

        return actualChanges;
    }

    @Override
    public Map<String, Object> rejectApplication(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.rejectAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.rejectedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        account.setStatus(CurrentAccountStatus.REJECTED);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());

        LocalDate rejectedOn = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.rejectedOnDateParamName);
        if (rejectedOn == null) {
            rejectedOn = DateUtils.getBusinessLocalDate();
        }
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());

        account.setRejectedOnDate(rejectedOn);
        account.setRejectedByUserId(context.authenticatedUser().getId());
        account.setClosedOnDate(rejectedOn);
        account.setClosedByUserId(context.authenticatedUser().getId());

        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.rejectedOnDateParamName, fmt.format(rejectedOn));
        actualChanges.put(CurrentAccountApiConstants.closedOnDateParamName, fmt.format(rejectedOn));

        final LocalDate submittalDate = account.getSubmittedOnDate();
        if (DateUtils.isBefore(rejectedOn, submittalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String submittalDateAsString = formatter.format(submittalDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.rejectedOnDateParamName).value(submittalDateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submittal.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(rejectedOn)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.rejectedOnDateParamName).value(rejectedOn)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return actualChanges;
    }

    @Override
    public Map<String, Object> withdrawApplication(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.withdrawnByApplicantAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.withdrawnOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        account.setStatus(CurrentAccountStatus.WITHDRAWN_BY_APPLICANT);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());

        LocalDate withdrawnOn = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.withdrawnOnDateParamName);
        if (withdrawnOn == null) {
            withdrawnOn = DateUtils.getBusinessLocalDate();
        }
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());

        account.setWithdrawnOnDate(withdrawnOn);
        account.setWithdrawnByUserId(context.authenticatedUser().getId());
        account.setClosedOnDate(withdrawnOn);
        account.setClosedByUserId(context.authenticatedUser().getId());

        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.withdrawnOnDateParamName, fmt.format(withdrawnOn));
        actualChanges.put(CurrentAccountApiConstants.closedOnDateParamName, fmt.format(withdrawnOn));

        final LocalDate submittalDate = account.getSubmittedOnDate();
        if (DateUtils.isBefore(withdrawnOn, submittalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String submittalDateAsString = formatter.format(submittalDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.withdrawnOnDateParamName).value(submittalDateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submittal.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(withdrawnOn)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.withdrawnOnDateParamName).value(withdrawnOn)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return actualChanges;
    }

    @Override
    public Map<String, Object> activate(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.activateAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.APPROVED.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.activatedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.approved.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        LocalDate activationDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.activatedOnDateParamName);
        if (activationDate == null) {
            activationDate = DateUtils.getBusinessLocalDate();
        }

        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
        account.setStatus(CurrentAccountStatus.ACTIVE);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.activatedOnDateParamName, fmt.format(activationDate));

        account.setActivatedOnDate(activationDate);
        account.setActivatedByUserId(context.authenticatedUser().getId());

        Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(account.getClientId()));
        if (client != null && client.isActivatedAfter(account.getActivatedOnDate())) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
        }

        final LocalDate approvalDate = account.getApprovedOnDate();
        if (DateUtils.isBefore(activationDate, approvalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String dateAsString = formatter.format(approvalDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.activatedOnDateParamName).value(dateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.approval.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        if (DateUtils.isAfterBusinessDate(activationDate)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.activatedOnDateParamName).value(activationDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return actualChanges;
    }

    @Override
    public Map<String, Object> close(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.closeAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.ACTIVE.hasStateOf(currentStatus)) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.active.state");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        final LocalDate closedDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.closedOnDateParamName);

        if (DateUtils.isBefore(closedDate, account.getActivatedOnDate())) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("must.be.after.activation.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(closedDate)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("cannot.be.a.future.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        // TODO: check balance

        account.setStatus(CurrentAccountStatus.CLOSED);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.closedOnDateParamName, closedDate.format(fmt));

        account.setClosedOnDate(closedDate);
        account.setClosedByUserId(context.authenticatedUser().getId());

        return actualChanges;
    }

    private void validateAccountValuesWithProduct(CurrentProduct product, CurrentAccount account) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        if (account.getOverdraftLimit() != null && product.getOverdraftLimit() != null
                && account.getOverdraftLimit().compareTo(product.getOverdraftLimit()) > 0) {
            baseDataValidator.reset().parameter(overdraftLimitParamName).value(account.getOverdraftLimit())
                    .parameter("overDraftLimitOnProduct").value(product.getOverdraftLimit()).failWithCode("cannot.exceed.product.value");
        }
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void validateDates(Client client, CurrentAccount account) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.submitAction);
        LocalDate submittedOn = account.getSubmittedOnDate();
        if (DateUtils.isDateInTheFuture(account.getSubmittedOnDate())) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOn)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");
        }

        if (client != null && client.isActivatedAfter(submittedOn)) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
