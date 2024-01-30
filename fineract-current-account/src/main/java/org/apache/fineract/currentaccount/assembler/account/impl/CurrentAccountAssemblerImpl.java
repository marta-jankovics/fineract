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
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowForceTransactionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.balanceCalculationTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.clientIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.identifiersParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minimumRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.productIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.submittedOnDateParamName;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.IdTypeValueSubValueData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.account.EntityAction;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.account.EntityActionType;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.repository.entityaction.EntityActionRepository;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;

@RequiredArgsConstructor
@Slf4j
public class CurrentAccountAssemblerImpl implements CurrentAccountAssembler {

    private final ClientRepository clientRepository;
    private final CurrentProductRepository currentProductRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final EntityActionRepository entityActionRepository;
    private final AccountIdentifierRepository accountIdentifierRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final ExternalIdFactory externalIdFactory;

    private static boolean valueIsEmpty(IdTypeValueSubValueData itemObject) {
        return itemObject.getValue() == null || itemObject.getValue().isEmpty();
    }

    /**
     * Assembles a new {@link CurrentAccount} from JSON details passed in request inheriting details where relevant from
     * chosen {@link org.apache.fineract.currentaccount.domain.product.CurrentProduct}.
     */
    @Override
    public CurrentAccount assemble(final JsonCommand command) {
        String accountNumber = command.stringValueOfParameterNamed(accountNumberParamName);
        final String externalId = command.stringValueOfParameterNamed(externalIdParamName);
        final String productId = command.stringValueOfParameterNamed(productIdParamName);

        final CurrentProduct product = this.currentProductRepository.findById(productId)
                .orElseThrow(() -> new PlatformResourceNotFoundException("current.product",
                        "Current product with provided id: %s cannot be found", productId));

        Client client;
        final Long clientId = command.longValueOfParameterNamed(clientIdParamName);
        if (clientId != null) {
            client = this.clientRepository.findById(clientId).orElseThrow(() -> new ClientNotFoundException(clientId));
            if (client.isNotActive()) {
                throw new ClientNotActiveException(clientId);
            }
        } else {
            throw new ClientNotFoundException(clientId);
        }
        LocalDate submittedOnDate = command.localDateValueOfParameterNamed(submittedOnDateParamName);
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

        BigDecimal minimumRequiredBalance;
        if (command.parameterExists(minimumRequiredBalanceParamName)) {
            minimumRequiredBalance = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minimumRequiredBalanceParamName);
        } else {
            minimumRequiredBalance = product.getMinimumRequiredBalance();
        }

        boolean allowForceTransaction;
        if (command.parameterExists(allowForceTransactionParamName)) {
            allowForceTransaction = command.booleanPrimitiveValueOfParameterNamed(allowForceTransactionParamName);
        } else {
            allowForceTransaction = product.isAllowForceTransaction();
        }

        BalanceCalculationType balanceCalculationType;
        if (command.parameterExists(balanceCalculationTypeParamName)) {
            balanceCalculationType = BalanceCalculationType.valueOf(command.stringValueOfParameterNamed(balanceCalculationTypeParamName));
        } else {
            balanceCalculationType = product.getBalanceCalculationType();
        }

        final CurrentAccount account = CurrentAccount.newInstanceForSubmit(client.getId(), product.getId(), accountNumber,
                externalIdFactory.create(externalId), allowOverdraft, overdraftLimit, allowForceTransaction, minimumRequiredBalance,
                balanceCalculationType);

        validateDates(client, submittedOnDate);
        validateAccountValuesWithProduct(product, account);
        // TODO: Would be better to not flush, but then the exception handlign should be moved to the transaction
        // boundary
        currentAccountRepository.save(account);

        persistEntityAction(account, EntityActionType.SUBMIT, submittedOnDate);
        persistAccountIdentifiers(account, command);

