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
package org.apache.fineract.currentaccount.job;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.METADATA_GENERATION;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class CalculateCurrentTransactionMetadataTasklet implements Tasklet {

    private final CurrentTransactionRepository transactionRepository;
    private final ConfigurationDomainService configurationService;
    private final CurrentTransactionMetadataService transactionMetadataService;

    @Override
    @SuppressFBWarnings({ "SLF4J_FORMAT_SHOULD_BE_CONST" })
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Processing {} job", JobName.CALCULATE_CURRENT_TRANSACTION_METADATA);
        HashMap<Throwable, List<String>> errors = new HashMap<>();
        try {
            List<CurrentAccountStatus> statuses = CurrentAccountStatus.getEnabledStatusList(METADATA_GENERATION);
            OffsetDateTime tillDateTime = getMetadataCalculationTill();
            List<String[]> transactionIds = transactionRepository.getTransactionIdsForMetadata(tillDateTime, statuses);
            Map<String, List<String>> byAccountIds = transactionIds.stream().collect(groupingBy(e -> e[0], mapping(e -> e[1], toList())));
            for (Map.Entry<String, List<String>> entry : byAccountIds.entrySet()) {
                List<String> ids = entry.getValue();
                try {
                    transactionMetadataService.assignMetadata(entry.getKey(), ids);
                } catch (Exception e) {
                    // We don't care if it failed, the job can continue
                    log.error(String.format("Calculate transaction metadata for account: %s is failed", entry.getKey()), e);
                    errors.put(e, ids);
                }
            }
        } catch (Exception e) {
            throw new JobExecutionException(List.of(e));
        }
        JobExecutionException.throwErrors(errors);
        return RepeatStatus.FINISHED;
    }

    @NotNull
    public OffsetDateTime getMetadataCalculationTill() {
        OffsetDateTime tillDateTime = DateUtils.getAuditOffsetDateTime();
        long delay = configurationService.getBalanceCalculationDelaySeconds();
        if (delay > 0) {
            tillDateTime = tillDateTime.minusSeconds(delay);
        }
        return tillDateTime;
    }
}
