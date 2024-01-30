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
package org.apache.fineract.currentaccount.job.balancecalculation;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class CalculateCurrentAccountBalanceTasklet implements Tasklet {

    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final CurrentAccountBalanceWriteService currentAccountBalanceWriteService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            // TODO: make it configurable
            OffsetDateTime tillDateTime = DateUtils.getAuditOffsetDateTime().minusMinutes(1);
            List<String> currentAccountBalanceIsBehindIds = currentAccountBalanceReadService
                    .getAccountIdsWhereBalanceRecalculationRequired(tillDateTime);
            List<String> currentAccountBalanceNotCalculatedIds = currentAccountBalanceReadService.getAccountIdsWhereBalanceNotCalculated();
            currentAccountBalanceIsBehindIds.addAll(currentAccountBalanceNotCalculatedIds);
            updateBalances(currentAccountBalanceIsBehindIds, tillDateTime);
        } catch (Exception e) {
            throw new JobExecutionException(List.of(e));
        }
        return RepeatStatus.FINISHED;
    }

    private void updateBalances(List<String> currentAccountBalanceIsBehindIds, OffsetDateTime tillDateTime) {
        for (String id : currentAccountBalanceIsBehindIds) {
            try {
                currentAccountBalanceWriteService.updateBalance(id, tillDateTime);
            } catch (Exception e) {
                // We don't care if it failed, the job can continue
                log.warn("Updating account snapshot balance for account: {} is failed", id);
            }
        }
    }
}