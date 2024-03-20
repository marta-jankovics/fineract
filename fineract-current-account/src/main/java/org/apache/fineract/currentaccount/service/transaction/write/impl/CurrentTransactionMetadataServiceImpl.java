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
import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.domain.transaction.ICurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataService;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.transaction.data.TransactionParamData;
import org.apache.fineract.portfolio.transaction.domain.TransactionParam;
import org.apache.fineract.portfolio.transaction.domain.TransactionParamRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionMetadataServiceImpl implements CurrentTransactionMetadataService {

    public static final DateTimeFormatter METADATA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String METADATA_SEPARATOR = "_";

    private final CurrentAccountRepository accountRepository;
    private final CurrentTransactionRepository transactionRepository;
    private final TransactionParamRepository transactionParamRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assignMetadata(@NotNull String accountId, @NotNull List<String> transactionIds) {
        final CurrentAccountData account = accountRepository.getAccountDataById(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId);
        }
        if (!account.isEnabled(METADATA_GENERATION)) {
            return;
        }
        List<CurrentTransaction> transactions = transactionRepository.getTransactionsForMetadata(accountId, transactionIds);
        persistBalances(calculateTransactionParams(account, transactions).values());
    }

    @Override
    @NotNull
    public Map<String, TransactionParamData> calculateTransactionParams(@NotNull CurrentAccountData account,
            @NotNull List<? extends ICurrentTransaction> transactions) {
        int sequence = 0;
        LocalDate submittedOnDate = null;
        String accountId = account.getId();
        HashMap<String, TransactionParamData> result = new HashMap<>();
        for (ICurrentTransaction transaction : transactions) {
            LocalDate trSubmitted = transaction.getSubmittedOnDate();
            if (submittedOnDate == null || DateUtils.isAfter(trSubmitted, submittedOnDate)) {
                Integer dbSequence = transactionRepository.getMaxSequenceNo(accountId, trSubmitted);
                sequence = dbSequence == null ? 1 : dbSequence + 1;
            } else {
                sequence++;
            }
            submittedOnDate = trSubmitted;

            String name = calculateTransactionName(account, transaction, sequence);
            result.put(transaction.getId(), new TransactionParamData(null, CURRENT, transaction.getId(), name, sequence));
        }
        return result;
    }

    @NotNull
    protected String calculateTransactionName(@NotNull CurrentAccountData account, @NotNull ICurrentTransaction transaction, int sequence) {
        return transaction.getId().substring(0, 8) + METADATA_SEPARATOR + METADATA_DATE_FORMATTER.format(transaction.getSubmittedOnDate())
                + METADATA_SEPARATOR + String.format("%06d", sequence);
    }

    private void persistBalances(@NotNull Collection<TransactionParamData> params) {
        for (TransactionParamData param : params) {
            TransactionParam transactionParam = new TransactionParam(CURRENT, param.getTransactionId(), param.getTransactionName(),
                    param.getSequenceNo());
            transactionParamRepository.save(transactionParam);
        }
    }
}
