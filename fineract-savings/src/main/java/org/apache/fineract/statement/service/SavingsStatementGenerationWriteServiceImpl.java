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

import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;
import static org.apache.fineract.statement.domain.StatementType.CAMT053;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.SavingsCamt053Data;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.StatementMetadata;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SavingsStatementGenerationWriteServiceImpl extends AccountStatementGenerationWriteServiceImpl {

    private final SavingsCamt053StatementGenerator camt053StatementGenerator;

    public SavingsStatementGenerationWriteServiceImpl(AccountStatementRepository statementRepository,
            AccountStatementResultRepository statementResultRepository, SavingsCamt053StatementGenerator savingsCamt053StatementGenerator) {
        super(statementRepository, statementResultRepository);
        this.camt053StatementGenerator = savingsCamt053StatementGenerator;
    }

    @Override
    public boolean isSupport(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType) {
        return productType == SAVING && statementType == CAMT053;
    }

    @Override
    @NotNull
    protected SavingsCamt053Data createContent(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Map<Long, AccountStatement> statements, LocalDate transactionDate) {
        return (SavingsCamt053Data) camt053StatementGenerator.generateContent(productType, publishType, statements);
    }

    @Override
    @NotNull
    protected String mapContentToString(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate) {
        try {
            return camt053StatementGenerator.mapContentToString((Camt053Data) content);
        } catch (JsonProcessingException e) {
            log.error("Statement result json mapping has failed for {} - {} - {}. Reason: {}", productType, statementType, publishType,
                    e.getMessage());
            throw ErrorHandler.getMappable(e);
        }
    }

    @Override
    protected Object createMetadata(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate) {
        return camt053StatementGenerator.generateMetadata(productType, publishType, (Camt053Data) content);
    }

    @Override
    protected String mapMetadataToString(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, Object metadata, LocalDate transactionDate) {
        try {
            return metadata == null ? null : camt053StatementGenerator.mapMetadataToString((StatementMetadata) metadata);
        } catch (JsonProcessingException e) {
            log.error("Statement metadata json mapping has failed for {} - {} - {}. Reason: {}", productType, statementType, publishType,
                    e.getMessage());
            throw ErrorHandler.getMappable(e);
        }
    }

    @Override
    @NotNull
    protected String calcResultCode(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, LocalDate transactionDate) {
        return ((Camt053Data) content).getGroupHeader().getMessageIdentification();
    }

    @Override
    @NotNull
    protected String calcResultPath(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull Object content, @NotNull LocalDate transactionDate) {
        String path = super.calcResultPath(productType, statementType, publishType, content, transactionDate);
        SavingsCamt053Data savingsData = (SavingsCamt053Data) content;
        path += File.separator + (savingsData.isConversionAccount() ? "conversion_account"
                : (savingsData.isDisposalAccount() ? "disposal_account" : "account"));
        return path;
    }
}
