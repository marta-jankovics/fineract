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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccountBalanceSnapshot;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceSnapshotRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountBalanceWriteServiceImpl implements CurrentAccountBalanceWriteService {

    private final CurrentAccountBalanceSnapshotRepository currentAccountBalanceSnapshotRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;

    @Override
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void updateBalance(UUID accountId, OffsetDateTime tillDateTime) {
        CurrentAccountBalanceSnapshot currentAccountBalanceSnapshot = currentAccountBalanceSnapshotRepository.findByAccountId(accountId).orElse(null);
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(accountId, tillDateTime);
        if (currentAccountBalanceSnapshot == null) {
            currentAccountBalanceSnapshot = new CurrentAccountBalanceSnapshot(null, accountId, currentAccountBalanceData.getAvailableBalance(), currentAccountBalanceData.getTotalOnHoldBalance(), currentAccountBalanceData.getCalculatedTill(), currentAccountBalanceData.getCalculatedTillTransactionId(), 1);
        } else {
            currentAccountBalanceSnapshot.setAvailableBalance(currentAccountBalanceData.getAvailableBalance());
            currentAccountBalanceSnapshot.setTotalOnHoldBalance(currentAccountBalanceData.getTotalOnHoldBalance());
            currentAccountBalanceSnapshot.setCalculatedTill(currentAccountBalanceData.getCalculatedTill());
            currentAccountBalanceSnapshot.setCalculatedTillTransactionId(currentAccountBalanceData.getCalculatedTillTransactionId());
        }
        currentAccountBalanceSnapshotRepository.save(currentAccountBalanceSnapshot);
    }
}
