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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountingTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowForceTransactionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.balanceCalculationTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyCodeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyDigitsAfterDecimalParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyInMultiplesOfParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.descriptionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minimumRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.nameParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.shortNameParamName;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.assembler.product.CurrentProductAssembler;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

public class CurrentProductAssemblerImpl implements CurrentProductAssembler {

    public CurrentProduct assemble(final JsonCommand command) {

        final Locale locale = command.extractLocale();
        final String name = command.stringValueOfParameterNamed(nameParamName);
        final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
        final String description = command.stringValueOfParameterNamedAllowingNull(descriptionParamName);
        final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        final Integer currencyDigitsAfterDecimal = command.integerValueOfParameterNamed(currencyDigitsAfterDecimalParamName);
        final Integer currencyInMultiplesOf = command.integerValueOfParameterNamed(currencyInMultiplesOfParamName);
        final MonetaryCurrency currency = new MonetaryCurrency(currencyCode, currencyDigitsAfterDecimal, currencyInMultiplesOf);
        final AccountingRuleType accountingRuleType = AccountingRuleType
                .valueOf(command.stringValueOfParameterNamed(accountingTypeParamName).toUpperCase(locale));
        final boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        final BigDecimal overdraftLimit = allowOverdraft ? command.bigDecimalValueOfParameterNamed(overdraftLimitParamName) : null;
        final boolean allowForceTransaction = command.booleanPrimitiveValueOfParameterNamed(allowForceTransactionParamName);
        final BalanceCalculationType balanceCalculationType = BalanceCalculationType
                .valueOf(command.stringValueOfParameterNamed(balanceCalculationTypeParamName));
        final BigDecimal minimumRequiredBalance = command.bigDecimalValueOfParameterNamed(minimumRequiredBalanceParamName);

        return new CurrentProduct(null, name, shortName, description, currency, accountingRuleType, allowOverdraft, overdraftLimit,
                allowForceTransaction, minimumRequiredBalance, balanceCalculationType, null);
    }

    public Map<String, Object> update(CurrentProduct product, JsonCommand command) {
        final Map<String, Object> actualChanges = new HashMap<>();

        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(nameParamName, product.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            product.setName(newValue);
        }

        if (command.isChangeInStringParameterNamed(shortNameParamName, product.getShortName())) {
            final String newValue = command.stringValueOfParameterNamed(shortNameParamName);
            actualChanges.put(shortNameParamName, newValue);
            product.setShortName(newValue);
        }

        if (command.isChangeInStringParameterNamed(descriptionParamName, product.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            product.setDescription(newValue);
        }

        if (command.isChangeInIntegerParameterNamed(currencyDigitsAfterDecimalParamName, product.getCurrency().getDigitsAfterDecimal())) {
            final Integer newValue = command.integerValueOfParameterNamed(currencyDigitsAfterDecimalParamName);
            actualChanges.put(currencyDigitsAfterDecimalParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.getCurrency().setDigitsAfterDecimal(newValue);
        }

        if (command.isChangeInStringParameterNamed(currencyCodeParamName, product.getCurrency().getCode())) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            product.getCurrency().setCode(newValue);
        }

        if (command.isChangeInIntegerParameterNamed(currencyInMultiplesOfParamName, product.getCurrency().getCurrencyInMultiplesOf())) {
            final Integer newValue = command.integerValueOfParameterNamed(currencyInMultiplesOfParamName);
            actualChanges.put(currencyInMultiplesOfParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.getCurrency().setInMultiplesOf(newValue);
        }

        if (command.isChangeInStringParameterNamed(accountingTypeParamName, product.getAccountingType().name())) {
            final String newValue = command.stringValueOfParameterNamed(accountingTypeParamName);
            actualChanges.put(accountingTypeParamName, newValue);
            product.setAccountingType(AccountingRuleType.valueOf(newValue));
        }

        if (command.isChangeInBooleanParameterNamed(allowOverdraftParamName, product.isAllowOverdraft())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            actualChanges.put(allowOverdraftParamName, newValue);
            product.setAllowOverdraft(newValue);
            if (!newValue) {
                product.setOverdraftLimit(null);
            }
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(overdraftLimitParamName, product.getOverdraftLimit())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(overdraftLimitParamName);
            actualChanges.put(overdraftLimitParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.setOverdraftLimit(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(allowForceTransactionParamName, product.isAllowForceTransaction())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowForceTransactionParamName);
            actualChanges.put(allowForceTransactionParamName, newValue);
            product.setAllowForceTransaction(newValue);
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minimumRequiredBalanceParamName,
                product.getMinimumRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minimumRequiredBalanceParamName);
            actualChanges.put(minimumRequiredBalanceParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.setMinimumRequiredBalance(newValue);
        }

        if (command.isChangeInStringParameterNamed(balanceCalculationTypeParamName, product.getBalanceCalculationType().name())) {
            final String newValue = command.stringValueOfParameterNamed(balanceCalculationTypeParamName);
            actualChanges.put(balanceCalculationTypeParamName, newValue);
            product.setBalanceCalculationType(BalanceCalculationType.valueOf(newValue));
        }

        return actualChanges;
    }
}
