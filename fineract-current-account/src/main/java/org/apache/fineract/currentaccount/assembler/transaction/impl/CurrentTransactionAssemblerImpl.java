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
package org.apache.fineract.currentaccount.assembler.transaction.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.externalIdParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.transactionAmountParamName;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.transactionDateParamName;
import static org.apache.fineract.infrastructure.core.filters.CorrelationHeaderFilter.CORRELATION_ID_KEY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.portfolio.paymentdetail.PaymentDetailConstants;
import org.slf4j.MDC;

@RequiredArgsConstructor
@Slf4j
public class CurrentTransactionAssemblerImpl implements CurrentTransactionAssembler {

    private final ExternalIdFactory externalIdFactory;

    @Override
    public CurrentTransaction deposit(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.createFromCommand(command, externalIdParamName);
        final Long paymentTypeId = command.longValueOfParameterNamed(PaymentDetailConstants.paymentTypeParamName);

        LocalDate transactionDate = command.localDateValueOfParameterNamed(transactionDateParamName);
        if (transactionDate == null) {
            transactionDate = DateUtils.getBusinessLocalDate();
        }
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transactionAmountParamName);
        LocalDate submittedOnDate = DateUtils.getBusinessLocalDate();

        return CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), null, paymentTypeId,
                CurrentTransactionType.DEPOSIT, transactionDate, submittedOnDate, transactionAmount);
    }

    @Override
    public CurrentTransaction withdrawal(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.createFromCommand(command, externalIdParamName);
        final Long paymentTypeId = command.longValueOfParameterNamed(PaymentDetailConstants.paymentTypeParamName);

        LocalDate transactionDate = command.localDateValueOfParameterNamed(transactionDateParamName);
        if (transactionDate == null) {
            transactionDate = DateUtils.getBusinessLocalDate();
        }
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transactionAmountParamName);
        LocalDate submittedOnDate = DateUtils.getBusinessLocalDate();

        return CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), null, paymentTypeId,
                CurrentTransactionType.WITHDRAWAL, transactionDate, submittedOnDate, transactionAmount);
    }

    @Override
    public CurrentTransaction hold(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.createFromCommand(command, externalIdParamName);
        final Long paymentTypeId = command.longValueOfParameterNamed(PaymentDetailConstants.paymentTypeParamName);

        LocalDate transactionDate = command.localDateValueOfParameterNamed(transactionDateParamName);
        if (transactionDate == null) {
            transactionDate = DateUtils.getBusinessLocalDate();
        }
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transactionAmountParamName);
        LocalDate submittedOnDate = DateUtils.getBusinessLocalDate();

        return CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), null, paymentTypeId,
                CurrentTransactionType.AMOUNT_HOLD, transactionDate, submittedOnDate, transactionAmount);
    }

    @Override
    public CurrentTransaction release(CurrentAccount account, CurrentTransaction holdTransaction, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.create();
        LocalDate actualDate = DateUtils.getBusinessLocalDate();

        return CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), holdTransaction.getId(),
                holdTransaction.getPaymentTypeId(), CurrentTransactionType.AMOUNT_RELEASE, actualDate, actualDate,
                holdTransaction.getTransactionAmount());
    }
}
