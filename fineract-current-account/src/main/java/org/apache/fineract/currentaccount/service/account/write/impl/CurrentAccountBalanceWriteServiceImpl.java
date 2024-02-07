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
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalance;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceWriteServiceImpl implements CurrentAccountBalanceWriteService {

    private final CurrentAccountBalanceRepository currentAccountBalanceRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBalance(@NotNull String accountId, OffsetDateTime tillDateTime) {
        CurrentAccountAction action = CurrentAccountAction.forActionName(ThreadLocalContextUtil.getCommandAction());
        calculateBalance(accountId, BalanceCalculationType.LAZY, action, tillDateTime);
    }

    @Override
    public CurrentAccountBalanceData calculateBalance(@NotNull String accountId, @NotNull BalanceCalculationType balanceType,
            @NotNull CurrentAccountAction action, OffsetDateTime tillDateTime) {
        boolean save = balanceType.isStrict(action);
        boolean delay = save && !balanceType.isStrict();
        CurrentAccountBalanceData balanceData = currentAccountBalanceReadService.getBalance(accountId, delay ? tillDateTime : null);
        if (save && balanceData != null && balanceData.isChanged()) {
            saveBalance(balanceData);
        }
        if (delay) {
            balanceData = currentAccountBalanceReadService.getBalance(accountId, null);
        }
        return balanceData;
    }

    @Override
    public void saveBalance(@NotNull CurrentAccountBalanceData balanceData) {
        String accountId = balanceData.getAccountId();
        CurrentAccountBalance balance = currentAccountBalanceRepository.findByAccountId(accountId).orElse(null);
        if (balance == null) {
            balance = new CurrentAccountBalance(accountId, balanceData.getAccountBalance(), balanceData.getHoldAmount(),
                    balanceData.getCalculatedTillTransactionId());
        } else {
            balance.setAccountBalance(balanceData.getAccountBalance());
            balance.setHoldAmount(balanceData.getHoldAmount());
            balance.setCalculatedTillTransactionId(balanceData.getCalculatedTillTransactionId());
        }
        currentAccountBalanceRepository.save(balance);
    }
}
