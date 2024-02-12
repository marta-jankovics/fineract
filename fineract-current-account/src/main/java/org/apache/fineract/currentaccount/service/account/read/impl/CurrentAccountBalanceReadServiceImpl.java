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
package org.apache.fineract.currentaccount.service.account.read.impl;

import com.google.common.collect.Iterables;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.BalanceCalculationData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountBalance;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceReadServiceImpl implements CurrentAccountBalanceReadService {

    private final ConfigurationDomainService configurationService;
    private final CurrentAccountBalanceRepository currentAccountBalanceRepository;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    @NotNull
    public OffsetDateTime getBalanceCalculationTill() {
        OffsetDateTime tillDateTime = DateUtils.getAuditOffsetDateTime();
        long delay = configurationService.getBalanceCalculationDelaySeconds();
        if (delay > 0) {
            tillDateTime = tillDateTime.minusSeconds(delay);
        }
        return tillDateTime;
    }

    @Override
    @NotNull
    public CurrentAccountBalanceData getCurrentBalance(@NotNull String accountId) {
        CurrentAccountBalanceData accountBalance = currentAccountBalanceRepository.getBalanceDataByAccountId(accountId);
        if (accountBalance == null) {
            accountBalance = new CurrentAccountBalanceData(null, accountId, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }
        // TODO CURRENT! filter transactions by type
        OffsetDateTime calculatedTill = accountBalance.getCalculatedTill();
        List<CurrentTransaction> transactions = calculatedTill == null ? currentTransactionRepository.getTransactions(accountId)
                : currentTransactionRepository.getTransactionsFrom(accountId, calculatedTill);
        transactions.forEach(accountBalance::applyTransaction);
        return accountBalance;
    }

    @Override
    @NotNull
    public BalanceCalculationData calculateBalance(@NotNull String accountId, OffsetDateTime delayDateTime) {
        CurrentAccountBalanceData accountBalance = currentAccountBalanceRepository.getBalanceDataByAccountId(accountId);
        Long id = accountBalance == null ? null : accountBalance.getId();
        OffsetDateTime calculatedTill = accountBalance == null ? null : accountBalance.getCalculatedTill();
        // TODO CURRENT! filter transactions by type
        List<CurrentTransaction> transactions = calculatedTill == null ? currentTransactionRepository.getTransactionsSorted(accountId)
                : currentTransactionRepository.getTransactionsFromSorted(accountId, calculatedTill);
        CurrentAccountBalanceData delayData = null;
        CurrentAccountBalanceData totalData;
        if (delayDateTime != null) {
            List<CurrentTransaction> delayTransactions;
            List<CurrentTransaction> totalTransactions;
            int idx = Iterables.indexOf(transactions, t -> DateUtils.isAfter(t.getCreatedDateTime(), delayDateTime));
            if (idx < 0) {
                delayTransactions = transactions;
                totalTransactions = Collections.emptyList();
            } else {
                delayTransactions = transactions.subList(0, idx);
                totalTransactions = transactions.subList(idx, transactions.size());
            }
            delayData = calculateData(id, accountId, delayTransactions, accountBalance, calculatedTill);
            totalData = calculateData(id, accountId, totalTransactions, delayData, delayDateTime);
        } else {
            totalData = calculateData(id, accountId, transactions, accountBalance, calculatedTill);
        }
        return new BalanceCalculationData(id, accountId, delayData, totalData);
    }

    private CurrentAccountBalanceData calculateData(Long balanceId, @NotNull String accountId,
            @NotNull List<CurrentTransaction> transactions, ICurrentAccountBalance baseBalance, OffsetDateTime calculatedTill) {
        BigDecimal accountBalance = null;
        BigDecimal holdAmount = null;
        String transactionId = null;
        boolean changed = false;
        if (baseBalance != null) {
            accountBalance = baseBalance.getAccountBalance();
            holdAmount = baseBalance.getHoldAmount();
            transactionId = baseBalance.getTransactionId();
            changed = baseBalance.isChanged();
        }
        if (!transactions.isEmpty()) {
            CurrentTransaction lastTransaction = transactions.get(transactions.size() - 1);
            calculatedTill = lastTransaction.getCreatedDateTime();
            transactionId = lastTransaction.getId();
            changed = true;
        }
        CurrentAccountBalanceData nextData = new CurrentAccountBalanceData(balanceId, accountId, accountBalance, holdAmount, transactionId,
                calculatedTill, changed);
        transactions.forEach(nextData::applyTransaction);
        return nextData;
    }
}
