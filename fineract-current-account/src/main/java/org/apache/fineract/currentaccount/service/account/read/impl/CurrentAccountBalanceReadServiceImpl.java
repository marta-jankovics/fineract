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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.BalanceCalculationData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountBalance;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.domain.transaction.ICurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceReadServiceImpl implements CurrentAccountBalanceReadService {

    private final ConfigurationDomainService configurationService;
    private final CurrentAccountBalanceRepository accountBalanceRepository;
    private final CurrentTransactionRepository transactionRepository;
    private final CurrentAccountDailyBalanceReadService dailyBalanceReadService;

    public static final Comparator<? super ICurrentTransaction> TRANSACTION_COMPARATOR = new Comparator<ICurrentTransaction>() {

        @Override
        public int compare(ICurrentTransaction o1, ICurrentTransaction o2) {
            OffsetDateTime cdt1 = o1.getCreatedDateTime();
            OffsetDateTime cdt2 = o2.getCreatedDateTime();
            int cmp = cdt1 == null ? (cdt2 == null ? 0 : 1) : (cdt2 == null ? 1 : cdt1.compareTo(cdt2));
            if (cmp != 0) {
                return cmp;
            }
            String id1 = o1.getId();
            String id2 = o2.getId();
            return id1 == null ? (id2 == null ? 0 : 1) : (id2 == null ? 1 : id1.compareTo(id2));
        }
    };

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
        CurrentAccountBalanceData accountBalance = accountBalanceRepository.getBalanceDataByAccountId(accountId);
        OffsetDateTime calculatedTill = accountBalance == null ? null : accountBalance.getCalculatedTill();
        // TODO CURRENT! filter transactions by type
        List<CurrentTransaction> transactions = calculatedTill == null ? transactionRepository.getTransactions(accountId)
                : transactionRepository.getTransactionsFrom(accountId, calculatedTill);
        return calculateData(accountId, transactions, accountBalance, calculatedTill);
    }

    @Override
    @NotNull
    public CurrentAccountBalanceData getTransactionBalance(@NotNull ICurrentTransaction transaction) {
        String transactionId = transaction.getId();
        String accountId = transaction.getAccountId();
        CurrentAccountBalanceData balance = accountBalanceRepository.getBalanceDataByAccountId(accountId);
        if (balance != null && balance.getTransactionId() != null && balance.getTransactionId().equals(transactionId)) {
            // existing balance is exactly for this transaction
            return balance;
        }
        OffsetDateTime toDateTime = transaction.getCreatedDateTime();
        List<CurrentTransaction> transactions;
        if (balance == null || balance.getTransactionId() == null) {
            // calculate from the beginning
            transactions = transactionRepository.getTransactionsTillSorted(accountId, toDateTime);
        } else {
            OffsetDateTime fromDateTime = balance.getCalculatedTill();
            if (DateUtils.isBefore(fromDateTime, toDateTime)) {
                // calculate the difference based on the existing balance
                transactions = transactionRepository.getTransactionsFromAndTillSorted(accountId, fromDateTime, toDateTime);
            } else {
                // existing balance is later than the transaction
                // use daily balance from two days before because around midnight there could other transactions exist
                // with submittedDate on yesterday and createdDateTime later than the transaction
                LocalDate balanceDate = transaction.getSubmittedOnDate().minusDays(2);
                // balanceDate might be earlier than account creation/activation, but then it will return empty balance
                ICurrentAccountDailyBalance dailyBalance = dailyBalanceReadService.getDailyBalance(accountId, balanceDate);
                balance = new CurrentAccountBalanceData(null, accountId, dailyBalance.getAccountBalance(), dailyBalance.getHoldAmount(),
                        null, null);
                transactions = transactionRepository.getTransactionsSubmittedFromAndTillSorted(accountId, balanceDate, toDateTime);
            }
        }
        int idx = Iterables.indexOf(transactions, t -> TRANSACTION_COMPARATOR.compare(t, transaction) == 0);
        // must be found or empty balance is returned
        return calculateData(accountId, transactions.subList(0, idx + 1), balance, toDateTime);
    }

    @Override
    @NotNull
    public BalanceCalculationData calculateBalance(@NotNull String accountId, OffsetDateTime delayDateTime) {
        CurrentAccountBalanceData accountBalance = accountBalanceRepository.getBalanceDataByAccountId(accountId);
        Long id = accountBalance == null ? null : accountBalance.getId();
        OffsetDateTime calculatedTill = accountBalance == null ? null : accountBalance.getCalculatedTill();
        // TODO CURRENT! filter transactions by type
        List<CurrentTransaction> transactions = calculatedTill == null ? transactionRepository.getTransactionsSorted(accountId)
                : transactionRepository.getTransactionsFromSorted(accountId, calculatedTill);
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
            delayData = calculateData(accountId, delayTransactions, accountBalance, calculatedTill);
            totalData = calculateData(accountId, totalTransactions, delayData, delayDateTime);
        } else {
            totalData = calculateData(accountId, transactions, accountBalance, calculatedTill);
        }
        return new BalanceCalculationData(id, accountId, delayData, totalData);
    }

    private CurrentAccountBalanceData calculateData(@NotNull String accountId, @NotNull List<CurrentTransaction> transactions,
            ICurrentAccountBalance baseBalance, OffsetDateTime calculatedTill) {
        Long balanceId = null;
        BigDecimal accountBalance = null;
        BigDecimal holdAmount = null;
        String transactionId = null;
        boolean changed = false;
        if (baseBalance != null) {
            balanceId = baseBalance.getId();
            accountBalance = baseBalance.getAccountBalance();
            holdAmount = baseBalance.getHoldAmount();
            transactionId = baseBalance.getTransactionId();
            changed = baseBalance.isChanged();
        }
        if (!transactions.isEmpty()) {
            CurrentTransaction lastTransaction = transactions.get(transactions.size() - 1);
            calculatedTill = lastTransaction.getCreatedDateTime();
            transactionId = lastTransaction.getId();
        }
        CurrentAccountBalanceData nextData = new CurrentAccountBalanceData(balanceId, accountId, accountBalance, holdAmount, transactionId,
                calculatedTill, changed);
        transactions.forEach(nextData::applyTransaction);
        return nextData;
    }
}
