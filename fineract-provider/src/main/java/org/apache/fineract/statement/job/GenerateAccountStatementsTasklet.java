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
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.data.AccountStatementGenerationData;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.apache.fineract.statement.service.AccountStatementGenerationWriteService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor // jobparam: contribution.getStepExecution().getJobParameters().getParameters(), value:
                         // chunkContext.getStepContext().getJobParameters(), jobparam:
                         // chunkContext.getStepContext().getStepExecution().getJobParameters().getParameters()
public class GenerateAccountStatementsTasklet implements Tasklet {

    private final AccountStatementServiceProvider statementServiceProvider;
    private final PlatformSecurityContext securityContext;

    @Override
    public RepeatStatus execute(@NotNull StepContribution contribution, @NotNull ChunkContext chunkContext) throws Exception {
        // AppUser user = securityContext.authenticatedUser();
        // user.validateHasCreatePermission(AccountStatementService.ENTITY_NAME_STATEMENT_RESULT);

        String deleteResultS = (String) chunkContext.getStepContext().getJobParameters().get("auto-delete-result");
        boolean deleteResult = Strings.isEmpty(deleteResultS) || Boolean.parseBoolean(deleteResultS);
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        for (PortfolioProductType productType : PortfolioProductType.values()) {
            AccountStatementGenerationReadService readService = statementServiceProvider
                    .findAccountStatementGenerationReadService(productType);
            if (readService == null) {
                continue;
            }
            Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> generationsMap = readService
                    .retrieveStatementsToGenerate(productType, transactionDate);
            for (StatementType statementType : generationsMap.keySet()) {
                Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>> byPublishType = generationsMap
                        .get(statementType);
                for (StatementPublishType publishType : byPublishType.keySet()) {
                    AccountStatementGenerationWriteService writeService = statementServiceProvider
                            .getAccountStatementGenerationWriteService(productType, statementType, publishType);
                    Map<String, List<AccountStatementGenerationData>> byBatchKey = byPublishType.get(publishType);
                    for (List<AccountStatementGenerationData> generationBatch : byBatchKey.values()) {
                        writeService.generateStatementBatch(productType, statementType, publishType, generationBatch, deleteResult);
                    }
                }
            }
        }
        return RepeatStatus.FINISHED;
    }
}
