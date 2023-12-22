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
package org.apache.fineract.currentaccount.validator.account;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.accountNoParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.activatedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.allowOverdraftParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.approvedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.clientIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.closedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.dateFormatParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceMinRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.minRequiredBalanceParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.overdraftLimitParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.productIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.rejectedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.submittedOnDateParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.withdrawnOnDateParamName;

import com.google.gson.JsonElement;
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
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@RequiredArgsConstructor
public class CurrentAccountDataValidatorImpl implements CurrentAccountDataValidator {

    public static final Set<String> CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName,
            dateFormatParamName, accountNoParamName, externalIdParamName, clientIdParamName, productIdParamName, submittedOnDateParamName,
            allowOverdraftParamName, overdraftLimitParamName, enforceMinRequiredBalanceParamName, minRequiredBalanceParamName));

    private final FromJsonHelper fromApiJsonHelper;

    public void validateForSubmit(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final Long clientId = this.fromApiJsonHelper.extractLongNamed(clientIdParamName, element);
        baseDataValidator.reset().parameter(clientIdParamName).value(clientId).notNull().integerGreaterThanZero();

        final Long productId = this.fromApiJsonHelper.extractLongNamed(productIdParamName, element);
        baseDataValidator.reset().parameter(productIdParamName).value(productId).notNull().integerGreaterThanZero();

        final LocalDate submittedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(submittedOnDateParamName, element);
        baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate).ignoreIfNull();

        if (this.fromApiJsonHelper.parameterExists(accountNoParamName, element)) {
            final String accountNo = this.fromApiJsonHelper.extractStringNamed(accountNoParamName, element);
            baseDataValidator.reset().parameter(accountNoParamName).value(accountNo).notBlank().notExceedingLengthOf(20);
        }

        if (this.fromApiJsonHelper.parameterExists(externalIdParamName, element)) {
            final String externalId = this.fromApiJsonHelper.extractStringNamed(externalIdParamName, element);
            baseDataValidator.reset().parameter(externalIdParamName).value(externalId).notExceedingLengthOf(100);
        }

        validateMinRequiredBalanceParams(baseDataValidator, element);
        validateOverdraftParams(baseDataValidator, element);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(CurrentAccount account, final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CURRENT_ACCOUNT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(clientIdParamName, element)) {
            Long clientId = this.fromApiJsonHelper.extractLongNamed(clientIdParamName, element);
            baseDataValidator.reset().parameter(clientIdParamName).value(clientId).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(productIdParamName, element)) {
            final Long productId = this.fromApiJsonHelper.extractLongNamed(productIdParamName, element);
            baseDataValidator.reset().parameter(productIdParamName).value(productId).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(submittedOnDateParamName, element)) {
            final LocalDate submittedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(submittedOnDateParamName, element);
            baseDataValidator.reset().parameter(submittedOnDateParamName).value(submittedOnDate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(accountNoParamName, element)) {
            final String accountNo = this.fromApiJsonHelper.extractStringNamed(accountNoParamName, element);
            baseDataValidator.reset().parameter(accountNoParamName).value(accountNo).notBlank().notExceedingLengthOf(20);
        }

        if (this.fromApiJsonHelper.parameterExists(externalIdParamName, element)) {
            final String externalId = this.fromApiJsonHelper.extractStringNamed(externalIdParamName, element);
            baseDataValidator.reset().parameter(externalIdParamName).value(externalId).notExceedingLengthOf(100);
        }

        validateMinRequiredBalanceParams(baseDataValidator, element);
        validateOverdraftParams(baseDataValidator, element);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateApproval(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(
                Arrays.asList(approvedOnDateParamName, localeParamName, dateFormatParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final LocalDate approvedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(approvedOnDateParamName, element);
        baseDataValidator.reset().parameter(approvedOnDateParamName).value(approvedOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateRejection(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(
                Arrays.asList(rejectedOnDateParamName, localeParamName, dateFormatParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final LocalDate rejectedOnDate = this.fromApiJsonHelper.extractLocalDateNamed(rejectedOnDateParamName, element);
        baseDataValidator.reset().parameter(rejectedOnDateParamName).value(rejectedOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateWithdrawal(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(
                Arrays.asList(withdrawnOnDateParamName, localeParamName, dateFormatParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final LocalDate withdrawnOnDate = this.fromApiJsonHelper.extractLocalDateNamed(withdrawnOnDateParamName, element);
        baseDataValidator.reset().parameter(withdrawnOnDateParamName).value(withdrawnOnDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateActivation(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> activationParameters = new HashSet<>(
                Arrays.asList(activatedOnDateParamName, localeParamName, dateFormatParamName));
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, activationParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate activationDate = this.fromApiJsonHelper.extractLocalDateNamed(activatedOnDateParamName, element);
        baseDataValidator.reset().parameter(activatedOnDateParamName).value(activationDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    @Override
    public void validateClosing(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final Set<String> closingParameters = new HashSet<>(Arrays.asList(closedOnDateParamName, localeParamName, dateFormatParamName));
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, closingParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate closedonDate = this.fromApiJsonHelper.extractLocalDateNamed(closedOnDateParamName, element);
        baseDataValidator.reset().parameter(closedOnDateParamName).value(closedonDate).ignoreIfNull()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void validateOverdraftParams(final DataValidatorBuilder baseDataValidator, final JsonElement element) {
        if (this.fromApiJsonHelper.parameterExists(allowOverdraftParamName, element)) {
            final Boolean allowOverdraft = this.fromApiJsonHelper.extractBooleanNamed(allowOverdraftParamName, element);
            baseDataValidator.reset().parameter(allowOverdraftParamName).value(allowOverdraft).notNull().validateForBooleanValue();

            if (allowOverdraft) {
                final BigDecimal overdraftLimit = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(overdraftLimitParamName, element);
                baseDataValidator.reset().parameter(overdraftLimitParamName).value(overdraftLimit).notNull().positiveAmount();
            }
        }
    }

    private void validateMinRequiredBalanceParams(final DataValidatorBuilder baseDataValidator, final JsonElement element) {
        if (this.fromApiJsonHelper.parameterExists(enforceMinRequiredBalanceParamName, element)) {
            final Boolean enforceMinRequiredBalance = this.fromApiJsonHelper.extractBooleanNamed(enforceMinRequiredBalanceParamName,
                    element);
            baseDataValidator.reset().parameter(enforceMinRequiredBalanceParamName).value(enforceMinRequiredBalance).notNull()
                    .validateForBooleanValue();

            if (enforceMinRequiredBalance) {
                final BigDecimal minRequiredBalance = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(minRequiredBalanceParamName,
                        element);
                baseDataValidator.reset().parameter(minRequiredBalanceParamName).value(minRequiredBalance).notNull().positiveAmount();
            }
        }
    }
}
