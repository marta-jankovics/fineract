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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceReadServiceImpl implements CurrentAccountBalanceReadService {

    private final CurrentAccountBalanceRepository currentAccountBalanceRepository;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    public CurrentAccountBalanceData getBalance(UUID accountId) {
        return calculateBalance(accountId, () -> currentTransactionRepository.getTransactions(accountId),
                (OffsetDateTime fromDateTime) -> currentTransactionRepository.getTransactionsFrom(accountId, fromDateTime));
    }

    @Override
    public CurrentAccountBalanceData getBalance(UUID accountId, OffsetDateTime tillDateTime) {
        return calculateBalance(accountId, () -> currentTransactionRepository.getTransactions(accountId, tillDateTime),
                (OffsetDateTime fromDateTime) -> currentTransactionRepository.getTransactionsFromAndTill(accountId, fromDateTime,
                        tillDateTime));
    }

    private CurrentAccountBalanceData calculateBalance(UUID accountId, Supplier<List<CurrentTransaction>> fetchTransactions,
            Function<OffsetDateTime, List<CurrentTransaction>> fetchTransactionsFrom) {
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceRepository.getBalance(accountId);
        List<CurrentTransaction> currentTransactionDataList;
        BigDecimal accountBalance;
        BigDecimal holdAmount;
        OffsetDateTime calculatedTillDate;
        UUID calculatedTillTxnId;
        if (currentAccountBalanceData == null) {
            accountBalance = BigDecimal.ZERO;
            holdAmount = BigDecimal.ZERO;
            currentTransactionDataList = fetchTransactions.get();
            if (currentTransactionDataList.isEmpty()) {
                calculatedTillDate = null;
                calculatedTillTxnId = null;
            } else {
                calculatedTillDate = currentTransactionDataList.get(currentTransactionDataList.size() - 1).getCreatedDateTime();
                calculatedTillTxnId = currentTransactionDataList.get(currentTransactionDataList.size() - 1).getId();
            }
        } else {
            accountBalance = currentAccountBalanceData.getAccountBalance();
            holdAmount = currentAccountBalanceData.getHoldAmount();
            OffsetDateTime fromDateTime = currentAccountBalanceData.getCalculatedTill();
            calculatedTillDate = currentAccountBalanceData.getCalculatedTill();
            calculatedTillTxnId = currentAccountBalanceData.getCalculatedTillTransactionId();
            currentTransactionDataList = fetchTransactionsFrom.apply(fromDateTime);
        }

        for (CurrentTransaction currentTransactionData : currentTransactionDataList) {
            switch (currentTransactionData.getTransactionType()) {
                case DEPOSIT -> accountBalance = accountBalance.add(currentTransactionData.getTransactionAmount());
                case WITHDRAWAL -> accountBalance = accountBalance.subtract(currentTransactionData.getTransactionAmount());
                case AMOUNT_HOLD -> holdAmount = holdAmount.add(currentTransactionData.getTransactionAmount());
                case AMOUNT_RELEASE -> holdAmount = holdAmount.subtract(currentTransactionData.getTransactionAmount());
                default -> throw new UnsupportedOperationException(currentTransactionData.getTransactionType().toString());
            }
            calculatedTillDate = currentTransactionData.getCreatedDateTime();
            calculatedTillTxnId = currentTransactionData.getId();
        }
        return new CurrentAccountBalanceData(null, accountId, accountBalance, holdAmount, calculatedTillDate, calculatedTillTxnId);
    }

    @Override
    public List<UUID> getAccountIdsWhereBalanceRecalculationRequired(OffsetDateTime tillDateTime) {
        return currentAccountBalanceRepository.getAccountIdsWhereBalanceRecalculationRequired(tillDateTime);
    }

    @Override
    public List<UUID> getAccountIdsWhereBalanceNotCalculated() {
        return currentAccountBalanceRepository.getAccountIdsWhereBalanceNotCalculated();
    }
}
