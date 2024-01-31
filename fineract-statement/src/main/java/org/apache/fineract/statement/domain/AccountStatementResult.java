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
package org.apache.fineract.statement.domain;

import static org.apache.fineract.statement.domain.StatementResultStatus.GENERATED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;

@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "m_account_statement_result")
public final class AccountStatementResult extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Version
    int version;

    @Column(name = "result_code", nullable = false, length = 40)
    private String resultCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 100)
    private PortfolioProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 100)
    private StatementType statementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_type", nullable = false, length = 100)
    private StatementPublishType publishType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private StatementResultStatus resultStatus;

    @Column(name = "result_content", nullable = false)
    private String content;

    @Column(name = "result_metadata", nullable = true)
    private String metadata;

    @Column(name = "result_path", nullable = true)
    private String resultPath;

    @Column(name = "result_name", nullable = true, length = 100)
    private String resultName;

    @Column(name = "generatedon_date", nullable = false)
    private LocalDate generatedOn; // date of generation

    @Column(name = "publishedon_date", nullable = true)
    private LocalDate publishedOn; // date of publication

    private AccountStatementResult(@NotNull String resultCode, @NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType, @NotNull String content, String metadata,
            String resultPath, String resultName, @NotNull LocalDate generatedDate) {
        this.resultCode = resultCode;
        this.productType = productType;
        this.statementType = statementType;
        this.publishType = publishType;
        this.content = content;
        this.metadata = metadata;
        this.resultPath = resultPath;
        this.resultName = resultName;
        this.generatedOn = generatedDate;
        this.resultStatus = GENERATED;
    }

    public static AccountStatementResult create(@NotNull String resultCode, @NotNull PortfolioProductType productType,
            @NotNull StatementType statementType, @NotNull StatementPublishType publishType, @NotNull String content, String metadata,
            String resultPath, String resultName) {
        return new AccountStatementResult(resultCode, productType, statementType, publishType, content, metadata, resultPath, resultName,
                DateUtils.getBusinessLocalDate());
    }

    public boolean canPublish() {
        return resultStatus.canPublish();
    }

    public void published() {
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        setPublishedOn(transactionDate);
        setResultStatus(resultStatus.publish());
    }
}
