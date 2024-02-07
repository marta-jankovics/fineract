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
package org.apache.fineract.currentaccount.enumeration.transaction;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.portfolio.TransactionEntryType;

/**
 * An enumeration of different transactions that can occur on a
 * {@link org.apache.fineract.currentaccount.domain.account.CurrentAccount}.
 */
@Getter
public enum CurrentTransactionType {

    DEPOSIT("currentTransactionType.deposit", "Deposit transaction", TransactionEntryType.CREDIT, true), //
    WITHDRAWAL("currentTransactionType.withdrawal", "Withdrawal transaction", TransactionEntryType.DEBIT, true), //
    WITHDRAWAL_FEE("currentTransactionType.withdrawal_fee", "Withdrawal Fee transaction", TransactionEntryType.DEBIT, true), //
    AMOUNT_HOLD("currentTransactionType.amount_hold", "Hold amount transaction", TransactionEntryType.DEBIT, false), //
    AMOUNT_RELEASE("currentTransactionType.amount_release", "Release amount transaction", TransactionEntryType.CREDIT, false); //

    private static final CurrentTransactionType[] VALUES = values();

    @NotNull
    private final String code;
    @NotNull
    private final String description;
    private final TransactionEntryType entryType;
    private final boolean monetary;

    CurrentTransactionType(@NotNull String code, @NotNull String description, TransactionEntryType entryType, boolean monetary) {
        this.code = code;
        this.description = description;
        this.entryType = entryType;
        this.monetary = monetary;
    }

    public boolean isCredit() {
        return entryType != null && entryType.isCredit();
    }

    public boolean isDebit() {
        return entryType != null && entryType.isDebit();
    }

    public boolean isDeposit() {
        return this == DEPOSIT;
    }

    public boolean isWithdrawal() {
        return this == WITHDRAWAL;
    }

    public boolean isWithdrawalFee() {
        return this == WITHDRAWAL_FEE;
    }

    public boolean isAmountOnHold() {
        return this == AMOUNT_HOLD;
    }

    public boolean isAmountRelease() {
        return this == AMOUNT_RELEASE;
    }

    public boolean isMonetaryCredit() {
        return isCredit() && isMonetary();
    }

    public boolean isMonetaryDebit() {
        return isDebit() && isMonetary();
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), getDescription());
    }

    public static List<CurrentTransactionType> getFiltered(Predicate<? super CurrentTransactionType> predicate) {
        return Arrays.stream(VALUES).filter(predicate).toList();
    }
}
