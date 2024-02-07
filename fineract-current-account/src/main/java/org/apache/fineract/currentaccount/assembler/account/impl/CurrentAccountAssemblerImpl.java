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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_NUMBER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_FORCE_TRANSACTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_OVERDRAFT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.BALANCE_CALCULATION_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CLIENT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIERS_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_VALUE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.MINIMUM_REQUIRED_BALANCE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_LIMIT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PRODUCT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SUBMITTED_ON_DATE_PARAM;
import static org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType.STRICT;
import static org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants.DATATABLES_PARAM;

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
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.account.EntityActionType;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.repository.entityaction.EntityActionRepository;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.currentaccount.service.common.IdTypeResolver;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
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
    private final CurrentAccountBalanceWriteService currentAccountBalanceWriteService;
    private final ExternalIdFactory externalIdFactory;
    ReadWriteNonCoreDataService readWriteNonCoreDataService;

    /**
     * Assembles a new {@link CurrentAccount} from JSON details passed in request inheriting details where relevant from
     * chosen {@link org.apache.fineract.currentaccount.domain.product.CurrentProduct}.
     */
    @Override
    public CurrentAccount assemble(final JsonCommand command) {
        String accountNumber = command.stringValueOfParameterNamedAllowingNull(ACCOUNT_NUMBER_PARAM);
        final String externalId = command.stringValueOfParameterNamedAllowingNull(EXTERNAL_ID_PARAM);
        final String productId = command.stringValueOfParameterNamedAllowingNull(PRODUCT_ID_PARAM);

        final CurrentProduct product = this.currentProductRepository.findById(productId)
                .orElseThrow(() -> new PlatformResourceNotFoundException("current.product",
                        "Current product with provided id: %s cannot be found", productId));

        Client client;
        final Long clientId = command.longValueOfParameterNamed(CLIENT_ID_PARAM);
        if (clientId != null) {
            client = this.clientRepository.findById(clientId).orElseThrow(() -> new ClientNotFoundException(clientId));
            if (client.isNotActive()) {
                throw new ClientNotActiveException(clientId);
            }
        } else {
            throw new ClientNotFoundException(clientId);
        }
        LocalDate submittedOnDate = command.localDateValueOfParameterNamed(SUBMITTED_ON_DATE_PARAM);
        if (submittedOnDate == null) {
            submittedOnDate = DateUtils.getBusinessLocalDate();
        }

        boolean allowOverdraft;
        if (command.parameterExists(ALLOW_OVERDRAFT_PARAM)) {
            allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
        } else {
            allowOverdraft = product.isAllowOverdraft();
        }

        BigDecimal overdraftLimit;
        if (command.parameterExists(OVERDRAFT_LIMIT_PARAM)) {
            overdraftLimit = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(OVERDRAFT_LIMIT_PARAM);
        } else {
            overdraftLimit = product.getOverdraftLimit();
        }

        BigDecimal minimumRequiredBalance;
        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            minimumRequiredBalance = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(MINIMUM_REQUIRED_BALANCE_PARAM);
        } else {
            minimumRequiredBalance = product.getMinimumRequiredBalance();
        }

        boolean allowForceTransaction;
        if (command.parameterExists(ALLOW_FORCE_TRANSACTION_PARAM)) {
            allowForceTransaction = command.booleanPrimitiveValueOfParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM);
        } else {
            allowForceTransaction = product.isAllowForceTransaction();
        }

        BalanceCalculationType balanceCalculationType;
        if (command.parameterExists(BALANCE_CALCULATION_TYPE_PARAM)) {
            balanceCalculationType = BalanceCalculationType
                    .valueOf(command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM));
        } else {
            balanceCalculationType = product.getBalanceCalculationType();
        }

        CurrentAccount account = CurrentAccount.newInstanceForSubmit(accountNumber, externalIdFactory.create(externalId), client.getId(),
                product.getId(), allowOverdraft, overdraftLimit, allowForceTransaction, minimumRequiredBalance, balanceCalculationType);

        validateDates(client, submittedOnDate);
        validateAccountValuesWithProduct(product, account);

        JsonArray datatables = command.arrayOfParameterNamed(DATATABLES_PARAM);
        if (datatables != null && !datatables.isEmpty()) {
            account = currentAccountRepository.saveAndFlush(account);
            persistDatatableEntries(EntityTables.CURRENT, account.getId(), datatables, false, readWriteNonCoreDataService);
        } else {
            account = currentAccountRepository.save(account);
        }

        persistEntityAction(account, EntityActionType.SUBMIT, submittedOnDate);
        persistAccountIdentifiers(account, command);
        return account;
    }

    @Override
    public Map<String, Object> update(CurrentAccount account, JsonCommand command) {
        Map<String, Object> actualChanges = new HashMap<>();
        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.MODIFY_ACTION);

        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(ACCOUNT_NUMBER_PARAM, account.getAccountNumber())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(ACCOUNT_NUMBER_PARAM);
            actualChanges.put(ACCOUNT_NUMBER_PARAM, newValue);
            account.setAccountNumber(newValue);
        }
        if (command.isChangeInStringParameterNamed(EXTERNAL_ID_PARAM, account.getExternalId().getValue())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(EXTERNAL_ID_PARAM);
            actualChanges.put(EXTERNAL_ID_PARAM, newValue);
            account.setExternalId(externalIdFactory.create(newValue));
        }

        if (command.isChangeInBooleanParameterNamed(ALLOW_OVERDRAFT_PARAM, account.isAllowOverdraft())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
            actualChanges.put(ALLOW_OVERDRAFT_PARAM, newValue);
            account.setAllowOverdraft(newValue);
            if (!newValue) {
                account.setOverdraftLimit(null);
            }
        }

        if (account.isAllowOverdraft() && command.isChangeInBigDecimalParameterNamed(OVERDRAFT_LIMIT_PARAM, account.getOverdraftLimit())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
            actualChanges.put(OVERDRAFT_LIMIT_PARAM, newValue);
            actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, localeAsInput);
            account.setOverdraftLimit(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM, account.isAllowForceTransaction())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM);
            actualChanges.put(ALLOW_FORCE_TRANSACTION_PARAM, newValue);
            account.setAllowForceTransaction(newValue);
        }

        if (command.isChangeInBigDecimalParameterNamed(MINIMUM_REQUIRED_BALANCE_PARAM, account.getMinimumRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(MINIMUM_REQUIRED_BALANCE_PARAM);
            actualChanges.put(MINIMUM_REQUIRED_BALANCE_PARAM, newValue);
            actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, localeAsInput);
            account.setMinimumRequiredBalance(newValue);
        }

        if (command.isChangeInStringParameterNamed(BALANCE_CALCULATION_TYPE_PARAM, account.getBalanceCalculationType().name())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM);
            actualChanges.put(BALANCE_CALCULATION_TYPE_PARAM, newValue);
            actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, localeAsInput);
            account.setBalanceCalculationType(BalanceCalculationType.valueOf(newValue));
        }

        if (!actualChanges.isEmpty()) {
            if (!account.getStatus().isSubmitted()) {
                dataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");
            }
            actualChanges.put("locale", localeAsInput);
        }

        updateIdentifiers(account, command, actualChanges);

        dataValidator.throwValidationErrors();

        final CurrentProduct product = currentProductRepository.findById(account.getProductId())
                .orElseThrow(() -> new PlatformResourceNotFoundException("current.product",
                        "Current product with provided id: %s cannot be found", account.getProductId()));
        validateAccountValuesWithProduct(product, account);

        if (!actualChanges.isEmpty()) {
            currentAccountRepository.save(account);
        }

        JsonArray datatables = command.arrayOfParameterNamed(DATATABLES_PARAM);
        if (datatables != null && !datatables.isEmpty()) {
            Map<String, Object> datatableChanges = persistDatatableEntries(EntityTables.CURRENT, account.getId(), datatables, true,
                    readWriteNonCoreDataService);
            if (datatableChanges != null && !datatableChanges.isEmpty()) {
                actualChanges.put(DATATABLES_PARAM, datatableChanges);
            }
        }
        return actualChanges;
    }

    @Override
    public Map<String, Object> cancelApplication(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.CANCEL_ACTION);

        account.setStatus(CurrentAccountStatus.CANCELLED);
        actualChanges.put(CurrentAccountApiConstants.STATUS_PARAM, account.getStatus().toStringEnumOptionData());

        LocalDate cancelledOnDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.ACTION_DATE_PARAM);
        // TODO CURRENT! check if cancelledOnDate is not earlier than submitted date
        if (cancelledOnDate == null) {
            cancelledOnDate = DateUtils.getBusinessLocalDate();
        }
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());

        actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, command.locale());
        actualChanges.put(CurrentAccountApiConstants.DATE_FORMAT_PARAM, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.ACTION_DATE_PARAM, fmt.format(cancelledOnDate));

        final LocalDate submittalDate = fetchSubmittedOnDate(account);
        if (DateUtils.isBefore(cancelledOnDate, submittalDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String submittalDateAsString = formatter.format(submittalDate);

            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(submittalDateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submittal.date");
            dataValidator.throwValidationErrors();
        }
        if (DateUtils.isAfterBusinessDate(cancelledOnDate)) {
            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(cancelledOnDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");
            dataValidator.throwValidationErrors();
        }
        persistEntityAction(account, EntityActionType.CANCEL, cancelledOnDate);
        return actualChanges;
    }

    @Override
    public Map<String, Object> activate(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.ACTIVATE_ACTION);

        LocalDate activationDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.ACTION_DATE_PARAM);
        // TODO CURRENT! check if activationDate is not earlier than submitted date
        if (activationDate == null) {
            activationDate = DateUtils.getBusinessLocalDate();
        }

        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
        account.setStatus(CurrentAccountStatus.ACTIVE);
        actualChanges.put(CurrentAccountApiConstants.STATUS_PARAM, account.getStatus().toStringEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, command.locale());
        actualChanges.put(CurrentAccountApiConstants.DATE_FORMAT_PARAM, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.ACTION_DATE_PARAM, fmt.format(activationDate));

        account.setActivatedOnDate(activationDate);

        Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(account.getClientId()));
        if (client != null && client.isActivatedAfter(account.getActivatedOnDate())) {
            dataValidator.reset().parameter(SUBMITTED_ON_DATE_PARAM).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
            dataValidator.throwValidationErrors();
        }

        final LocalDate submittedOnDate = fetchSubmittedOnDate(account);
        if (DateUtils.isBefore(activationDate, submittedOnDate)) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(command.extractLocale());
            final String dateAsString = formatter.format(submittedOnDate);

            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(dateAsString)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.submitted.date");
            dataValidator.throwValidationErrors();
        }

        if (DateUtils.isAfterBusinessDate(activationDate)) {
            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(activationDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");
            dataValidator.throwValidationErrors();
        }

        persistEntityAction(account, EntityActionType.ACTIVATE, activationDate);

        return actualChanges;
    }

    @Override
    public Map<String, Object> close(CurrentAccount account, JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.CLOSE_ACTION);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(command.dateFormat()).withLocale(locale);
        final LocalDate closedDate = command.localDateValueOfParameterNamed(CurrentAccountApiConstants.ACTION_DATE_PARAM);

        if (DateUtils.isBefore(closedDate, account.getActivatedOnDate())) {
            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(closedDate)
                    .failWithCode("must.be.after.activation.date");
            dataValidator.throwValidationErrors();
        }
        if (DateUtils.isAfterBusinessDate(closedDate)) {
            dataValidator.reset().parameter(CurrentAccountApiConstants.ACTION_DATE_PARAM).value(closedDate)
                    .failWithCode("cannot.be.a.future.date");
            dataValidator.throwValidationErrors();
        }
        // TODO CURRENT! pessimistic lock if not strict, but what?
        CurrentAccountAction action = CurrentAccountAction.forActionName(ThreadLocalContextUtil.getCommandAction());
        CurrentAccountBalanceData balanceData = currentAccountBalanceWriteService.calculateBalance(account.getId(), STRICT, action, null);

        if (!MathUtil.isEmpty(balanceData.getAccountBalance()) || !MathUtil.isEmpty(balanceData.getHoldAmount())) {
            throw new GeneralPlatformDomainRuleException("error.msg.account.close.with.balance",
                    "Account cannot be closed. Balance is not 0.", account.getId());
        }

        account.setStatus(CurrentAccountStatus.CLOSED);
        actualChanges.put(CurrentAccountApiConstants.STATUS_PARAM, account.getStatus().toStringEnumOptionData());
        actualChanges.put(CurrentAccountApiConstants.LOCALE_PARAM, command.locale());
        actualChanges.put(CurrentAccountApiConstants.DATE_FORMAT_PARAM, command.dateFormat());
        actualChanges.put(CurrentAccountApiConstants.ACTION_DATE_PARAM, closedDate.format(fmt));

        persistEntityAction(account, EntityActionType.CLOSE, closedDate);

        return actualChanges;
    }

    private void persistEntityAction(CurrentAccount account, EntityActionType actionType, LocalDate actionDate) {
        EntityAction entityAction = new EntityAction(PortfolioProductType.CURRENT, account.getId(), actionType, actionDate);
        entityActionRepository.save(entityAction);
    }

    private void validateAccountValuesWithProduct(CurrentProduct product, CurrentAccount account) {
        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        if (product.getOverdraftLimit() != null && MathUtil.isGreaterThan(account.getOverdraftLimit(), product.getOverdraftLimit())) {
            dataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(account.getOverdraftLimit()).parameter("overDraftLimitOnProduct")
                    .value(product.getOverdraftLimit()).failWithCode("cannot.exceed.product.value");
        }
        dataValidator.throwValidationErrors();
    }

    private void validateDates(Client client, LocalDate submittedOnDate) {
        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME + CurrentAccountApiConstants.CREATE_ACTION);

        if (DateUtils.isDateInTheFuture(submittedOnDate)) {
            dataValidator.reset().parameter(SUBMITTED_ON_DATE_PARAM).value(submittedOnDate)
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.a.future.date");
        }

        if (client != null && client.isActivatedAfter(submittedOnDate)) {
            dataValidator.reset().parameter(SUBMITTED_ON_DATE_PARAM).value(client.getActivationDate())
                    .failWithCodeNoParameterAddedToErrorCode("cannot.be.before.client.activation.date");
        }

        dataValidator.throwValidationErrors();
    }

    private LocalDate fetchSubmittedOnDate(CurrentAccount account) {
        return entityActionRepository
                .getActionDateByEntityTypeAndEntityIdAndActionType(PortfolioProductType.CURRENT, account.getId(), EntityActionType.SUBMIT)
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.submit.date.is.missing",
                        "Current account submit date is missing"));
    }

    private void persistAccountIdentifiers(CurrentAccount account, JsonCommand command) {
        // no separate identifiers permission
        if (!command.hasParameter(IDENTIFIERS_PARAM)) {
            return;
        }
        final DataValidatorBuilder validator = new DataValidatorBuilder().resource(CURRENT_ACCOUNT_RESOURCE_NAME);
        JsonArray identifierArray = command.jsonElement(IDENTIFIERS_PARAM).getAsJsonArray();
        if (identifierArray == null) {
            return;
        }
        ArrayList<AccountIdentifier> identifiers = new ArrayList<>();
        for (JsonElement identifierElement : identifierArray) {
            IdTypeValueSubValueData identifierObject = new Gson().fromJson(identifierElement, IdTypeValueSubValueData.class);
            String idType = identifierObject.getIdType();
            InteropIdentifierType identifierType = InteropIdentifierType.resolveName(IdTypeResolver.formatIdType(idType));
            if (identifierType == null) {
                validator.reset().parameter(ID_TYPE_PARAM).value(idType).failWithCode("unknown.identifier");
                continue;
            }
            String value = identifierObject.getValue();
            validator.reset().parameter(ID_VALUE_PARAM).value(value).notBlank();

            String subValue = identifierObject.getSubValue();
            identifiers.add(new AccountIdentifier(PortfolioAccountType.CURRENT, account.getId(), identifierType, value, subValue));
        }
        validator.throwValidationErrors();

        accountIdentifierRepository.saveAll(identifiers);
    }

    private void updateIdentifiers(CurrentAccount account, JsonCommand command, Map<String, Object> actualChanges) {
        if (!command.hasParameter(IDENTIFIERS_PARAM)) {
            return;
        }
        actualChanges.computeIfAbsent(IDENTIFIERS_PARAM, k -> new ArrayList<>());
        final DataValidatorBuilder validator = new DataValidatorBuilder().resource(CURRENT_ACCOUNT_RESOURCE_NAME);
        JsonArray identifierArray = command.jsonElement(IDENTIFIERS_PARAM).getAsJsonArray();
        if (identifierArray == null) {
            return;
        }

        Map<IdTypeValueSubValueData, AccountIdentifier> existingIdentifiers = accountIdentifierRepository
                .retrieveAccountIdentifiers(PortfolioAccountType.CURRENT, account.getId()).stream()
                .collect(Collectors.toMap(ai -> new IdTypeValueSubValueData(ai.getIdentifierType().name(), ai.getValue(), ai.getSubValue()),
                        Function.identity()));

        List<AccountIdentifier> persistedIdentifiers = new ArrayList<>();
        for (JsonElement identifierElement : identifierArray) {
            IdTypeValueSubValueData identifierObject = new Gson().fromJson(identifierElement, IdTypeValueSubValueData.class);
            String idType = identifierObject.getIdType();
            InteropIdentifierType identifierType = InteropIdentifierType.resolveName(IdTypeResolver.formatIdType(idType));
            if (identifierType == null) {
                validator.reset().parameter(ID_TYPE_PARAM).value(idType).failWithCode("unknown.identifier");
                continue;
            }
            String value = identifierObject.getValue();
            AccountIdentifier accountIdentifier;
            if (existingIdentifiers.containsKey(identifierObject)) {
                accountIdentifier = existingIdentifiers.get(identifierObject);
                validator.reset().parameter(ID_VALUE_PARAM).value(value).notBlank();
                accountIdentifier.setValue(value);
                accountIdentifier.setSubValue(identifierObject.getSubValue());
                persistedIdentifiers.add(accountIdentifier);
                existingIdentifiers.remove(identifierObject);
            } else {
                validator.reset().parameter(ID_VALUE_PARAM).value(value).notBlank();
                AccountIdentifier savedIdentifier = accountIdentifierRepository.save(new AccountIdentifier(PortfolioAccountType.CURRENT,
                        account.getId(), identifierType, value, identifierObject.getSubValue()));
                persistedIdentifiers.add(savedIdentifier);
            }
        }
        List<AccountIdentifier> removedIdentifiers = new ArrayList<>(existingIdentifiers.values());
        if (!removedIdentifiers.isEmpty()) {
            accountIdentifierRepository.deleteAll(removedIdentifiers);
            ((List) actualChanges.get(IDENTIFIERS_PARAM)).add(Map.of("removed", removedIdentifiers));
        }
        if (!persistedIdentifiers.isEmpty()) {
            ((List) actualChanges.get(IDENTIFIERS_PARAM)).add(Map.of("persisted", persistedIdentifiers));
        }

        validator.throwValidationErrors();
    }
}
