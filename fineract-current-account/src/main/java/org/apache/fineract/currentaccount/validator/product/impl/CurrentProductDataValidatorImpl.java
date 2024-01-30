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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNTING_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_FORCE_TRANSACTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_OVERDRAFT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.BALANCE_CALCULATION_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CONTROL_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_CODE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_DIGITS_AFTER_DECIMAL_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_IN_MULTIPLES_OF_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DESCRIPTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_FEE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_PENALTY_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.LOCALE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.MINIMUM_REQUIRED_BALANCE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.NAME_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_LIMIT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.REFERENCE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SHORT_NAME_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.WRITE_OFF_ACCOUNT_ID_PARAM;

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

    private static final Set<String> CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(LOCALE_PARAM, NAME_PARAM, EXTERNAL_ID_PARAM, SHORT_NAME_PARAM, DESCRIPTION_PARAM, CURRENCY_CODE_PARAM,
                    CURRENCY_DIGITS_AFTER_DECIMAL_PARAM, CURRENCY_IN_MULTIPLES_OF_PARAM, ACCOUNTING_TYPE_PARAM, ALLOW_OVERDRAFT_PARAM,
                    OVERDRAFT_LIMIT_PARAM, ALLOW_FORCE_TRANSACTION_PARAM, MINIMUM_REQUIRED_BALANCE_PARAM, BALANCE_CALCULATION_TYPE_PARAM,
                    CONTROL_ACCOUNT_ID_PARAM, REFERENCE_ACCOUNT_ID_PARAM, OVERDRAFT_ACCOUNT_ID_PARAM,
                    TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM, WRITE_OFF_ACCOUNT_ID_PARAM, INCOME_FROM_FEE_PARAM, INCOME_FROM_PENALTY_PARAM));

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

        final String name = command.stringValueOfParameterNamed(NAME_PARAM);
        baseDataValidator.reset().parameter(NAME_PARAM).value(name).notBlank().notExceedingLengthOf(100);

        final String shortName = command.stringValueOfParameterNamed(SHORT_NAME_PARAM);
        baseDataValidator.reset().parameter(SHORT_NAME_PARAM).value(shortName).notBlank().notExceedingLengthOf(4);

        final String currencyCode = command.stringValueOfParameterNamed(CURRENCY_CODE_PARAM);
        baseDataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).notBlank();

        final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
        baseDataValidator.reset().parameter(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM).value(digitsAfterDecimal).notNull().inMinMaxRange(0, 6);

        if (command.parameterExists(CURRENCY_IN_MULTIPLES_OF_PARAM)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM, Locale.getDefault());
            baseDataValidator.reset().parameter(CURRENCY_IN_MULTIPLES_OF_PARAM).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }

        if (command.parameterExists(DESCRIPTION_PARAM)) {
            final String description = command.stringValueOfParameterNamed(DESCRIPTION_PARAM);
            baseDataValidator.reset().parameter(DESCRIPTION_PARAM).value(description).ignoreIfNull().notExceedingLengthOf(500);
        }

        // accounting related data validation
        final String accountingRuleType = command.stringValueOfParameterNamed(ACCOUNTING_TYPE_PARAM);
        baseDataValidator.reset().parameter(ACCOUNTING_TYPE_PARAM).value(accountingRuleType).notNull()
                .isOneOfEnumValues(AccountingRuleType.class);

        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            final BigDecimal minimumRequiredBalance = command.bigDecimalValueOfParameterNamed(MINIMUM_REQUIRED_BALANCE_PARAM);
            baseDataValidator.reset().parameter(MINIMUM_REQUIRED_BALANCE_PARAM).value(minimumRequiredBalance).zeroOrPositiveAmount();
        }

        final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
        baseDataValidator.reset().parameter(ALLOW_OVERDRAFT_PARAM).value(allowOverdraft).notNull().validateForBooleanValue();

        if (allowOverdraft) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
            baseDataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
        }

        final String balanceCalculationType = command.stringValueOfParameterNamed(BALANCE_CALCULATION_TYPE_PARAM);
        baseDataValidator.reset().parameter(BALANCE_CALCULATION_TYPE_PARAM).value(balanceCalculationType).notNull()
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

        if (command.parameterExists(NAME_PARAM)) {
            final String name = command.stringValueOfParameterNamed(NAME_PARAM);
            baseDataValidator.reset().parameter(NAME_PARAM).value(name).notBlank().notExceedingLengthOf(100);
        }

        if (command.parameterExists(SHORT_NAME_PARAM)) {
            final String shortName = command.stringValueOfParameterNamed(SHORT_NAME_PARAM);
            baseDataValidator.reset().parameter(SHORT_NAME_PARAM).value(shortName).notBlank().notExceedingLengthOf(4);
        }

        if (command.parameterExists(DESCRIPTION_PARAM)) {
            final String description = command.stringValueOfParameterNamed(DESCRIPTION_PARAM);
            baseDataValidator.reset().parameter(DESCRIPTION_PARAM).value(description).notBlank().notExceedingLengthOf(500);
        }

        if (command.parameterExists(CURRENCY_CODE_PARAM)) {
            final String currencyCode = command.stringValueOfParameterNamed(CURRENCY_CODE_PARAM);
            baseDataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).notBlank();
        }

        if (command.parameterExists(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM)) {
            final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
            baseDataValidator.reset().parameter(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM).value(digitsAfterDecimal).notNull().inMinMaxRange(0,
                    6);
        }

        if (command.parameterExists(CURRENCY_IN_MULTIPLES_OF_PARAM)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM, Locale.getDefault());
            baseDataValidator.reset().parameter(CURRENCY_IN_MULTIPLES_OF_PARAM).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }

        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            final BigDecimal minimumRequiredBalance = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(MINIMUM_REQUIRED_BALANCE_PARAM);
            baseDataValidator.reset().parameter(MINIMUM_REQUIRED_BALANCE_PARAM).value(minimumRequiredBalance);
        }

        if (command.parameterExists(ALLOW_OVERDRAFT_PARAM)) {
            final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
            baseDataValidator.reset().parameter(ALLOW_OVERDRAFT_PARAM).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
                baseDataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
            }
        }

        if (!command.parameterExists(ALLOW_OVERDRAFT_PARAM) && command.parameterExists(OVERDRAFT_LIMIT_PARAM)) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
            baseDataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
        }

        if (command.parameterExists(BALANCE_CALCULATION_TYPE_PARAM)) {
            final String balanceCalculationType = command.stringValueOfParameterNamed(BALANCE_CALCULATION_TYPE_PARAM);
            baseDataValidator.reset().parameter(BALANCE_CALCULATION_TYPE_PARAM).value(balanceCalculationType).notNull()
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