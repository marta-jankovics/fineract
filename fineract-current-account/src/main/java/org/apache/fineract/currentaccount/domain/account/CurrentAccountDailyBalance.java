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
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.service.MathUtil;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account_daily_balance")
public class CurrentAccountDailyBalance extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "account_balance", nullable = false, precision = 6)
    private BigDecimal accountBalance;

    @Column(name = "hold_amount", precision = 6)
    private BigDecimal holdAmount;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Version
    private Long version;

    public CurrentAccountDailyBalance(String accountId, BigDecimal accountBalance, BigDecimal holdAmount, LocalDate balanceDate) {
        this.accountId = accountId;
        this.accountBalance = accountBalance;
        this.holdAmount = holdAmount;
        this.balanceDate = balanceDate;
    }

    public void applyTransaction(@NotNull CurrentTransaction transaction) {
        CurrentTransactionType transactionType = transaction.getTransactionType();
        BigDecimal amount = transaction.getAmount();
        if (transactionType.isMonetaryCredit()) {
            accountBalance = MathUtil.add(accountBalance, amount);
        } else if (transactionType.isMonetaryDebit()) {
            accountBalance = MathUtil.subtract(accountBalance, amount);
        } else if (transactionType.isAmountOnHold()) {
            holdAmount = MathUtil.add(holdAmount, amount);
        } else if (transactionType.isAmountRelease()) {
            holdAmount = MathUtil.subtract(holdAmount, amount);
        }
    }
}
