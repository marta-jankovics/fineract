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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_AMOUNT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_DATE_PARAM;
import static org.apache.fineract.infrastructure.core.filters.CorrelationHeaderFilter.CORRELATION_ID_KEY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.exception.UnsupportedCommandException;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.paymentdetail.PaymentDetailConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

@RequiredArgsConstructor
@Slf4j
public class CurrentTransactionAssemblerImpl implements CurrentTransactionAssembler {

    private final ExternalIdFactory externalIdFactory;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    public CurrentTransaction assemble(JsonCommand command) {
        throw new UnsupportedCommandException("assemble", "Transaction can not be assembled");
    }

    @Override
    public Map<String, Object> update(CurrentTransaction account, JsonCommand command) {
        throw new UnsupportedCommandException("update", "Transaction can not be updated");
    }

    @Override
    public CurrentTransaction deposit(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        return assemble(command, account, CurrentTransactionType.DEPOSIT);
    }

    @Override
    public CurrentTransaction withdrawal(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        return assemble(command, account, CurrentTransactionType.WITHDRAWAL);
    }

    @Override
    public CurrentTransaction hold(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        return assemble(command, account, CurrentTransactionType.AMOUNT_HOLD);
    }

    @Override
    public CurrentTransaction release(CurrentAccount account, CurrentTransaction holdTransaction, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.create();
        LocalDate actualDate = DateUtils.getBusinessLocalDate();

        CurrentTransaction transaction = CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY),
                holdTransaction.getId(), holdTransaction.getPaymentTypeId(), CurrentTransactionType.AMOUNT_RELEASE, actualDate, actualDate,
                holdTransaction.getAmount());
        return currentTransactionRepository.save(transaction);
    }

    @NotNull
    private CurrentTransaction assemble(JsonCommand command, CurrentAccount account, CurrentTransactionType deposit) {
        ExternalId externalId = externalIdFactory.createFromCommand(command, EXTERNAL_ID_PARAM);
        final Long paymentTypeId = command.longValueOfParameterNamed(PaymentDetailConstants.paymentTypeParamName);

        LocalDate transactionDate = command.localDateValueOfParameterNamed(TRANSACTION_DATE_PARAM);
        if (transactionDate == null) {
            transactionDate = DateUtils.getBusinessLocalDate();
        }
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(TRANSACTION_AMOUNT_PARAM);
        LocalDate submittedOnDate = DateUtils.getBusinessLocalDate();

        CurrentTransaction transaction = CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), null,
                paymentTypeId, deposit, transactionDate, submittedOnDate, transactionAmount);
        transaction = currentTransactionRepository.save(transaction);

        persistDatatableEntries(EntityTables.CURRENT_TRANSACTION, transaction.getId(), command, false, readWriteNonCoreDataService);
        return transaction;
    }
}
