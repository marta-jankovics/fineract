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
package org.apache.fineract.currentaccount.validator.product.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountingTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowForceTransactionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.balanceCalculationTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyCodeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyDigitsAfterDecimalParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.currencyInMultiplesOfParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.descriptionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minimumRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.nameParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.shortNameParamName;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.validator.product.CurrentProductDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;

@RequiredArgsConstructor
public class CurrentProductDataValidatorImpl implements CurrentProductDataValidator {

    private static final Set<String> CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, nameParamName,
            externalIdParamName, shortNameParamName, descriptionParamName, currencyCodeParamName, currencyDigitsAfterDecimalParamName,
            currencyInMultiplesOfParamName, accountingTypeParamName, allowOverdraftParamName, overdraftLimitParamName,
            allowForceTransactionParamName, minimumRequiredBalanceParamName, balanceCalculationTypeParamName));

    @Override
    public void validateForCreate(final JsonCommand command) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_PRODUCT_RESOURCE_NAME);

        final String name = command.stringValueOfParameterNamed(nameParamName);
        baseDataValidator.reset().parameter(nameParamName).value(name).notBlank().notExceedingLengthOf(100);

        final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
        baseDataValidator.reset().parameter(shortNameParamName).value(shortName).notBlank().notExceedingLengthOf(4);

        final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        baseDataValidator.reset().parameter(currencyCodeParamName).value(currencyCode).notBlank();

        final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(currencyDigitsAfterDecimalParamName);
        baseDataValidator.reset().parameter(currencyDigitsAfterDecimalParamName).value(digitsAfterDecimal).notNull().inMinMaxRange(0, 6);

        if (command.parameterExists(currencyInMultiplesOfParamName)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(currencyInMultiplesOfParamName, Locale.getDefault());
            baseDataValidator.reset().parameter(currencyInMultiplesOfParamName).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }

        if (command.parameterExists(descriptionParamName)) {
            final String description = command.stringValueOfParameterNamed(descriptionParamName);
            baseDataValidator.reset().parameter(descriptionParamName).value(description).ignoreIfNull().notExceedingLengthOf(500);
        }

        // accounting related data validation
        final String accountingRuleType = command.stringValueOfParameterNamed(accountingTypeParamName);
        baseDataValidator.reset().parameter(accountingTypeParamName).value(accountingRuleType).notNull()
                .isOneOfEnumValues(AccountingRuleType.class);

        if (command.parameterExists(minimumRequiredBalanceParamName)) {
            final BigDecimal minimumRequiredBalance = command.bigDecimalValueOfParameterNamed(minimumRequiredBalanceParamName);
            baseDataValidator.reset().parameter(minimumRequiredBalanceParamName).value(minimumRequiredBalance).zeroOrPositiveAmount();
        }

        final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        baseDataValidator.reset().parameter(allowOverdraftParamName).value(allowOverdraft).notNull().validateForBooleanValue();

        if (allowOverdraft) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
            baseDataValidator.reset().parameter(overdraftLimitParamName).value(overdraftLimit).notNull().positiveAmount();
        }

        final String balanceCalculationType = command.stringValueOfParameterNamed(balanceCalculationTypeParamName);
        baseDataValidator.reset().parameter(balanceCalculationTypeParamName).value(balanceCalculationType).notNull()
                .isOneOfEnumValues(BalanceCalculationType.class);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateForUpdate(final JsonCommand command, final CurrentProduct product) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_PRODUCT_RESOURCE_NAME);

        if (command.parameterExists(nameParamName)) {
            final String name = command.stringValueOfParameterNamed(nameParamName);
            baseDataValidator.reset().parameter(nameParamName).value(name).notBlank().notExceedingLengthOf(100);
        }

        if (command.parameterExists(shortNameParamName)) {
            final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
            baseDataValidator.reset().parameter(shortNameParamName).value(shortName).notBlank().notExceedingLengthOf(4);
        }

        if (command.parameterExists(descriptionParamName)) {
            final String description = command.stringValueOfParameterNamed(descriptionParamName);
            baseDataValidator.reset().parameter(descriptionParamName).value(description).notBlank().notExceedingLengthOf(500);
        }

        if (command.parameterExists(currencyCodeParamName)) {
            final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
            baseDataValidator.reset().parameter(currencyCodeParamName).value(currencyCode).notBlank();
        }

        if (command.parameterExists(currencyDigitsAfterDecimalParamName)) {
            final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(currencyDigitsAfterDecimalParamName);
            baseDataValidator.reset().parameter(currencyDigitsAfterDecimalParamName).value(digitsAfterDecimal).notNull().inMinMaxRange(0,
                    6);
        }

        if (command.parameterExists(currencyInMultiplesOfParamName)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(currencyInMultiplesOfParamName, Locale.getDefault());
            baseDataValidator.reset().parameter(currencyInMultiplesOfParamName).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }

        if (command.parameterExists(minimumRequiredBalanceParamName)) {
            final BigDecimal minimumRequiredBalance = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minimumRequiredBalanceParamName);
            baseDataValidator.reset().parameter(minimumRequiredBalanceParamName).value(minimumRequiredBalance);
        }

        if (command.parameterExists(allowOverdraftParamName)) {
            final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            baseDataValidator.reset().parameter(allowOverdraftParamName).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
                baseDataValidator.reset().parameter(overdraftLimitParamName).value(overdraftLimit).notNull().positiveAmount();
            }
        }

        if (!command.parameterExists(allowOverdraftParamName) && command.parameterExists(overdraftLimitParamName)) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
            baseDataValidator.reset().parameter(overdraftLimitParamName).value(overdraftLimit).notNull().positiveAmount();
        }

        if (command.parameterExists(balanceCalculationTypeParamName)) {
            final String balanceCalculationType = command.stringValueOfParameterNamed(balanceCalculationTypeParamName);
            baseDataValidator.reset().parameter(balanceCalculationTypeParamName).value(balanceCalculationType).notNull()
                    .isOneOfEnumValues(BalanceCalculationType.class);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
