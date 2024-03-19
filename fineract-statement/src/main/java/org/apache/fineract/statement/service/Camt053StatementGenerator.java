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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.StatementMetadata;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.StatementPublishType;

@Slf4j
public abstract class Camt053StatementGenerator {

    public static final JsonMapper JSON_MAPPER = JsonMapper.builder().serializationInclusion(NON_NULL).addModule(new JavaTimeModule())
            .build();

    @NotNull
    @Transactional
    public Camt053Data generateContent(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType,
            @NotNull Map<Long, AccountStatement> statements) {
        Camt053Data camt053 = createCamt053Data(productType, publishType);
        for (AccountStatement statement : statements.values()) {
            log.debug("Generating statement result for id {}", statement.getId());
            statement.generate(); // validation
            addStatementData(statement, camt053);
        }
        return camt053;
    }

    @NotNull
    protected GroupHeaderData createHeader(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType) {
        OffsetDateTime creationDateTime = DateUtils.getAuditOffsetDateTime();
        String messageId = UUID.randomUUID().toString();
        return new GroupHeaderData(messageId, creationDateTime);
    }

    @NotNull
    protected Camt053Data createCamt053Data(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType) {
        GroupHeaderData header = createHeader(productType, publishType);
        return new Camt053Data(header);
    }

    protected abstract void addStatementData(@NotNull AccountStatement statement, @NotNull Camt053Data content);

    protected String calcIdentification(@NotNull AccountStatement statement) {
        LocalDate generationDate = statement.getNextStatementDate();
        String pfx = statement.getSequencePrefix();
        int year = generationDate.getYear();
        String seq = StringUtils.leftPad(statement.getSequenceNo().toString(), 2, '0');
        String identification = pfx == null ? format("%s/%s", year, seq) : format("%s-%s/%s", pfx, year, seq);
        return identification;
    }

    @NotNull
    public String mapContentToString(@NotNull Camt053Data content) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(content);
    }

    @NotNull
    public JsonNode mapContentToJson(@NotNull Camt053Data content) throws JsonProcessingException {
        return JSON_MAPPER.valueToTree(content);
    }

    public Object generateMetadata(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType,
            @NotNull Camt053Data content) {
        StatementMetadata metadata = createMetadata(productType, publishType);
        for (StatementData statementData : content.getStatements()) {
            addStatementMetadata(statementData, metadata);
        }
        return metadata;
    }

    @NotNull
    protected StatementMetadata createMetadata(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType) {
        return new StatementMetadata();
    }

    protected void addStatementMetadata(@NotNull StatementData statementData, @NotNull StatementMetadata metadata) {
        log.debug("Generating statement metadata for id {}", statementData.getIdentification());
        String customerId = Strings.nullToEmpty(statementData.getCustomerId());
        String accountId = Strings.nullToEmpty(statementData.getAccountId());
        String iban = Strings.nullToEmpty(statementData.getIban());
        String accountType = statementData.getAccountType();
        String currency = statementData.getAccount().getCurrency();
        metadata.add(customerId, accountId, iban, accountType, currency, statementData.getFromDate(), statementData.getToDate());
    }

    @NotNull
    public String mapMetadataToString(@NotNull StatementMetadata metadata) throws JsonProcessingException {
        metadata.emptyToNull();
        return JSON_MAPPER.writeValueAsString(metadata);
    }

    @NotNull
    public JsonNode mapMetadataToJson(@NotNull StatementMetadata metadata) {
        metadata.emptyToNull();
        return JSON_MAPPER.valueToTree(metadata);
    }

    @NotNull
    public String calcResultCode(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType,
            @NotNull Object content, LocalDate transactionDate) {
        return ((Camt053Data) content).getGroupHeader().getMessageIdentification();
    }

    @NotNull
    public String calcResultPath(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType,
            @NotNull Object content, @NotNull LocalDate transactionDate) {
        int year = transactionDate.get(ChronoField.YEAR);
        int month = transactionDate.get(ChronoField.MONTH_OF_YEAR);
        int day = transactionDate.get(ChronoField.DAY_OF_MONTH);
        String accountType = Optional.ofNullable(((Camt053Data) content).getAccountType()).orElse(productType.name().toLowerCase());
        return year + File.separator + year + '-' + month + File.separator + year + '-' + month + '-' + day + File.separator + accountType;
    }
}
