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
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountBalance;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;

@Data
@AllArgsConstructor
public class CurrentAccountBalanceData implements Serializable, ICurrentAccountBalance {

    private final Long id;
    private final String accountId;
    private BigDecimal accountBalance;
    private BigDecimal holdAmount;
    private String transactionId;
    private OffsetDateTime calculatedTill;
    private boolean changed;

    public CurrentAccountBalanceData(Long id, String accountId, BigDecimal accountBalance, BigDecimal holdAmount, String transactionId,
            OffsetDateTime calculatedTill) {
        this(id, accountId, accountBalance, holdAmount, transactionId, calculatedTill, false);
    }

    @Override
    public boolean applyTransaction(@NotNull CurrentTransaction transaction) {
        boolean changed = ICurrentAccountBalance.super.applyTransaction(transaction);
        if (changed) {
            transactionId = transaction.getId();
            calculatedTill = transaction.getCreatedDateTime();
            this.changed = true;
        }
        return changed;
    }
}
