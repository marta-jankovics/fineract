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
package org.apache.fineract.currentaccount.validator.transaction.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_TRANSACTION_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.bankNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.checkNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.dateFormatParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.localeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.paymentTypeIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.receiptNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.routingCodeParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.transactionAccountNumberParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.transactionAmountParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.transactionDateParamName;

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
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;

@RequiredArgsConstructor
public class CurrentTransactionDataValidatorImpl implements CurrentTransactionDataValidator {

    protected static final Set<String> CURRENT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName,
            dateFormatParamName, transactionDateParamName, transactionAmountParamName, paymentTypeIdParamName,
            transactionAccountNumberParamName, checkNumberParamName, routingCodeParamName, receiptNumberParamName, bankNumberParamName));

    @Override
    public void validateDeposit(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateWithdraw(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateHold(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateRelease(JsonCommand command) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_TRANSACTION_RESOURCE_NAME);

        final String transactionId = command.getTransactionId();
        baseDataValidator.reset().parameter("transactionId").value(transactionId).notNull().notBlank();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void validateTransaction(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CURRENT_ACCOUNT_TRANSACTION_RESOURCE_NAME);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed(transactionDateParamName);
        baseDataValidator.reset().parameter(transactionDateParamName).value(transactionDate).notNull();

        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transactionAmountParamName);
        baseDataValidator.reset().parameter(transactionAmountParamName).value(transactionAmount).notNull().positiveAmount();

        final Integer paymentType = command.integerValueOfParameterNamed(paymentTypeIdParamName);
        baseDataValidator.reset().parameter(paymentTypeIdParamName).value(paymentType).notNull();

        validatePaymentTypeDetails(baseDataValidator, command);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void validatePaymentTypeDetails(final DataValidatorBuilder baseDataValidator, JsonCommand command) {
        // Validate all string payment detail fields for max length
        boolean checkPaymentTypeDetails = false;
        final Integer paymentTypeId = command.integerValueOfParameterNamed(paymentTypeIdParamName);
        baseDataValidator.reset().parameter(paymentTypeIdParamName).value(paymentTypeId).ignoreIfNull().integerGreaterThanZero();
        final Set<String> paymentDetailParameters = new HashSet<>(Arrays.asList(transactionAccountNumberParamName, checkNumberParamName,
                routingCodeParamName, receiptNumberParamName, bankNumberParamName));
        for (final String paymentDetailParameterName : paymentDetailParameters) {
            final String paymentDetailParameterValue = command.stringValueOfParameterNamed(paymentDetailParameterName);
            baseDataValidator.reset().parameter(paymentDetailParameterName).value(paymentDetailParameterValue).ignoreIfNull()
                    .notExceedingLengthOf(50);
            if (paymentDetailParameterValue != null && !paymentDetailParameterValue.isEmpty()) {
                checkPaymentTypeDetails = true;
            }
        }
        if (checkPaymentTypeDetails) {
            baseDataValidator.reset().parameter(paymentTypeIdParamName).value(paymentTypeId).notBlank().integerGreaterThanZero();
        }
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
