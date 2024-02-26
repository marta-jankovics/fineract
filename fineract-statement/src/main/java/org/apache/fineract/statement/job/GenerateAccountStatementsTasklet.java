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
package org.apache.fineract.statement.job;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.apache.fineract.statement.service.AccountStatementGenerationWriteService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class GenerateAccountStatementsTasklet implements Tasklet {

    private final AccountStatementServiceProvider statementServiceProvider;

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("Processing {} job", JobName.GENERATE_STATEMENTS);
        String deleteResultS = (String) chunkContext.getStepContext().getJobParameters().get("auto-delete-result");
        boolean deleteResult = Strings.isEmpty(deleteResultS) || Boolean.parseBoolean(deleteResultS);
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        for (PortfolioProductType productType : PortfolioProductType.values()) {
            AccountStatementGenerationReadService readService = statementServiceProvider
                    .findAccountStatementGenerationReadService(productType);
            if (readService == null) {
                log.debug("Read service for {} - {} is not implemented", JobName.GENERATE_STATEMENTS, productType);
                continue;
            }
            log.debug("Processing {} - {}", JobName.GENERATE_STATEMENTS, productType);
            Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> generationsMap = readService
                    .retrieveStatementsToGenerate(productType, transactionDate);
            log.info("Statements to generate for {} were {}", productType, generationsMap.isEmpty() ? "not found" : "found");

            for (Map.Entry<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> statementType : generationsMap
                    .entrySet()) {
                for (Map.Entry<StatementPublishType, Map<String, List<AccountStatementGenerationData>>> publishType : statementType
                        .getValue().entrySet()) {
                    AccountStatementGenerationWriteService writeService = statementServiceProvider
                            .getAccountStatementGenerationWriteService(productType, statementType.getKey(), publishType.getKey());
                    Map<String, List<AccountStatementGenerationData>> byBatchKey = publishType.getValue();
                    log.info("Processing {} statement generation batches for {} - {} - {}", byBatchKey.values().size(), productType,
                            statementType, publishType);
                    for (List<AccountStatementGenerationData> generationBatch : byBatchKey.values()) {
                        writeService.generateStatementBatch(productType, statementType.getKey(), publishType.getKey(), generationBatch,
                                deleteResult);
                    }
                }
            }
        }
        return RepeatStatus.FINISHED;
    }
}
