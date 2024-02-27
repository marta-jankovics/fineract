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
package org.apache.fineract.currentaccount.service.account.write.impl;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountDailyBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountDailyBalanceRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountDailyBalanceReadServiceImpl implements CurrentAccountDailyBalanceReadService {

    private final CurrentAccountDailyBalanceRepository dailyBalanceRepository;
    private final CurrentTransactionRepository transactionRepository;

    @Override
    @Transactional()
    @NotNull
    public ICurrentAccountDailyBalance getDailyBalance(@NotNull String accountId, @NotNull LocalDate balanceDate) {
        CurrentAccountDailyBalance latestBalance = dailyBalanceRepository.getLatestDailyBalanceTill(accountId, balanceDate);
        if (latestBalance != null && DateUtils.isEqual(balanceDate, latestBalance.getBalanceDate())) {
            return latestBalance;
        }
        List<CurrentTransactionType> types = CurrentTransactionType.getFiltered(t -> t.isDebit() || t.isCredit());
        List<CurrentTransaction> transactions = latestBalance == null
                ? transactionRepository.getTransactionsSubmittedTo(accountId, balanceDate, types)
                : transactionRepository.getTransactionsSubmittedFromTo(accountId, latestBalance.getBalanceDate(), balanceDate, types);
        BigDecimal accountBalance = latestBalance == null ? BigDecimal.ZERO : latestBalance.getAccountBalance();
        BigDecimal holdAmount = latestBalance == null ? BigDecimal.ZERO : latestBalance.getHoldAmount();
        CurrentAccountDailyBalanceData balance = new CurrentAccountDailyBalanceData(null, accountId, accountBalance, holdAmount,
                balanceDate);
        transactions.forEach(balance::applyTransaction);
        return balance;
    }
}
