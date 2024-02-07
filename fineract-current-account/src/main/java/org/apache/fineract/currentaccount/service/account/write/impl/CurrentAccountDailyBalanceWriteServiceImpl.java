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
import org.apache.fineract.currentaccount.domain.account.CurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountDailyBalanceRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceWriteService;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountDailyBalanceWriteServiceImpl implements CurrentAccountDailyBalanceWriteService {

    private final CurrentAccountDailyBalanceRepository dailyBalanceRepository;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    public void calculateDailyBalance(@NotNull String accountId, @NotNull LocalDate balanceDate) {
        CurrentAccountDailyBalance latestBalance = dailyBalanceRepository.getLatestDailyBalanceBefore(accountId, balanceDate);
        List<CurrentTransactionType> types = CurrentTransactionType.getFiltered(t -> t.isDebit() || t.isCredit());
        List<CurrentTransaction> transactions = latestBalance == null
                ? currentTransactionRepository.getTransactionsSubmittedTo(accountId, balanceDate, types)
                : currentTransactionRepository.getTransactionsSubmittedFromTo(accountId, latestBalance.getBalanceDate(), balanceDate,
                        types);
        if (transactions.isEmpty()) {
            return;
        }
        BigDecimal accountBalance = latestBalance == null ? BigDecimal.ZERO : latestBalance.getAccountBalance();
        BigDecimal holdAmount = latestBalance == null ? BigDecimal.ZERO : latestBalance.getHoldAmount();
        CurrentAccountDailyBalance balance = new CurrentAccountDailyBalance(accountId, accountBalance, holdAmount, balanceDate);
        transactions.forEach(balance::applyTransaction);
        dailyBalanceRepository.save(balance);
    }
}
