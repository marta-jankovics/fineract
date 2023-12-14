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
package org.apache.fineract.portfolio.statement.domain;

import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_BATCH_TYPE;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_PUBLISH_TYPE;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_RECURRENCE;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_SEQUENCE_PREFIX;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_STATEMENT_CODE;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_STATEMENT_TYPE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.data.ProductStatementData;

@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "m_product_statement", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_id", "product_type", "statement_code" }, name = "uk_product_statement") })
public class ProductStatement extends AbstractPersistableCustom {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 100)
    private PortfolioProductType productType;

    @Column(name = "statement_code", nullable = false, length = 40)
    private String statementCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 100)
    private StatementType statementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_type", nullable = false, length = 100)
    private StatementPublishType publishType;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", nullable = false, length = 100)
    private StatementBatchType batchType;

    @Column(name = "recurrence", nullable = true, length = 100)
    private String recurrence;

    @Column(name = "sequence_prefix", nullable = true, length = 10)
    private String sequencePrefix;

    public static ProductStatement create(@NotNull ProductStatementData statementData) {
        return new ProductStatement(statementData.getProductId(), statementData.getProductType(), statementData.getStatementCode(),
                Optional.ofNullable(statementData.getStatementType()).orElse(StatementType.getDefault()),
                Optional.ofNullable(statementData.getPublishType()).orElse(StatementPublishType.getDefault()),
                Optional.ofNullable(statementData.getBatchType()).orElse(StatementBatchType.getDefault()), statementData.getRecurrence(),
                statementData.getSequencePrefix());
    }

    public boolean update(@NotNull ProductStatementData statementData, @NotNull Map<String, Object> changes) {
        boolean changed = false;
        String statementCode = statementData.getStatementCode();
        if (statementCode != null && !Objects.equals(this.statementCode, statementCode)) {
            setStatementCode(statementCode);
            changes.put(PARAM_STATEMENT_CODE, statementCode);
            changed = true;
        }
        StatementType statementType = statementData.getStatementType();
        if (statementType != null && this.statementType != statementType) {
            setStatementType(statementType);
            changes.put(PARAM_STATEMENT_TYPE, statementType);
            changed = true;
        }
        StatementPublishType publishType = statementData.getPublishType();
        if (publishType != null && this.publishType != publishType) {
            setPublishType(publishType);
            changes.put(PARAM_PUBLISH_TYPE, publishType);
            changed = true;
        }
        StatementBatchType batchType = statementData.getBatchType();
        if (batchType != null && this.batchType != batchType) {
            setBatchType(batchType);
            changes.put(PARAM_BATCH_TYPE, batchType);
            changed = true;
        }
        String recurrence = statementData.getRecurrence();
        if (recurrence != null && !Objects.equals(this.recurrence, recurrence)) {
            setRecurrence(recurrence);
            changes.put(PARAM_RECURRENCE, recurrence);
            changed = true;
        }
        String sequencePrefix = statementData.getSequencePrefix();
        if (sequencePrefix != null && !Objects.equals(this.sequencePrefix, sequencePrefix)) {
            setSequencePrefix(sequencePrefix);
            changes.put(PARAM_SEQUENCE_PREFIX, sequencePrefix);
            changed = true;
        }
        return changed;
    }
}
