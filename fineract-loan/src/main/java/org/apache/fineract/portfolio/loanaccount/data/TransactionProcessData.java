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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;

@Data
@Accessors(chain = true)
@RequiredArgsConstructor
public class TransactionProcessData {

    private final Long transactionId;
    private final LoanTransactionType transactionType;
    private Money transactionAmount;
    private BigDecimal processedAmount;

    public boolean isAccrual() {
        return transactionType.isAccrual();
    }

    public boolean isAccrualAdjustment() {
        return transactionType.isAccrualAdjustment();
    }

    public boolean isWaiveInterest() {
        return transactionType.isWaiveInterest();
    }

    public boolean isWaiveCharge() {
        return transactionType.isWaiveCharges();
    }

    public BigDecimal getUnprocessedAmount() {
        return MathUtil.subtract(MathUtil.toBigDecimal(transactionAmount), processedAmount);
    }
}
