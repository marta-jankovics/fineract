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

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.service.MathUtil;

public interface ICurrentAccountBalance {

    Long getId();

    String getAccountId();

    BigDecimal getAccountBalance();

    void setAccountBalance(BigDecimal accountBalance);

    BigDecimal getHoldAmount();

    void setHoldAmount(BigDecimal holdAmount);

    String getTransactionId();

    void setTransactionId(String transactionId);

    default boolean isChanged() {
        return false;
    }

    default boolean applyTransaction(@NotNull CurrentTransaction transaction) {
        CurrentTransactionType transactionType = transaction.getTransactionType();
        BigDecimal amount = transaction.getAmount();
        BigDecimal accountBalance = getAccountBalance();
        BigDecimal holdAmount = getHoldAmount();
        boolean changed = false;
        if (transactionType.isMonetaryCredit()) {
            setAccountBalance(MathUtil.add(accountBalance, amount));
            changed = true;
        } else if (transactionType.isMonetaryDebit()) {
            setAccountBalance(MathUtil.subtract(accountBalance, amount));
            changed = true;
        } else if (transactionType.isAmountOnHold()) {
            setHoldAmount(MathUtil.add(holdAmount, amount));
            changed = true;
        } else if (transactionType.isAmountRelease()) {
            setHoldAmount(MathUtil.subtract(holdAmount, amount));
            changed = true;
        }
        if (changed) {
            setTransactionId(transaction.getId());
        }
        return changed;
    }
}
