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
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.data.AccountStatementPublishData;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.apache.fineract.statement.service.AccountStatementPublishReadService;
import org.apache.fineract.statement.service.AccountStatementPublisher;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class PublishAccountStatementsTasklet implements Tasklet {

    private final AccountStatementServiceProvider statementServiceProvider;
    private final PlatformSecurityContext securityContext;

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        log.info("Processing {} job", JobName.PUBLISH_STATEMENTS);
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        for (PortfolioProductType productType : PortfolioProductType.values()) {
            AccountStatementPublishReadService readService = statementServiceProvider.findAccountStatementPublishReadService(productType);
            if (readService == null) {
                log.debug("Read service for {} - {} is not implemented", JobName.PUBLISH_STATEMENTS, productType);
                continue;
            }
            log.debug("Processing {} - {}", JobName.PUBLISH_STATEMENTS, productType);
            Map<StatementType, Map<StatementPublishType, List<AccountStatementPublishData>>> generationsMap = readService
                    .retrieveStatementsToPublish(productType, transactionDate);
            log.info("Statements to publish for {} were {}", productType, generationsMap.isEmpty() ? "not found" : "found");

            for (StatementType statementType : generationsMap.keySet()) {
                Map<StatementPublishType, List<AccountStatementPublishData>> byPublishType = generationsMap.get(statementType);
                for (StatementPublishType publishType : byPublishType.keySet()) {
                    AccountStatementPublisher publisher = statementServiceProvider.getAccountStatementPublishWriteService(productType,
                            statementType, publishType);
                    List<AccountStatementPublishData> publishBatch = byPublishType.get(publishType);
                    log.info("Processing publish statement batch for {} - {} - {}", productType, statementType, publishType);
                    publisher.publish(productType, statementType, publishType, publishBatch);
                }
            }
        }
        return RepeatStatus.FINISHED;
    }
}