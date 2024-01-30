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
package org.apache.fineract.currentaccount.assembler.product.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNTING_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_FORCE_TRANSACTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_OVERDRAFT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.BALANCE_CALCULATION_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_CODE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_DIGITS_AFTER_DECIMAL_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_IN_MULTIPLES_OF_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DESCRIPTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.LOCALE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.MINIMUM_REQUIRED_BALANCE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.NAME_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_LIMIT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SHORT_NAME_PARAM;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.assembler.product.CurrentProductAssembler;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

@Slf4j
@RequiredArgsConstructor
public class CurrentProductAssemblerImpl implements CurrentProductAssembler {

    private final ExternalIdFactory externalIdFactory;
    private final CurrentProductRepository currentProductRepository;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;

    @Override
    public CurrentProduct assemble(final JsonCommand command) {
        final Locale locale = command.extractLocale();
        final String name = command.stringValueOfParameterNamedAllowingNull(NAME_PARAM);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, EXTERNAL_ID_PARAM);
        final String shortName = command.stringValueOfParameterNamedAllowingNull(SHORT_NAME_PARAM);
        final String description = command.stringValueOfParameterNamedAllowingNull(DESCRIPTION_PARAM);
        final String currencyCode = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
        final Integer currencyDigitsAfterDecimal = command.integerValueOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
        final Integer currencyInMultiplesOf = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM);
        final MonetaryCurrency currency = new MonetaryCurrency(currencyCode, currencyDigitsAfterDecimal, currencyInMultiplesOf);
        final AccountingRuleType accountingRuleType = AccountingRuleType
                .valueOf(command.stringValueOfParameterNamedAllowingNull(ACCOUNTING_TYPE_PARAM).toUpperCase(locale));
        final boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
        final BigDecimal overdraftLimit = allowOverdraft ? command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM) : null;
        final boolean allowForceTransaction = command.booleanPrimitiveValueOfParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM);
        final BalanceCalculationType balanceCalculationType = BalanceCalculationType
                .valueOf(command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM));
        final BigDecimal minimumRequiredBalance = command.bigDecimalValueOfParameterNamed(MINIMUM_REQUIRED_BALANCE_PARAM);

        CurrentProduct product = new CurrentProduct(null, externalId, name, shortName, description, currency, accountingRuleType,
                allowOverdraft, overdraftLimit, allowForceTransaction, minimumRequiredBalance, balanceCalculationType, null);

        product = currentProductRepository.save(product);

        persistDatatableEntries(EntityTables.CURRENT_PRODUCT, product.getId(), command, false, readWriteNonCoreDataService);

        return product;
    }

    @Override
    public Map<String, Object> update(CurrentProduct product, JsonCommand command) {
        final Map<String, Object> actualChanges = new HashMap<>();

        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(NAME_PARAM, product.getName())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(NAME_PARAM);
            actualChanges.put(NAME_PARAM, newValue);
            product.setName(newValue);
        }

        if (command.isChangeInStringParameterNamed(SHORT_NAME_PARAM, product.getShortName())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(SHORT_NAME_PARAM);
            actualChanges.put(SHORT_NAME_PARAM, newValue);
            product.setShortName(newValue);
        }

        if (command.isChangeInStringParameterNamed(DESCRIPTION_PARAM, product.getDescription())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(DESCRIPTION_PARAM);
            actualChanges.put(DESCRIPTION_PARAM, newValue);
            product.setDescription(newValue);
        }

        if (command.isChangeInIntegerParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM, product.getCurrency().getDigitsAfterDecimal())) {
            final Integer newValue = command.integerValueOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
            actualChanges.put(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM, newValue);
            actualChanges.put(LOCALE_PARAM, localeAsInput);
            product.getCurrency().setDigitsAfterDecimal(newValue);
        }

        if (command.isChangeInStringParameterNamed(CURRENCY_CODE_PARAM, product.getCurrency().getCode())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
            actualChanges.put(CURRENCY_CODE_PARAM, newValue);
            product.getCurrency().setCode(newValue);
        }

        if (command.isChangeInIntegerParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM, product.getCurrency().getCurrencyInMultiplesOf())) {
            final Integer newValue = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM);
            actualChanges.put(CURRENCY_IN_MULTIPLES_OF_PARAM, newValue);
            actualChanges.put(LOCALE_PARAM, localeAsInput);
            product.getCurrency().setInMultiplesOf(newValue);
        }

        if (command.isChangeInStringParameterNamed(ACCOUNTING_TYPE_PARAM, product.getAccountingType().name())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(ACCOUNTING_TYPE_PARAM);
            actualChanges.put(ACCOUNTING_TYPE_PARAM, newValue);
            product.setAccountingType(AccountingRuleType.valueOf(newValue));
        }

        if (command.isChangeInBooleanParameterNamed(ALLOW_OVERDRAFT_PARAM, product.isAllowOverdraft())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
            actualChanges.put(ALLOW_OVERDRAFT_PARAM, newValue);
            product.setAllowOverdraft(newValue);
            if (!newValue) {
                product.setOverdraftLimit(null);
            }
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(OVERDRAFT_LIMIT_PARAM, product.getOverdraftLimit())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(OVERDRAFT_LIMIT_PARAM);
            actualChanges.put(OVERDRAFT_LIMIT_PARAM, newValue);
            actualChanges.put(LOCALE_PARAM, localeAsInput);
            product.setOverdraftLimit(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM, product.isAllowForceTransaction())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(ALLOW_FORCE_TRANSACTION_PARAM);
            actualChanges.put(ALLOW_FORCE_TRANSACTION_PARAM, newValue);
            product.setAllowForceTransaction(newValue);
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(MINIMUM_REQUIRED_BALANCE_PARAM,
                product.getMinimumRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(MINIMUM_REQUIRED_BALANCE_PARAM);
            actualChanges.put(MINIMUM_REQUIRED_BALANCE_PARAM, newValue);
            actualChanges.put(LOCALE_PARAM, localeAsInput);
            product.setMinimumRequiredBalance(newValue);
        }

        if (command.isChangeInStringParameterNamed(BALANCE_CALCULATION_TYPE_PARAM, product.getBalanceCalculationType().name())) {
            final String newValue = command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM);
            actualChanges.put(BALANCE_CALCULATION_TYPE_PARAM, newValue);
            product.setBalanceCalculationType(BalanceCalculationType.valueOf(newValue));
        }

        persistDatatableEntries(EntityTables.CURRENT_PRODUCT, product.getId(), command, true, readWriteNonCoreDataService);
        return actualChanges;
    }
}
