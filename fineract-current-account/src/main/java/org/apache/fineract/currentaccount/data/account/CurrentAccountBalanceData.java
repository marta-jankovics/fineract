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
package org.apache.fineract.currentaccount.data.account;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.service.MathUtil;

@Data
public class CurrentAccountBalanceData implements Serializable {

    // Current account balance data
    private final Long id;
    private final String accountId;
    private BigDecimal accountBalance;
    private BigDecimal holdAmount;
    private OffsetDateTime calculatedTill;
    private String calculatedTillTransactionId;
    private boolean changed;

    public CurrentAccountBalanceData(Long id, String accountId, BigDecimal accountBalance, BigDecimal holdAmount,
            OffsetDateTime calculatedTill, String calculatedTillTransactionId, boolean changed) {
        this.id = id;
        this.accountId = accountId;
        this.accountBalance = accountBalance;
        this.holdAmount = holdAmount;
        this.calculatedTill = calculatedTill;
        this.calculatedTillTransactionId = calculatedTillTransactionId;
        this.changed = changed;
    }

    public CurrentAccountBalanceData(Long id, String accountId, BigDecimal accountBalance, BigDecimal holdAmount,
            OffsetDateTime calculatedTill, String calculatedTillTransactionId) {
        this(id, accountId, accountBalance, holdAmount, calculatedTill, calculatedTillTransactionId, false);
    }

    public BigDecimal getAvailableBalance() {
        return MathUtil.subtract(accountBalance, holdAmount);
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
