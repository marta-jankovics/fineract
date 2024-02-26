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
package org.apache.fineract.currentaccount.enumeration.product;

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.BALANCE_CALCULATION;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CLOSE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CREATE;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;

@Getter
public enum BalanceCalculationType {

    LAZY("balanceCalculationType.lazy", "Lazy balance calculation for debit/credit transactions"), //
    STRICT_DEBIT("balanceCalculationType.strict_debit", "Strict balance calculation for debit transactions"), //
    STRICT("balanceCalculationType.strict", "Strict balance calculation for debit/credit transactions"); //

    private final String code;
    private final String description;

    BalanceCalculationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), getDescription());
    }

    public boolean isLazy() {
        return this == LAZY;
    }

    public boolean isStrictDebit() {
        return this == STRICT_DEBIT;
    }

    public boolean isStrict() {
        return this == STRICT;
    }

    public boolean hasDelay(@NotNull CurrentAccountAction action) {
        return switch (this) {
            case LAZY -> action != CREATE && action != CLOSE;
            case STRICT_DEBIT -> isPersist(action);
            case STRICT -> false;
        };
    }

    public boolean isPersist(@NotNull CurrentTransactionType transactionType) {
        return switch (this) {
            case LAZY -> false;
            case STRICT_DEBIT -> transactionType.isDebit();
            case STRICT -> true;
        };
    }

    public boolean isPersist(@NotNull CurrentAccountAction action) {
        CurrentTransactionType transactionType = action.getTransactionType();
        if (transactionType != null) {
            return isPersist(transactionType);
        }
        return action == CREATE || action == CLOSE || action == BALANCE_CALCULATION;
    }
}
