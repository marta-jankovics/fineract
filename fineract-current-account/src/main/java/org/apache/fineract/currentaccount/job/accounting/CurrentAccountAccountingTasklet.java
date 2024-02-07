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
package org.apache.fineract.currentaccount.job.accounting;

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus.ACTIVE;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.repository.accounting.CurrentAccountAccountingRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.accounting.write.CurrentAccountAccountingWriteService;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountAccountingTasklet implements Tasklet {

    private final CurrentAccountAccountingRepository currentAccountAccountingRepository;
    private final CurrentAccountAccountingWriteService currentAccountAccountingWriteService;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            OffsetDateTime tillDateTime = currentAccountBalanceReadService.getBalanceCalculationTill();
            List<CurrentAccountStatus> statuses = List.of(ACTIVE);
            List<String> accountIds = currentAccountAccountingRepository.getAccountIdsAccountingBehind(tillDateTime, statuses);
            accountIds.addAll(currentAccountAccountingRepository.getAccountIdsNoAccounting(statuses));
            writeAccounting(accountIds, tillDateTime);
        } catch (Exception e) {
            throw new JobExecutionException(List.of(e));
        }
        return RepeatStatus.FINISHED;
    }

    private void writeAccounting(List<String> accountIds, OffsetDateTime tillDateTime) {
        for (String accountId : accountIds) {
            try {
                currentAccountAccountingWriteService.createGLEntries(accountId, tillDateTime);
            } catch (Exception e) {
                // We don't care if it failed, the job can continue
                log.warn("Update accounting for current account: {} is failed", accountId);
            }
        }
    }
}
