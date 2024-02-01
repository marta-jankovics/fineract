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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_NUMBER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACTION_DATE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_FORCE_TRANSACTION_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ALLOW_OVERDRAFT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.BALANCE_CALCULATION_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CLIENT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DATATABLES_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DATE_FORMAT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIERS_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.LOCALE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.MINIMUM_REQUIRED_BALANCE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.OVERDRAFT_LIMIT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PRODUCT_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SUBMITTED_ON_DATE_PARAM;

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

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_FOR_CREATE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(LOCALE_PARAM, DATE_FORMAT_PARAM, ACCOUNT_NUMBER_PARAM, EXTERNAL_ID_PARAM, CLIENT_ID_PARAM, PRODUCT_ID_PARAM,
                    SUBMITTED_ON_DATE_PARAM, ALLOW_OVERDRAFT_PARAM, OVERDRAFT_LIMIT_PARAM, MINIMUM_REQUIRED_BALANCE_PARAM,
                    ALLOW_FORCE_TRANSACTION_PARAM, BALANCE_CALCULATION_TYPE_PARAM, DATATABLES_PARAM, IDENTIFIERS_PARAM));

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_FOR_UPDATE_DATA_PARAMETERS = new HashSet<>(Arrays.asList(LOCALE_PARAM,
            ACCOUNT_NUMBER_PARAM, EXTERNAL_ID_PARAM, ALLOW_OVERDRAFT_PARAM, OVERDRAFT_LIMIT_PARAM, MINIMUM_REQUIRED_BALANCE_PARAM,
            ALLOW_FORCE_TRANSACTION_PARAM, BALANCE_CALCULATION_TYPE_PARAM, DATATABLES_PARAM, IDENTIFIERS_PARAM));

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

        final Long clientId = command.longValueOfParameterNamed(CLIENT_ID_PARAM);
        baseDataValidator.reset().parameter(CLIENT_ID_PARAM).value(clientId).notNull().integerGreaterThanZero();

        final String productId = command.stringValueOfParameterNamedAllowingNull(PRODUCT_ID_PARAM);
        baseDataValidator.reset().parameter(PRODUCT_ID_PARAM).value(productId).notNull().notBlank();

        final LocalDate submittedOnDate = command.localDateValueOfParameterNamed(SUBMITTED_ON_DATE_PARAM);
        baseDataValidator.reset().parameter(SUBMITTED_ON_DATE_PARAM).value(submittedOnDate).ignoreIfNull();

        if (command.hasParameter(ACCOUNT_NUMBER_PARAM)) {
            final String accountNumber = command.stringValueOfParameterNamedAllowingNull(ACCOUNT_NUMBER_PARAM);
            baseDataValidator.reset().parameter(ACCOUNT_NUMBER_PARAM).value(accountNumber).notBlank().notExceedingLengthOf(50);
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

        if (command.parameterExists(ACCOUNT_NUMBER_PARAM)) {
            final String accountNumber = command.stringValueOfParameterNamedAllowingNull(ACCOUNT_NUMBER_PARAM);
            baseDataValidator.reset().parameter(ACCOUNT_NUMBER_PARAM).value(accountNumber).notBlank().notExceedingLengthOf(50);
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
        if (command.parameterExists(ALLOW_OVERDRAFT_PARAM)) {
            final Boolean allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(ALLOW_OVERDRAFT_PARAM);
            baseDataValidator.reset().parameter(ALLOW_OVERDRAFT_PARAM).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed(OVERDRAFT_LIMIT_PARAM);
                baseDataValidator.reset().parameter(OVERDRAFT_LIMIT_PARAM).value(overdraftLimit).notNull().positiveAmount();
            }
        }
    }

    private void validateMinimumRequiredBalanceParams(final DataValidatorBuilder baseDataValidator, final JsonCommand command) {
        if (command.parameterExists(MINIMUM_REQUIRED_BALANCE_PARAM)) {
            final BigDecimal minimumRequiredBalance = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(MINIMUM_REQUIRED_BALANCE_PARAM);
            baseDataValidator.reset().parameter(MINIMUM_REQUIRED_BALANCE_PARAM).value(minimumRequiredBalance).zeroOrPositiveAmount();
        }
    }

    private void validateAccountAction(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(ACTION_DATE_PARAM, LOCALE_PARAM, DATE_FORMAT_PARAM));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final LocalDate cancelledOnDate = command.localDateValueOfParameterNamed(ACTION_DATE_PARAM);
        baseDataValidator.reset().parameter(ACTION_DATE_PARAM).value(cancelledOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private static void validateExternalId(JsonCommand command, DataValidatorBuilder baseDataValidator) {
        if (command.parameterExists(EXTERNAL_ID_PARAM)) {
            final String externalId = command.stringValueOfParameterNamedAllowingNull(EXTERNAL_ID_PARAM);
            baseDataValidator.reset().parameter(EXTERNAL_ID_PARAM).value(externalId).notExceedingLengthOf(100);
        }
    }
}
