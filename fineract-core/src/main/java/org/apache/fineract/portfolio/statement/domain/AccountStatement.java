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

import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_RECURRENCE;
import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_SEQUENCE_PREFIX;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.statement.data.AccountStatementData;

@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "m_account_statement", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_statement_id", "account_id" }, name = "uk_account_statement") })
public class AccountStatement extends AbstractAuditableWithUTCDateTimeCustom {

    @Version
    int version;

    @ManyToOne
    @JoinColumn(name = "product_statement_id", nullable = false)
    private ProductStatement productStatement;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "recurrence", nullable = true, length = 100)
    private String recurrence;

    @Column(name = "sequence_prefix", nullable = true, length = 10)
    private String sequencePrefix;

    @Column(name = "last_date")
    private LocalDate lastDate;

    @Column(name = "next_date")
    private LocalDate nextDate;

    @Column(name = "sequence_no", precision = 4)
    private Integer sequenceNo;

    public AccountStatement(@NotNull ProductStatement productStatement, @NotNull Long accountId, String recurrence, String sequencePrefix) {
        this.productStatement = productStatement;
        this.recurrence = recurrence;
        this.sequencePrefix = sequencePrefix;
    }

    public static AccountStatement create(@NotNull ProductStatement productStatement, @NotNull AccountStatementData statementData) {
        AccountStatement statement = new AccountStatement(productStatement, statementData.getAccountId(),
                Optional.ofNullable(statementData.getRecurrence()).orElse(productStatement.getRecurrence()),
                statementData.getSequencePrefix());
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        statement.setNextDate(statement.calcNextDate(transactionDate));
        statement.setSequenceNo(statement.calcNextSequence(transactionDate));
        return statement;
    }

    public void update(@NotNull AccountStatementData statementData, @NotNull Map<String, Object> changes) {
        String recurrence = statementData.getRecurrence();
        if (recurrence != null && !Objects.equals(this.recurrence, recurrence)) {
            setRecurrence(recurrence);
            changes.put(PARAM_RECURRENCE, recurrence);
        }
        String sequencePrefix = statementData.getSequencePrefix();
        if (sequencePrefix != null && !Objects.equals(this.sequencePrefix, sequencePrefix)) {
            setRecurrence(sequencePrefix);
            changes.put(PARAM_SEQUENCE_PREFIX, sequencePrefix);
        }
    }

    public void initNext(LocalDate transactionDate) {
        setLastDate(transactionDate);
        setNextDate(calcNextDate(transactionDate));
        setSequenceNo(calcNextSequence(transactionDate));
    }

    public LocalDate calcNextDate(LocalDate transactionDate) {
        if (recurrence == null) {
            return null;
        }
        LocalDate seedDate = lastDate == null ? transactionDate : lastDate;
        return CalendarUtils.getNextRecurringDate(getRecurrence(), seedDate, transactionDate);
    }

    public Integer calcNextSequence(LocalDate transactionDate) {
        if (sequenceNo == null || lastDate == null) {
            return 1;
        }
        if (lastDate.getYear() < transactionDate.getYear()) {
            return 1;
        }
        return sequenceNo + 1;
    }
}
