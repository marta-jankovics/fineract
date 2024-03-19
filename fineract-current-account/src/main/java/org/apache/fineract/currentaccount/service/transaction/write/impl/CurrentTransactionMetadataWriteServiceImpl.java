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
package org.apache.fineract.currentaccount.service.transaction.write.impl;

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.METADATA_GENERATION;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataWriteService;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionMetadataWriteServiceImpl implements CurrentTransactionMetadataWriteService {

    public static final DateTimeFormatter METADATA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String METADATA_SEPARATOR = "_";

    private final CurrentAccountRepository accountRepository;
    private final CurrentTransactionRepository transactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assignMetadata(@NotNull String accountId, @NotNull List<String> transactionIds, OffsetDateTime tillDateTime) {
        final CurrentAccountData account = accountRepository.getAccountDataById(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId);
        }
        if (!account.isEnabled(METADATA_GENERATION)) {
            return;
        }
        List<CurrentTransaction> transactions = tillDateTime == null
                ? transactionRepository.getTransactionsForMetadata(accountId, transactionIds)
                : transactionRepository.getTransactionsForMetadataTill(accountId, transactionIds, tillDateTime);
        int sequence = 0;
        LocalDate submittedOnDate = null;
        for (CurrentTransaction transaction : transactions) {
            if (submittedOnDate == null) {
                Integer dbSequence = transactionRepository.getMaxSequenceNo(accountId, transaction.getSubmittedOnDate());
                sequence = dbSequence == null ? 1 : dbSequence + 1;
            } else if (DateUtils.isAfter(transaction.getSubmittedOnDate(), submittedOnDate)) {
                sequence = 1;
            } else {
                sequence++;
            }
            submittedOnDate = transaction.getSubmittedOnDate();

            String name = calculateTransactionName(account, transaction, sequence);
            transaction.setSequenceNo(sequence);
            transaction.setTransactionName(name);
        }
    }

    @NotNull
    protected String calculateTransactionName(@NotNull CurrentAccountData account, @NotNull CurrentTransaction transaction, int sequence) {
        return transaction.getId().substring(0, 8) + METADATA_SEPARATOR + METADATA_DATE_FORMATTER.format(transaction.getSubmittedOnDate())
                + METADATA_SEPARATOR + String.format("%06d", sequence);
    }
}
