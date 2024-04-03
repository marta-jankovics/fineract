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
import static lombok.AccessLevel.PROTECTED;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.AccountStatementResult;
import org.apache.fineract.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;

@Slf4j
@AllArgsConstructor(access = PROTECTED)
public abstract class AccountStatementGenerationWriteServiceImpl implements AccountStatementGenerationWriteService {

    private final AccountStatementRepository statementRepository;
    private final AccountStatementResultRepository statementResultRepository;

    @Override
    @Transactional(REQUIRES_NEW)
    public void generateStatementBatch(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
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
        AccountStatementResult statementResult = generateResult(productType, statementType, publishType, statements);
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
    }

    protected AccountStatementResult generateResult(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Map<Long, AccountStatement> statements) {
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();

        Object content = createContent(productType, statementType, publishType, statements, transactionDate);
        Object metadata = createMetadata(productType, statementType, publishType, content, transactionDate);
        String resultCode = calcResultCode(productType, statementType, publishType, content, transactionDate);
        String path = calcResultPath(productType, statementType, publishType, content, transactionDate);
        String name = calcResultName(productType, statementType, publishType, content, transactionDate);
        String contentS = mapContentToString(productType, statementType, publishType, content, transactionDate);
        String metadataS = mapMetadataToString(productType, statementType, publishType, metadata, transactionDate);
        return AccountStatementResult.create(resultCode, productType, statementType, publishType, contentS, metadataS, path, name);
    }

    @NotNull
    protected abstract Object createContent(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Map<Long, AccountStatement> statements, LocalDate transactionDate);

    @NotNull
    protected abstract String mapContentToString(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate);

    protected abstract Object createMetadata(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate);

    protected abstract String mapMetadataToString(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, Object metadata, LocalDate transactionDate);

    @NotNull
    protected abstract String calcResultCode(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate);

    @NotNull
    protected String calcResultPath(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, @NotNull LocalDate transactionDate) {
        int year = transactionDate.get(ChronoField.YEAR);
        int month = transactionDate.get(ChronoField.MONTH_OF_YEAR);
        int day = transactionDate.get(ChronoField.DAY_OF_MONTH);
        return year + File.separator + year + '-' + month + File.separator + year + '-' + month + '-' + day;
    }

    @NotNull
    protected String calcResultName(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate) {
        String messageId = calcResultCode(productType, statementType, publishType, content, transactionDate);
        return productType.name().toLowerCase() + "_" + statementType.name().toLowerCase() + "_" + transactionDate + "_"
                + messageId.replaceAll("[^a-zA-Z0-9!\\-_.'()$]", "_") + ".json";
    }
}
