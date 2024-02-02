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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_CODE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DATATABLES_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.DATE_FORMAT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.LOCALE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_TYPE_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_ACCOUNT_NUMBER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_AMOUNT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_DATE_PARAM;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@RequiredArgsConstructor
public class CurrentTransactionDataValidatorImpl implements CurrentTransactionDataValidator {

    protected static final Set<String> CURRENT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(LOCALE_PARAM, DATE_FORMAT_PARAM, TRANSACTION_DATE_PARAM, TRANSACTION_AMOUNT_PARAM, PAYMENT_TYPE_ID_PARAM,
                    TRANSACTION_ACCOUNT_NUMBER_PARAM, CURRENCY_CODE_PARAM, DATATABLES_PARAM));

    @Override
    public void validateDeposit(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateWithdrawal(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateHold(JsonCommand command) {
        validateTransaction(command);
    }

    @Override
    public void validateRelease(JsonCommand command) {
        final DataValidatorBuilder dataValidator = new DataValidatorBuilder().resource(CURRENT_TRANSACTION_RESOURCE_NAME);

        final String holdTransactionId = command.getTransactionId();
        dataValidator.reset().parameter("transactionId").value(holdTransactionId).notNull().notBlank();

        dataValidator.throwValidationErrors();
    }

    private void validateTransaction(JsonCommand command) {
        if (StringUtils.isBlank(command.json())) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        command.checkForUnsupportedParameters(typeOfMap, command.json(), CURRENT_ACCOUNT_TRANSACTION_REQUEST_DATA_PARAMETERS);

        final DataValidatorBuilder dataValidator = new DataValidatorBuilder().resource(CURRENT_TRANSACTION_RESOURCE_NAME);

        if (command.hasParameter(TRANSACTION_DATE_PARAM)) {
            final LocalDate transactionDate = command.localDateValueOfParameterNamed(TRANSACTION_DATE_PARAM);
            dataValidator.reset().parameter(TRANSACTION_DATE_PARAM).value(transactionDate).notNull()
                    .validateDateForEqual(DateUtils.getBusinessLocalDate());
        }

        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(TRANSACTION_AMOUNT_PARAM);
        dataValidator.reset().parameter(TRANSACTION_AMOUNT_PARAM).value(transactionAmount).notNull().positiveAmount();

        final Integer paymentType = command.integerValueOfParameterNamed(PAYMENT_TYPE_ID_PARAM);
        dataValidator.reset().parameter(PAYMENT_TYPE_ID_PARAM).value(paymentType).notNull();

        validatePaymentTypeDetails(dataValidator, command);

        if (command.hasParameter(CURRENCY_CODE_PARAM)) {
            final String currencyCode = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
            dataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).notBlank();
        }
        dataValidator.throwValidationErrors();
    }

    private void validatePaymentTypeDetails(final DataValidatorBuilder baseDataValidator, JsonCommand command) {
        final Integer paymentTypeId = command.integerValueOfParameterNamed(PAYMENT_TYPE_ID_PARAM);
        baseDataValidator.reset().parameter(PAYMENT_TYPE_ID_PARAM).value(paymentTypeId).ignoreIfNull().integerGreaterThanZero();
    }
}
