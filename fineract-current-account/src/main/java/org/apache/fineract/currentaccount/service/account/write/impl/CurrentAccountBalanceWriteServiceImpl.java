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
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.BalanceCalculationData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountBalance;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceWriteServiceImpl implements CurrentAccountBalanceWriteService {

    private final CurrentAccountRepository accountRepository;
    private final CurrentAccountBalanceRepository accountBalanceRepository;
    private final CurrentAccountBalanceReadService accountBalanceReadService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBalanceInNewTransaction(@NotNull String accountId, OffsetDateTime tillDateTime) {
        updateBalance(accountId, tillDateTime);
    }

    @Override
    public void updateBalance(@NotNull String accountId, OffsetDateTime tillDateTime) {
        final CurrentAccountData account = accountRepository.getAccountDataById(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId);
        }
        if (!account.isEnabled(BALANCE_CALCULATION)) {
            return;
        }
        boolean hasDelay = account.hasBalanceDelay(BALANCE_CALCULATION);
        BalanceCalculationData balanceData = accountBalanceReadService.calculateBalance(accountId, hasDelay ? tillDateTime : null);
        CurrentAccountBalanceData balance = hasDelay ? balanceData.getDelayData() : balanceData.getTotalData();
        if (balance.isChanged()) {
            saveBalance(balance);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveBalance(@NotNull ICurrentAccountBalance iBalance) {
        CurrentAccountBalance balance;
        if (iBalance instanceof CurrentAccountBalance) {
            balance = (CurrentAccountBalance) iBalance;
        } else {
            Long id = iBalance.getId();
            if (id == null) {
                balance = new CurrentAccountBalance(iBalance.getAccountId(), iBalance.getAccountBalance(), iBalance.getHoldAmount(),
                        iBalance.getTransactionId());
            } else {
                balance = accountBalanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("current.account.balance",
                        "Current account balance with id: %s cannot be found", id));
                balance.setAccountBalance(iBalance.getAccountBalance());
                balance.setHoldAmount(iBalance.getHoldAmount());
                balance.setTransactionId(iBalance.getTransactionId());
            }
        }
        accountBalanceRepository.save(balance);
    }
}
