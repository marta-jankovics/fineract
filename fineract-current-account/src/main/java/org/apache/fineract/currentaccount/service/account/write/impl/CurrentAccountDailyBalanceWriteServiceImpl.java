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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.BALANCE_CALCULATION;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountDailyBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountDailyBalanceRepository;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceWriteService;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountDailyBalanceWriteServiceImpl implements CurrentAccountDailyBalanceWriteService {

    private final CurrentAccountRepository accountRepository;
    private final CurrentAccountDailyBalanceRepository dailyBalanceRepository;
    private final CurrentAccountDailyBalanceReadService dailyBalanceReadService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDailyBalance(@NotNull String accountId, @NotNull LocalDate balanceDate) {
        final CurrentAccountData account = accountRepository.getAccountDataById(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId);
        }
        if (!account.isEnabled(BALANCE_CALCULATION)) {
            return;
        }
        if (DateUtils.isBefore(balanceDate, account.getActivatedOnDate())) {
            return;
        }
        ICurrentAccountDailyBalance balance = dailyBalanceReadService.getDailyBalance(accountId, balanceDate);
        if (balance instanceof CurrentAccountDailyBalanceData) {
            saveBalance((CurrentAccountDailyBalanceData) balance);
        }
    }

    private void saveBalance(@NotNull CurrentAccountDailyBalanceData balanceData) {
        CurrentAccountDailyBalance balance = new CurrentAccountDailyBalance(balanceData.getAccountId(), balanceData.getAccountBalance(),
                balanceData.getHoldAmount(), balanceData.getBalanceDate());
        dailyBalanceRepository.save(balance);
    }
}
