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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceSnapshotRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceReadServiceImpl implements CurrentAccountBalanceReadService {

    private final CurrentAccountBalanceSnapshotRepository currentAccountBalanceSnapshotRepository;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    public CurrentAccountBalanceData getBalance(UUID accountId) {
        CurrentAccountBalanceData currentAccountBalanceSnapshotData = currentAccountBalanceSnapshotRepository.getBalance(accountId);
        List<CurrentTransactionData> currentTransactionDataList;
        BigDecimal availableBalance;
        BigDecimal totalOnHoldBalance;
        OffsetDateTime calculatedTillDate;
        UUID calculatedTillTxnId;
        if (currentAccountBalanceSnapshotData == null) {
            availableBalance = BigDecimal.ZERO;
            totalOnHoldBalance = BigDecimal.ZERO;
            currentTransactionDataList = currentTransactionRepository.getTransactions(accountId);
            if (currentTransactionDataList.isEmpty()) {
                calculatedTillDate = null;
                calculatedTillTxnId = null;
            } else {
                calculatedTillDate = currentTransactionDataList.get(currentTransactionDataList.size() - 1).getCreatedDateTime();
                calculatedTillTxnId = currentTransactionDataList.get(currentTransactionDataList.size() - 1).getId();
            }
        } else {
            availableBalance = currentAccountBalanceSnapshotData.getAvailableBalance();
            totalOnHoldBalance = currentAccountBalanceSnapshotData.getTotalOnHoldBalance();
            calculatedTillDate = currentAccountBalanceSnapshotData.getCalculatedTill();
            currentTransactionDataList = currentTransactionRepository.getTransactionsFrom(accountId, calculatedTillDate);
            calculatedTillTxnId = currentTransactionDataList.get(currentTransactionDataList.size() - 1).getId();
        }

        for (CurrentTransactionData currentTransactionData : currentTransactionDataList) {
            switch (currentTransactionData.getTransactionType()) {
                case DEPOSIT -> availableBalance = availableBalance.add(currentTransactionData.getTransactionAmount());
                case WITHDRAWAL -> availableBalance = availableBalance.subtract(currentTransactionData.getTransactionAmount());
                case AMOUNT_HOLD -> {

                    totalOnHoldBalance = totalOnHoldBalance.add(currentTransactionData.getTransactionAmount());
                    availableBalance = availableBalance.subtract(currentTransactionData.getTransactionAmount());
                }
                case AMOUNT_RELEASE -> {
                    totalOnHoldBalance = totalOnHoldBalance.subtract(currentTransactionData.getTransactionAmount());
                    availableBalance = availableBalance.add(currentTransactionData.getTransactionAmount());
                }
                default -> throw new UnsupportedOperationException(currentTransactionData.getTransactionType().toString());
            }
        }

        return new CurrentAccountBalanceData(null, accountId, availableBalance, totalOnHoldBalance, calculatedTillDate,
                calculatedTillTxnId);
    }
}
