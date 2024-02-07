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

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
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
    public OffsetDateTime getBalanceCalculationTill() {
        OffsetDateTime tillDateTime = DateUtils.getAuditOffsetDateTime();
        long delay = configurationService.getBalanceCalculationDelaySeconds();
        if (delay > 0) {
            tillDateTime = tillDateTime.minusSeconds(delay);
        }
        return tillDateTime;
    }

    @Override
    public CurrentAccountBalanceData getBalance(@NotNull String accountId) {
        return getBalance(accountId, null);
    }

    @Override
    public CurrentAccountBalanceData getBalance(@NotNull String accountId, OffsetDateTime tillDateTime) {
        CurrentAccountBalanceData balanceData = currentAccountBalanceRepository.getBalanceData(accountId);
        OffsetDateTime fromDateTime = null;
        BigDecimal balance = null;
        BigDecimal holdAmount = null;
        OffsetDateTime calculatedTillDate;
        if (balanceData != null) {
            fromDateTime = balanceData.getCalculatedTill();
            balance = balanceData.getAccountBalance();
            holdAmount = balanceData.getHoldAmount();
        }
        List<CurrentTransaction> transactions; // TODO CURRENT! calculate data in the sql, no need to load transactions
                                               // + filter transactions by type
        if (fromDateTime == null && tillDateTime == null) {
            transactions = currentTransactionRepository.getByAccountIdOrderByCreatedDateAscIdAsc(accountId);
        } else if (fromDateTime == null) {
            transactions = currentTransactionRepository.getTransactionsTill(accountId, tillDateTime);
        } else if (tillDateTime == null) {
            transactions = currentTransactionRepository.getTransactionsFrom(accountId, fromDateTime);
        } else {
            transactions = currentTransactionRepository.getTransactionsFromAndTill(accountId, fromDateTime, tillDateTime);
        }
        if (transactions.isEmpty()) {
            return balanceData;
        }

        CurrentTransaction lastTransaction = transactions.get(transactions.size() - 1);
        CurrentAccountBalanceData result = new CurrentAccountBalanceData(null, accountId, balance, holdAmount,
                lastTransaction.getCreatedDateTime(), lastTransaction.getId(), true);
        transactions.forEach(result::applyTransaction);

        // TODO CURRENT! use Money to set balance data everywhere!!! set scale
        return result;
    }
}
