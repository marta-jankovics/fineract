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

import static org.apache.fineract.statement.data.StatementParser.PARAM_RECURRENCE;
import static org.apache.fineract.statement.data.StatementParser.PARAM_SEQUENCE_PREFIX;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
import org.apache.fineract.statement.data.dto.AccountStatementData;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "m_account_statement", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "product_statement_id", "account_id" }, name = "uk_account_statement") })
public class AccountStatement extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Version
    int version;

    @ManyToOne
    @JoinColumn(name = "product_statement_id", nullable = false)
    private ProductStatement productStatement;

    @Column(name = "account_id", nullable = false, length = 21)
    private String accountId;

    @Setter(AccessLevel.PROTECTED)
    @Column(name = "recurrence", nullable = true, length = 100)
    private String recurrence;

    @Setter(AccessLevel.PROTECTED)
    @Column(name = "sequence_prefix", nullable = true, length = 10)
    private String sequencePrefix;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_status", nullable = false, length = 100)
    private StatementStatus statementStatus;

    @Column(name = "sequence_no", precision = 4, nullable = false)
    private Integer sequenceNo;

    @Column(name = "statement_date", nullable = true)
    private LocalDate statementDate; // date for which the last statement was generated (not the date of generation)

    @Setter()
    @Column(name = "statement_balance", nullable = true)
    private BigDecimal statementBalance;

    @ManyToOne
    @JoinColumn(name = "statement_result_id", nullable = true)
    private AccountStatementResult statementResult;

    @Column(name = "next_statement_date", nullable = true)
    private LocalDate nextStatementDate;

    protected AccountStatement(@NotNull ProductStatement productStatement, @NotNull String accountId, String recurrence,
            String sequencePrefix) {
        this.productStatement = productStatement;
        this.accountId = accountId;
        this.recurrence = recurrence;
        this.sequencePrefix = sequencePrefix;
        this.statementStatus = StatementStatus.INACTIVE;
        this.sequenceNo = 0;
    }

    public static AccountStatement create(@NotNull ProductStatement productStatement, @NotNull AccountStatementData statementData) {
        return new AccountStatement(productStatement, statementData.getAccountId(),
                Optional.ofNullable(statementData.getRecurrence()).orElse(productStatement.getRecurrence()),
                Optional.ofNullable(statementData.getSequencePrefix()).orElse(productStatement.getSequencePrefix()));
    }

    public static AccountStatement create(@NotNull ProductStatement productStatement, @NotNull String accountId) {
        return new AccountStatement(productStatement, accountId, productStatement.getRecurrence(), productStatement.getSequencePrefix());
    }

    public boolean update(@NotNull AccountStatementData statementData, @NotNull Map<String, Object> changes) {
        boolean changed = false;
        String recurrence = statementData.getRecurrence();
        if (recurrence != null && !Objects.equals(this.recurrence, recurrence)) {
            setRecurrence(recurrence);
            changes.put(PARAM_RECURRENCE, recurrence);
            changed = true;
        }
        String sequencePrefix = statementData.getSequencePrefix();
        if (sequencePrefix != null && !Objects.equals(this.sequencePrefix, sequencePrefix)) {
            setRecurrence(sequencePrefix);
            changes.put(PARAM_SEQUENCE_PREFIX, sequencePrefix);
            changed = true;
        }
        return changed;
    }

    public boolean inherit(@NotNull ProductStatement productStatement) {
        boolean changed = false;
        String recurrence = productStatement.getRecurrence();
        if (recurrence != null && !Objects.equals(this.recurrence, recurrence)) {
            setRecurrence(recurrence);
            changed = true;
        }
        String sequencePrefix = productStatement.getSequencePrefix();
        if (sequencePrefix != null && !Objects.equals(this.sequencePrefix, sequencePrefix)) {
            setSequencePrefix(sequencePrefix);
            changed = true;
        }
        return changed;
    }

    public String getStatementCode() {
        return productStatement.getStatementCode();
    }

    public void activate() {
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        nextStatementDate = calcNextDate(transactionDate);
        sequenceNo = calcNextSequence();
        statementStatus = statementStatus.activate();
    }

    public void inactivate() {
        nextStatementDate = null;
        statementStatus = statementStatus.inactivate();
    }

    public boolean canGenerate() {
        return statementStatus.canGenerate();
    }

    public void generate() {
        statementStatus.generate();
    }

    public void generated(AccountStatementResult result) {
        statementResult = result;
        statementDate = nextStatementDate;
        nextStatementDate = calcNextDate(nextStatementDate);
        sequenceNo = calcNextSequence();
        statementStatus = statementStatus.generate();
    }

    public LocalDate calcNextDate(@NotNull LocalDate startDate) {
        if (recurrence == null) {
            return null;
        }
        // nextDate might be calculated earlier than today, but it is ok, the next generation job will generate again
        LocalDate seedDate = statementDate == null ? startDate : statementDate;
        return CalendarUtils.getNextRecurringDate(getRecurrence(), seedDate, startDate, false);
    }

    public Integer calcNextSequence() {
        if (sequenceNo == null || statementDate == null) {
            return 1;
        }
        if (nextStatementDate != null && statementDate.getYear() < nextStatementDate.getYear()) {
            return 1;
        }
        return sequenceNo + 1;
    }
}
