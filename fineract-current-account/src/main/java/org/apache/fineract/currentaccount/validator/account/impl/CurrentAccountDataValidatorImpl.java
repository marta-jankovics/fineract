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
package org.apache.fineract.currentaccount.validator.account.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.actionDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowForceTransactionParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.balanceCalculationTypeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.clientIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.dateFormatParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.identifiersParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minimumRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.productIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.submittedOnDateParamName;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.validator.account.CurrentAccountDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@RequiredArgsConstructor
public class CurrentAccountDataValidatorImpl implements CurrentAccountDataValidator {

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_FOR_CREATE_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName,
            dateFormatParamName, accountNumberParamName, externalIdParamName, clientIdParamName, productIdParamName,
            submittedOnDateParamName, allowOverdraftParamName, overdraftLimitParamName, minimumRequiredBalanceParamName,
            allowForceTransactionParamName, balanceCalculationTypeParamName, identifiersParamName));

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_FOR_UPDATE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(localeParamName, accountNumberParamName, externalIdParamName, allowOverdraftParamName, overdraftLimitParamName,
                    minimumRequiredBalanceParamName, allowForceTransactionParamName, balanceCalculationTypeParamName));

    @Override
    public void validateForSubmit(final JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_REQUEST_FOR_CREATE_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final Long clientId = command.longValueOfParameterNamed(clientIdParamName);
        baseDataValidator.reset().parameter(clientIdParamName).value(clientId).notNull().integerGreaterThanZero();

        final String productId = command.stringValueOfParameterNamed(productIdParamName);
        baseDataValidator.reset().parameter(productIdParamName).value(productId).notNull().notBlank();

        final LocalDate submittedOnDate = command.localDateValueOfParameterNamed(submittedOnDateParamName);
        baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate).ignoreIfNull();

        if (command.hasParameter(accountNumberParamName)) {
            final String accountNumber = command.stringValueOfParameterNamed(accountNumberParamName);
            baseDataValidator.reset().parameter(accountNumberParamName).value(accountNumber).notBlank().notExceedingLengthOf(50);
        }

        validateExternalId(command, baseDataValidator);
        validateMinimumRequiredBalanceParams(baseDataValidator, command);
        validateOverdraftParams(baseDataValidator, command);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateForUpdate(final JsonCommand command, CurrentAccount account) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_REQUEST_FOR_UPDATE_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        if (command.parameterExists(accountNumberParamName)) {
            final String accountNumber = command.stringValueOfParameterNamed(accountNumberParamName);
            baseDataValidator.reset().parameter(accountNumberParamName).value(accountNumber).notBlank().notExceedingLengthOf(50);
        }

        validateExternalId(command, baseDataValidator);

        validateMinimumRequiredBalanceParams(baseDataValidator, command);
        validateOverdraftParams(baseDataValidator, command);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateCancellation(JsonCommand command) {
        validateAccountAction(command);
    }

    @Override
    public void validateActivation(JsonCommand command) {
        validateAccountAction(command);
    }

    @Override
    public void validateClosing(JsonCommand command) {
        validateAccountAction(command);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void validateOverdraftParams(final DataValidatorBuilder baseDataValidator, final JsonCommand command) {
        if (command.parameterExists(allowOverdraftParamName)) {
            final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            baseDataValidator.reset().parameter(allowOverdraftParamName).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
                baseDataValidator.reset().parameter(overdraftLimitParamName).value(overdraftLimit).notNull().positiveAmount();
            }
        }
    }

    private void validateMinimumRequiredBalanceParams(final DataValidatorBuilder baseDataValidator, final JsonCommand command) {
        if (command.parameterExists(minimumRequiredBalanceParamName)) {
            final BigDecimal minimumRequiredBalance = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minimumRequiredBalanceParamName);
            baseDataValidator.reset().parameter(minimumRequiredBalanceParamName).value(minimumRequiredBalance).zeroOrPositiveAmount();
        }
    }

    private void validateAccountAction(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(actionDateParamName, localeParamName, dateFormatParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final LocalDate cancelledOnDate = command.localDateValueOfParameterNamed(actionDateParamName);
        baseDataValidator.reset().parameter(actionDateParamName).value(cancelledOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private static void validateExternalId(JsonCommand command, DataValidatorBuilder baseDataValidator) {
        if (command.parameterExists(externalIdParamName)) {
            final String externalId = command.stringValueOfParameterNamed(externalIdParamName);
            baseDataValidator.reset().parameter(externalIdParamName).value(externalId).notExceedingLengthOf(100);
        }
    }
}
