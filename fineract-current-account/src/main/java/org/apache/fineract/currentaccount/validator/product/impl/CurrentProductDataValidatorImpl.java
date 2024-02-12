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
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DATATABLES_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DESCRIPTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.FUND_SOURCE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_FEE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.LOCALE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.MINIMUM_REQUIRED_BALANCE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.NAME_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_LIMIT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_TYPE_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.REFERENCE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SHORT_NAME_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.WRITE_OFF_ACCOUNT_ID_PARAM;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
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
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;

@RequiredArgsConstructor
public class CurrentProductDataValidatorImpl implements CurrentProductDataValidator {

    private static final Set<String> CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(LOCALE_PARAM, NAME_PARAM, EXTERNAL_ID_PARAM, SHORT_NAME_PARAM, DESCRIPTION_PARAM, CURRENCY_CODE_PARAM,
                    CURRENCY_DIGITS_AFTER_DECIMAL_PARAM, CURRENCY_IN_MULTIPLES_OF_PARAM, ACCOUNTING_TYPE_PARAM, ALLOW_OVERDRAFT_PARAM,
                    OVERDRAFT_LIMIT_PARAM, ALLOW_FORCE_TRANSACTION_PARAM, MINIMUM_REQUIRED_BALANCE_PARAM, BALANCE_CALCULATION_TYPE_PARAM,
                    CONTROL_ACCOUNT_ID_PARAM, REFERENCE_ACCOUNT_ID_PARAM, OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM,
                    TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM, WRITE_OFF_ACCOUNT_ID_PARAM, INCOME_FROM_FEE_ACCOUNT_ID_PARAM,
                    INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM, PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM, DATATABLES_PARAM));

    @Override
    public void validateForCreate(final JsonCommand command) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS);

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder().resource(CURRENT_PRODUCT_RESOURCE_NAME);

        final String name = command.stringValueOfParameterNamedAllowingNull(NAME_PARAM);
        dataValidator.reset().parameter(NAME_PARAM).value(name).notBlank().notExceedingLengthOf(100);

        final String shortName = command.stringValueOfParameterNamedAllowingNull(SHORT_NAME_PARAM);
        dataValidator.reset().parameter(SHORT_NAME_PARAM).value(shortName).notBlank().notExceedingLengthOf(4);

        final String currencyCode = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
        dataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).notBlank();

        final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
        dataValidator.reset().parameter(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM).value(digitsAfterDecimal).notNull().inMinMaxRange(0, 6);

        if (command.parameterExists(CURRENCY_IN_MULTIPLES_OF_PARAM)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM, Locale.getDefault());
            dataValidator.reset().parameter(CURRENCY_IN_MULTIPLES_OF_PARAM).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }

        if (command.parameterExists(DESCRIPTION_PARAM)) {
            final String description = command.stringValueOfParameterNamedAllowingNull(DESCRIPTION_PARAM);
            dataValidator.reset().parameter(DESCRIPTION_PARAM).value(description).ignoreIfNull().notExceedingLengthOf(500);
        }

        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            final BigDecimal minimumRequiredBalance = command.bigDecimalValueOfParameterNamed(MINIMUM_REQUIRED_BALANCE_PARAM);
            dataValidator.reset().parameter(MINIMUM_REQUIRED_BALANCE_PARAM).value(minimumRequiredBalance).zeroOrPositiveAmount();
        }

        final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
        dataValidator.reset().parameter(ALLOW_OVERDRAFT_PARAM).value(allowOverdraft).notNull().validateForBooleanValue();

        if (allowOverdraft) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
            dataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
        }

        final String balanceCalculationType = command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM);
        dataValidator.reset().parameter(BALANCE_CALCULATION_TYPE_PARAM).value(balanceCalculationType).notNull()
                .isOneOfEnumValues(BalanceCalculationType.class);

        // accounting related data validation
        final String accountingRuleType = command.stringValueOfParameterNamedAllowingNull(ACCOUNTING_TYPE_PARAM);
        dataValidator.reset().parameter(ACCOUNTING_TYPE_PARAM).value(accountingRuleType).notNull()
                .isOneOfEnumValues(AccountingRuleType.class);
        boolean cashBased = AccountingRuleType.CASH_BASED.name().equals(accountingRuleType);
        if (!(cashBased || AccountingRuleType.NONE.name().equals(accountingRuleType))) {
            dataValidator.failWithCode("is.not.one.of.supported.enumerations");
        }
        if (cashBased) {
            final Long controlAccountId = command.longValueOfParameterNamed(CONTROL_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(CONTROL_ACCOUNT_ID_PARAM).value(controlAccountId).notNull().integerGreaterThanZero();

            final Long referenceAccountId = command.longValueOfParameterNamed(REFERENCE_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(REFERENCE_ACCOUNT_ID_PARAM).value(referenceAccountId).notNull().integerGreaterThanZero();

            final Long transfersInSuspenseAccountId = command.longValueOfParameterNamed(TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM).value(transfersInSuspenseAccountId).notNull()
                    .integerGreaterThanZero();

            final Long incomeFromFeeAccountId = command.longValueOfParameterNamed(INCOME_FROM_FEE_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(INCOME_FROM_FEE_ACCOUNT_ID_PARAM).value(incomeFromFeeAccountId).notNull()
                    .integerGreaterThanZero();

            final Long incomeFromPenaltyAccountId = command.longValueOfParameterNamed(INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM).value(incomeFromPenaltyAccountId).notNull()
                    .integerGreaterThanZero();

            final Long overdraftControlAccountId = command.longValueOfParameterNamed(OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM).value(overdraftControlAccountId).notNull()
                    .integerGreaterThanZero();

            final Long writeOffAccountId = command.longValueOfParameterNamed(WRITE_OFF_ACCOUNT_ID_PARAM);
            dataValidator.reset().parameter(WRITE_OFF_ACCOUNT_ID_PARAM).value(writeOffAccountId).notNull().integerGreaterThanZero();

            validatePaymentChannelFundSourceMappings(dataValidator, command);
        }
        dataValidator.throwValidationErrors();
    }

    @Override
    public void validateForUpdate(final JsonCommand command, final CurrentProduct product) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_PRODUCT_REQUEST_DATA_PARAMETERS);

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder().resource(CURRENT_PRODUCT_RESOURCE_NAME);

        if (command.parameterExists(NAME_PARAM)) {
            final String name = command.stringValueOfParameterNamedAllowingNull(NAME_PARAM);
            dataValidator.reset().parameter(NAME_PARAM).value(name).notBlank().notExceedingLengthOf(100);
        }

        if (command.parameterExists(SHORT_NAME_PARAM)) {
            final String shortName = command.stringValueOfParameterNamedAllowingNull(SHORT_NAME_PARAM);
            dataValidator.reset().parameter(SHORT_NAME_PARAM).value(shortName).notBlank().notExceedingLengthOf(4);
        }

        if (command.parameterExists(DESCRIPTION_PARAM)) {
            final String description = command.stringValueOfParameterNamedAllowingNull(DESCRIPTION_PARAM);
            dataValidator.reset().parameter(DESCRIPTION_PARAM).value(description).notBlank().notExceedingLengthOf(500);
        }

        if (command.parameterExists(CURRENCY_CODE_PARAM)) {
            final String currencyCode = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
            dataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).notBlank();
        }

        if (command.parameterExists(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM)) {
            final Integer digitsAfterDecimal = command.integerValueSansLocaleOfParameterNamed(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM);
            dataValidator.reset().parameter(CURRENCY_DIGITS_AFTER_DECIMAL_PARAM).value(digitsAfterDecimal).notNull().inMinMaxRange(0, 6);
        }

        if (command.parameterExists(CURRENCY_IN_MULTIPLES_OF_PARAM)) {
            final Integer inMultiplesOf = command.integerValueOfParameterNamed(CURRENCY_IN_MULTIPLES_OF_PARAM, Locale.getDefault());
            dataValidator.reset().parameter(CURRENCY_IN_MULTIPLES_OF_PARAM).value(inMultiplesOf).ignoreIfNull().integerZeroOrGreater();
        }
        AccountingRuleType accountingRuleType = product.getAccountingType();
        if (command.parameterExists(CURRENCY_IN_MULTIPLES_OF_PARAM)) {
            final String accountingRuleTypeStr = command.stringValueOfParameterNamedAllowingNull(ACCOUNTING_TYPE_PARAM);
            dataValidator.reset().parameter(ACCOUNTING_TYPE_PARAM).value(accountingRuleTypeStr).notNull()
                    .isOneOfEnumValues(AccountingRuleType.class);
            accountingRuleType = AccountingRuleType.valueOf(accountingRuleTypeStr);
        }
        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            final BigDecimal minimumRequiredBalance = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(MINIMUM_REQUIRED_BALANCE_PARAM);
            dataValidator.reset().parameter(MINIMUM_REQUIRED_BALANCE_PARAM).value(minimumRequiredBalance);
        }

        if (command.parameterExists(ALLOW_OVERDRAFT_PARAM)) {
            final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
            dataValidator.reset().parameter(ALLOW_OVERDRAFT_PARAM).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
                dataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
            }
        }

        if (!command.parameterExists(ALLOW_OVERDRAFT_PARAM) && command.parameterExists(OVERDRAFT_LIMIT_PARAM)) {
            final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
            dataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
        }

        if (command.parameterExists(BALANCE_CALCULATION_TYPE_PARAM)) {
            final String balanceCalculationType = command.stringValueOfParameterNamedAllowingNull(BALANCE_CALCULATION_TYPE_PARAM);
            dataValidator.reset().parameter(BALANCE_CALCULATION_TYPE_PARAM).value(balanceCalculationType).notNull()
                    .isOneOfEnumValues(BalanceCalculationType.class);
        }

        if (accountingRuleType.equals(AccountingRuleType.CASH_BASED)) {
            if (command.parameterExists(CONTROL_ACCOUNT_ID_PARAM)) {
                final Long savingsControlAccountId = command.longValueOfParameterNamed(CONTROL_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(CONTROL_ACCOUNT_ID_PARAM).value(savingsControlAccountId).notNull().integerGreaterThanZero();

            }
            if (command.parameterExists(REFERENCE_ACCOUNT_ID_PARAM)) {
                final Long savingsReferenceAccountId = command.longValueOfParameterNamed(REFERENCE_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(REFERENCE_ACCOUNT_ID_PARAM).value(savingsReferenceAccountId).notNull()
                        .integerGreaterThanZero();
            }
            if (command.parameterExists(TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM)) {
                final Long transfersInSuspenseAccountId = command.longValueOfParameterNamed(TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(TRANSFERS_IN_SUSPENSE_ACCOUNT_ID_PARAM).value(transfersInSuspenseAccountId).notNull()
                        .integerGreaterThanZero();
            }
            if (command.parameterExists(INCOME_FROM_FEE_ACCOUNT_ID_PARAM)) {
                final Long incomeFromFeeAccountId = command.longValueOfParameterNamed(INCOME_FROM_FEE_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(INCOME_FROM_FEE_ACCOUNT_ID_PARAM).value(incomeFromFeeAccountId).notNull()
                        .integerGreaterThanZero();
            }
            if (command.parameterExists(INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM)) {
                final Long incomeFromPenaltyAccountId = command.longValueOfParameterNamed(INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(INCOME_FROM_PENALTY_ACCOUNT_ID_PARAM).value(incomeFromPenaltyAccountId).notNull()
                        .integerGreaterThanZero();
            }
            if (command.parameterExists(OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM)) {
                final Long overdraftControlAccountId = command.longValueOfParameterNamed(OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(OVERDRAFT_CONTROL_ACCOUNT_ID_PARAM).value(overdraftControlAccountId).notNull()
                        .integerGreaterThanZero();
            }
            if (command.parameterExists(WRITE_OFF_ACCOUNT_ID_PARAM)) {
                final Long writtenoff = command.longValueOfParameterNamed(WRITE_OFF_ACCOUNT_ID_PARAM);
                dataValidator.reset().parameter(WRITE_OFF_ACCOUNT_ID_PARAM).value(writtenoff).notNull().integerGreaterThanZero();
            }
            validatePaymentChannelFundSourceMappings(dataValidator, command);
        }
        dataValidator.throwValidationErrors();
    }

    /**
     * Validation for advanced accounting options
     */
    private void validatePaymentChannelFundSourceMappings(final DataValidatorBuilder baseDataValidator, final JsonCommand command) {
        if (command.parameterExists(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM)) {
            final JsonArray paymentChannelMappingArray = command.arrayOfParameterNamed(PAYMENT_CHANNEL_TO_FUND_SOURCE_MAPPINGS_PARAM);
            if (paymentChannelMappingArray != null) {
                for (JsonElement paymentChannelMapping : paymentChannelMappingArray) {
                    final JsonObject jsonObject = paymentChannelMapping.getAsJsonObject();
                    final Long paymentTypeId = jsonObject.get(PAYMENT_TYPE_ID_PARAM).getAsLong();
                    final Long paymentSpecificFundAccountId = jsonObject.get(FUND_SOURCE_ACCOUNT_ID_PARAM).getAsLong();
                    baseDataValidator.reset().parameter(PAYMENT_TYPE_ID_PARAM).value(paymentTypeId).notNull().longGreaterThanZero();
                    baseDataValidator.reset().parameter(FUND_SOURCE_ACCOUNT_ID_PARAM).value(paymentSpecificFundAccountId).notNull()
                            .longGreaterThanZero();
                }
            }
        }
    }
}
