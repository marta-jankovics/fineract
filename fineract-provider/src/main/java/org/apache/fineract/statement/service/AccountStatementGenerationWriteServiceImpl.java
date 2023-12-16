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
package org.apache.fineract.statement.service;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.products.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.statement.data.AccountStatementGenerationData;
import org.apache.fineract.portfolio.statement.domain.AccountStatement;
import org.apache.fineract.portfolio.statement.domain.AccountStatementRepository;
import org.apache.fineract.portfolio.statement.domain.AccountStatementResult;
import org.apache.fineract.portfolio.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;

@Slf4j
public abstract class AccountStatementGenerationWriteServiceImpl implements AccountStatementGenerationWriteService {

    private final AccountStatementRepository statementRepository;
    private final AccountStatementResultRepository statementResultRepository;

    protected AccountStatementGenerationWriteServiceImpl(AccountStatementRepository statementRepository,
            AccountStatementResultRepository statementResultRepository) {
        this.statementRepository = statementRepository;
        this.statementResultRepository = statementResultRepository;
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Response generateStatementBatch(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull List<AccountStatementGenerationData> generationBatch,
            boolean deleteResult) {
        HashMap<Long, AccountStatement> statements = new HashMap<>();
        HashMap<Long, AccountStatementResult> existingResults = new HashMap<>();
        for (AccountStatementGenerationData statementGeneration : generationBatch) {
            Long statementId = statementGeneration.getAccountStatementId();
            AccountStatement statement = statementRepository.findById(statementId)
                    .orElseThrow(() -> new ResourceNotFoundException("account.statement", statementId.toString()));
            statements.put(statementId, statement);
            Optional.ofNullable(statement.getStatementResult()).ifPresent(e -> existingResults.put(e.getId(), e));
        }
        AccountStatementResult statementResult = generateResult(productType, statementType, publishType, generationBatch, statements);
        for (AccountStatement statement : statements.values()) {
            statement.generated(statementResult);
            log.info("Statement result generated for id {}", statement.getId());
            statementRepository.save(statement);
        }
        if (deleteResult) {
            Set<Long> statementIds = statements.keySet();
            for (AccountStatementResult existingResult : existingResults.values()) {
                if (existingResult.getResultStatus().isPublished()
                        || !statementResultRepository.hasAccountReference(existingResult.getId(), statementIds)) {
                    log.info("Delete existing statement result {}", existingResult.getId());
                    statementResultRepository.delete(existingResult);
                }
            }
        }

        return Response.ok().entity(statementResult.getContent()).build();
    }

    protected abstract AccountStatementResult generateResult(@NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType,
            @NotNull List<AccountStatementGenerationData> generationBatch, @NotNull HashMap<Long, AccountStatement> statements);
}
