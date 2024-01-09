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
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountNoParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.activatedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.cancelledOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.clientIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.closedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.dateFormatParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceMinRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minRequiredBalanceParamName;
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

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName,
            dateFormatParamName, accountNoParamName, externalIdParamName, clientIdParamName, productIdParamName, submittedOnDateParamName,
            allowOverdraftParamName, overdraftLimitParamName, enforceMinRequiredBalanceParamName, minRequiredBalanceParamName));

    @Override
    public void validateForSubmit(final JsonCommand command) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final Long clientId = command.longValueOfParameterNamed(clientIdParamName);
        baseDataValidator.reset().parameter(clientIdParamName).value(clientId).notNull().integerGreaterThanZero();

        final String productId = command.stringValueOfParameterNamed(productIdParamName);
        baseDataValidator.reset().parameter(productIdParamName).value(productId).notNull().notBlank();

        final LocalDate submittedOnDate = command.localDateValueOfParameterNamed(submittedOnDateParamName);
        baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate).ignoreIfNull();

        if (command.parameterExists(accountNoParamName)) {
            final String accountNo = command.stringValueOfParameterNamed(accountNoParamName);
            baseDataValidator.reset().parameter(accountNoParamName).value(accountNo).notBlank().notExceedingLengthOf(20);
        }

        if (command.parameterExists(externalIdParamName)) {
            final String externalId = command.stringValueOfParameterNamed(externalIdParamName);
            baseDataValidator.reset().parameter(externalIdParamName).value(externalId).notExceedingLengthOf(100);
        }

        validateMinRequiredBalanceParams(baseDataValidator, command);
        validateOverdraftParams(baseDataValidator, command);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateForUpdate(final JsonCommand command, CurrentAccount account) {

        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        if (command.parameterExists(clientIdParamName)) {
            Long clientId = command.longValueOfParameterNamed(clientIdParamName);
            baseDataValidator.reset().parameter(clientIdParamName).value(clientId).notNull().integerGreaterThanZero();
        }

        if (command.parameterExists(productIdParamName)) {
            final Long productId = command.longValueOfParameterNamed(productIdParamName);
            baseDataValidator.reset().parameter(productIdParamName).value(productId).notNull().integerGreaterThanZero();
        }

        if (command.parameterExists(submittedOnDateParamName)) {
            final LocalDate submittedOnDate = command.localDateValueOfParameterNamed(submittedOnDateParamName);
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate).notNull();
        }

        if (command.parameterExists(accountNoParamName)) {
            final String accountNo = command.stringValueOfParameterNamed(accountNoParamName);
            baseDataValidator.reset().parameter(accountNoParamName).value(accountNo).notBlank().notExceedingLengthOf(20);
        }

        if (command.parameterExists(externalIdParamName)) {
            final String externalId = command.stringValueOfParameterNamed(externalIdParamName);
            baseDataValidator.reset().parameter(externalIdParamName).value(externalId).notExceedingLengthOf(100);
        }

        validateMinRequiredBalanceParams(baseDataValidator, command);
        validateOverdraftParams(baseDataValidator, command);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateCancellation(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(
                Arrays.asList(cancelledOnDateParamName, localeParamName, dateFormatParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final LocalDate cancelledOnDate = command.localDateValueOfParameterNamed(cancelledOnDateParamName);
        baseDataValidator.reset().parameter(cancelledOnDateParamName).value(cancelledOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateActivation(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Set<String> activationParameters = new HashSet<>(
                Arrays.asList(activatedOnDateParamName, localeParamName, dateFormatParamName));
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), activationParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final LocalDate activationDate = command.localDateValueOfParameterNamed(activatedOnDateParamName);
        baseDataValidator.reset().parameter(activatedOnDateParamName).value(activationDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateClosing(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }
        final Set<String> closingParameters = new HashSet<>(Arrays.asList(closedOnDateParamName, localeParamName, dateFormatParamName));
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), closingParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final LocalDate closedonDate = command.localDateValueOfParameterNamed(closedOnDateParamName);
        baseDataValidator.reset().parameter(closedOnDateParamName).value(closedonDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
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

    private void validateMinRequiredBalanceParams(final DataValidatorBuilder baseDataValidator, final JsonCommand command) {
        if (command.parameterExists(enforceMinRequiredBalanceParamName)) {
            final Boolean enforceMinRequiredBalance = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
            baseDataValidator.reset().parameter(enforceMinRequiredBalanceParamName).value(enforceMinRequiredBalance).notNull()
                    .validateForBooleanValue();

            if (enforceMinRequiredBalance) {
                final BigDecimal minRequiredBalance = command
                        .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredBalanceParamName);
                baseDataValidator.reset().parameter(minRequiredBalanceParamName).value(minRequiredBalance).notNull().positiveAmount();
            }
        }
    }
}
