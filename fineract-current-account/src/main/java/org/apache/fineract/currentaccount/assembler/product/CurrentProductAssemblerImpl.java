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
package org.apache.fineract.currentaccount.assembler.product;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountingTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyCodeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.descriptionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.digitsAfterDecimalParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceMinRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.inMultiplesOfParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.nameParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.shortNameParamName;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

public class CurrentProductAssemblerImpl implements CurrentProductAssembler {

    public CurrentProduct assemble(final JsonCommand command) {

        final Locale locale = command.extractLocale();
        final String name = command.stringValueOfParameterNamed(nameParamName);
        final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
        final String description = command.stringValueOfParameterNamedAllowingNull(descriptionParamName);
        final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        final Integer digitsAfterDecimal = command.integerValueOfParameterNamed(digitsAfterDecimalParamName);
        final Integer inMultiplesOf = command.integerValueOfParameterNamed(inMultiplesOfParamName);
        final MonetaryCurrency currency = new MonetaryCurrency(currencyCode, digitsAfterDecimal, inMultiplesOf);
        final AccountingRuleType accountingRuleType = AccountingRuleType
                .valueOf(command.stringValueOfParameterNamed("accountingType").toUpperCase(locale));
        boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
        boolean enforceMinRequiredBalance = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
        BigDecimal minRequiredBalance = command.bigDecimalValueOfParameterNamed(minRequiredBalanceParamName);

        return new CurrentProduct(name, shortName, description, currency, accountingRuleType, allowOverdraft, overdraftLimit,
                enforceMinRequiredBalance, minRequiredBalance);
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

        if (command.isChangeInIntegerParameterNamed(digitsAfterDecimalParamName, product.getCurrency().getDigitsAfterDecimal())) {
            final Integer newValue = command.integerValueOfParameterNamed(digitsAfterDecimalParamName);
            actualChanges.put(digitsAfterDecimalParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.getCurrency().setDigitsAfterDecimal(newValue);
        }

        if (command.isChangeInStringParameterNamed(currencyCodeParamName, product.getCurrency().getCode())) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            product.getCurrency().setCode(newValue);
        }

        if (command.isChangeInIntegerParameterNamed(inMultiplesOfParamName, product.getCurrency().getCurrencyInMultiplesOf())) {
            final Integer newValue = command.integerValueOfParameterNamed(inMultiplesOfParamName);
            actualChanges.put(inMultiplesOfParamName, newValue);
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
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(overdraftLimitParamName, product.getOverdraftLimit())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(overdraftLimitParamName);
            actualChanges.put(overdraftLimitParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.setOverdraftLimit(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(enforceMinRequiredBalanceParamName, product.isEnforceMinRequiredBalance())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
            actualChanges.put(minRequiredBalanceParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.setEnforceMinRequiredBalance(newValue);

            if (!newValue) {
                product.setMinRequiredBalance(null);
            }
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minRequiredBalanceParamName, product.getMinRequiredBalance())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredBalanceParamName);
            actualChanges.put(minRequiredBalanceParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            product.setMinRequiredBalance(newValue);
        }

        return actualChanges;
    }
}
