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
package org.apache.fineract.currentaccount.domain.accounting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.account.PortfolioAccountType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "acc_account_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account_id", "account_type" }, name = "acc_account_history_account_id_type_key") })
public class GLAccountingHistory extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private PortfolioAccountType accountType;

    @Column(name = "account_balance", precision = 6, nullable = false)
    private BigDecimal accountBalance;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Version
    private Long version;

    public GLAccountingHistory(String accountId, PortfolioAccountType accountType, BigDecimal accountBalance, String transactionId) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.accountBalance = accountBalance;
        this.transactionId = transactionId;
    }

    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