        return account;
    }

    @Override
    public Map<String, Object> update(CurrentAccount account, JsonCommand command) {
        Map<String, Object> actualChanges = new HashMap<>();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.modifyApplicationAction);

        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(accountNumberParamName, account.getAccountNumber())) {
            final String newValue = command.stringValueOfParameterNamed(accountNumberParamName);
            actualChanges.put(accountNumberParamName, newValue);
            account.setAccountNumber(newValue);
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

        if (command.isChangeInBooleanParameterNamed(allowForceTransactionParamName, account.isAllowForceTransaction())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowForceTransactionParamName);
            actualChanges.put(allowForceTransactionParamName, newValue);
            account.setAllowForceTransaction(newValue);
        }

        if (command.isChangeInBigDecimalParameterNamed(minimumRequiredBalanceParamName, account.getMinimumRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(minimumRequiredBalanceParamName);
            actualChanges.put(minimumRequiredBalanceParamName, newValue);
            actualChanges.put(CurrentAccountApiConstants.localeParamName, localeAsInput);
            account.setMinimumRequiredBalance(newValue);
        }

        if (command.isChangeInStringParameterNamed(balanceCalculationTypeParamName, account.getBalanceCalculationType().name())) {
            final String newValue = command.stringValueOfParameterNamed(balanceCalculationTypeParamName);
            actualChanges.put(balanceCalculationTypeParamName, newValue);
            actualChanges.put(CurrentAccountApiConstants.localeParamName, localeAsInput);
            account.setBalanceCalculationType(BalanceCalculationType.valueOf(newValue));
        }

        if (!actualChanges.isEmpty()) {
            if (!account.getStatus().isSubmitted()) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");
            }
            actualChanges.put("locale", localeAsInput);
        }

        updateIdentifiers(account, command, actualChanges);

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        final CurrentProduct product = currentProductRepository.findById(account.getProductId())
                .orElseThrow(() -> new PlatformResourceNotFoundException("current.product",
                        "Current product with provided id: %s cannot be found", account.getProductId()));
        validateAccountValuesWithProduct(product, account);

        if (!actualChanges.isEmpty()) {
            // TODO: Would be better to not flush
            currentAccountRepository.save(account);
        }

        return actualChanges;
    }

    @Override
    public Map<String, Object> cancelApplication(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.cancelAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.SUBMITTED.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        account.setStatus(CurrentAccountStatus.CANCELLED);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toStringEnumOptionData());

        LocalDate cancelledOnDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.actionDateParamName);
        if (cancelledOnDate == null) {
            cancelledOnDate = DateUtils.getBusinessLocalDate();
        }
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());

        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.actionDateParamName, fmt.format(cancelledOnDate));

        final LocalDate submittalDate = fetchSubmittedOnDate(account);
        if (DateUtils.isBefore(cancelledOnDate, submittalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String submittalDateAsString = formatter.format(submittalDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(submittalDateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submittal.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(cancelledOnDate)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(cancelledOnDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        persistEntityAction(account, EntityActionType.CANCEL, cancelledOnDate);
        return actualChanges;
    }

    @Override
    public Map<String, Object> activate(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.activateAction);

        final CurrentAccountStatus currentStatus = account.getStatus();
        if (!CurrentAccountStatus.SUBMITTED.hasStateOf(currentStatus)) {

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submitted.state");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        LocalDate activationDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.actionDateParamName);
        if (activationDate == null) {
            activationDate = DateUtils.getBusinessLocalDate();
        }

        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
        account.setStatus(CurrentAccountStatus.ACTIVE);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toStringEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.actionDateParamName, fmt.format(activationDate));

        account.setActivatedOnDate(activationDate);

        Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(account.getClientId()));
        if (client != null && client.isActivatedAfter(account.getActivatedOnDate())) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
        }

        final LocalDate submittedOnDate = fetchSubmittedOnDate(account);
        if (DateUtils.isBefore(activationDate, submittedOnDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String dateAsString = formatter.format(submittedOnDate);

            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(dateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submitted.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        if (DateUtils.isAfterBusinessDate(activationDate)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(activationDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        persistEntityAction(account, EntityActionType.ACTIVATE, activationDate);

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
        final LocalDate closedDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.actionDateParamName);

        if (DateUtils.isBefore(closedDate, account.getActivatedOnDate())) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(closedDate)
                    .failWithCode("must.be.after.activation.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        if (DateUtils.isAfterBusinessDate(closedDate)) {
            baseDataValidator.reset().parameter(CurrentAccountApiConstants.actionDateParamName).value(closedDate)
                    .failWithCode("cannot.be.a.future.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(account.getId());

        if (currentAccountBalanceData.getAccountBalance().compareTo(BigDecimal.ZERO) != 0
                || currentAccountBalanceData.getHoldAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.account.close.with.balance",
                    "Account cannot be closed. Balance is not 0.", account.getId());
        }

        account.setStatus(CurrentAccountStatus.CLOSED);
        actualChanges.put(CurrentAccountApiConstants.statusParamName, account.getStatus().toStringEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.localeParamName, command.locale());
        actualChanges.put(CurrentAccountApiConstants.dateFormatParamName, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.actionDateParamName, closedDate.format(fmt));

        persistEntityAction(account, EntityActionType.CLOSE, closedDate);

        return actualChanges;
    }

    private void persistEntityAction(CurrentAccount account, EntityActionType actionType, LocalDate actionDate) {
        EntityAction entityAction = new EntityAction(PortfolioProductType.CURRENT, account.getId(), actionType, actionDate);
        entityActionRepository.save(entityAction);
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

    private void validateDates(Client client, LocalDate submittedOnDate) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.submitAction);

        if (DateUtils.isDateInTheFuture(submittedOnDate)) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");
        }

        if (client != null && client.isActivatedAfter(submittedOnDate)) {
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private LocalDate fetchSubmittedOnDate(CurrentAccount account) {
        return entityActionRepository
                .getActionDateByEntityTypeAndEntityIdAndActionType(PortfolioProductType.CURRENT, account.getId(), EntityActionType.SUBMIT)
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.submit.date.is.missing",
                        "Current account submit date is missing"));
    }

    private void persistAccountIdentifiers(CurrentAccount account, JsonCommand command) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);
        if (command.hasParameter(identifiersParamName)) {
            JsonArray identifiers = command.jsonElement(identifiersParamName).getAsJsonArray();

            if (identifiers != null) {
                for (JsonElement itemElement : identifiers) {
                    IdTypeValueSubValueData itemObject = new Gson().fromJson(itemElement, IdTypeValueSubValueData.class);
                    String reformattedKey = itemObject.getIdType().toUpperCase().replace("-", "_");
                    InteropIdentifierType identifierType = InteropIdentifierType.resolveName(reformattedKey);
                    if (identifierType != null) {
                        String value = itemObject.getValue();
                        String subValue = itemObject.getSubValue();
                        AccountIdentifier entityAction = new AccountIdentifier(PortfolioAccountType.CURRENT, account.getId(),
                                identifierType, value, subValue, 1L);
                        // TODO: would be better to not flush, but then the error handling should be moved to
                        // the transaction boundary
                        accountIdentifierRepository.save(entityAction);
                    } else {
                        baseDataValidator.reset().parameter(itemObject.getIdType())
                                .failWithCodeNoParameterAddedToErrorCode("unknown.identifier.found");
                    }
                }
            }
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void updateIdentifiers(CurrentAccount account, JsonCommand command, Map<String, Object> actualChanges) {
        if (command.hasParameter(identifiersParamName)) {
            actualChanges.computeIfAbsent(identifiersParamName, k -> new ArrayList<>());
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors);
            Map<InteropIdentifierType, AccountIdentifier> secondaryIdentifiers = accountIdentifierRepository
                    .retrieveAccountIdentifiers(PortfolioAccountType.CURRENT, account.getId()).stream()
                    .collect(Collectors.toMap(AccountIdentifier::getIdentifierType, Function.identity()));
            JsonArray identifiers = command.jsonElement(identifiersParamName).getAsJsonArray();

            if (identifiers != null) {
                for (JsonElement itemElement : identifiers) {
                    IdTypeValueSubValueData itemObject = new Gson().fromJson(itemElement, IdTypeValueSubValueData.class);
                    String reformattedKey = itemObject.getIdType().toUpperCase().replace("-", "_");
                    InteropIdentifierType identifierType = InteropIdentifierType.resolveName(reformattedKey);
                    if (identifierType != null) {
                        AccountIdentifier accountIdentifier;
                        if (secondaryIdentifiers.containsKey(identifierType)) {
                            accountIdentifier = secondaryIdentifiers.get(identifierType);
                            if (valueIsEmpty(itemObject)) {
                                accountIdentifierRepository.delete(accountIdentifier);
                            } else {
                                accountIdentifier.setValue(itemObject.getValue());
                                accountIdentifier.setSubValue(itemObject.getSubValue());
                            }
                        } else {
                            if (valueIsEmpty(itemObject)) {
                                baseDataValidator.reset().parameter(itemObject.getIdType())
                                        .failWithCodeNoParameterAddedToErrorCode("value.must.be.set");
                            } else {
                                accountIdentifier = new AccountIdentifier(PortfolioAccountType.CURRENT, account.getId(), identifierType,
                                        itemObject.getValue(), itemObject.getSubValue(), 1L);
                                accountIdentifierRepository.save(accountIdentifier);
                            }
                        }
                    } else {
                        baseDataValidator.reset().parameter(itemObject.getIdType())
                                .failWithCodeNoParameterAddedToErrorCode("unknown.identifier.found");
                    }

                    ((List) actualChanges.get(identifiersParamName)).add(itemObject);
                }
                if (!dataValidationErrors.isEmpty()) {
                    throw new PlatformApiDataValidationException(dataValidationErrors);
                }
            }
        }
    }
}
