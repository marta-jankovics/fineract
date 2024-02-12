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
package org.apache.fineract.currentaccount.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account_balance")
public class CurrentAccountBalance extends AbstractAuditableWithUTCDateTimeCustom<Long> implements ICurrentAccountBalance {

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "account_balance", nullable = false, precision = 6)
    private BigDecimal accountBalance;

    @Column(name = "hold_amount", precision = 6)
    private BigDecimal holdAmount;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Version
    private Long version;

    public CurrentAccountBalance(String accountId, BigDecimal accountBalance, BigDecimal holdAmount, String transactionId) {
        this.accountId = accountId;
        this.accountBalance = accountBalance;
        this.holdAmount = holdAmount;
        this.transactionId = transactionId;
    }

    @Override
    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    @Override
    public void setHoldAmount(BigDecimal holdAmount) {
        this.holdAmount = holdAmount;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
