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
package org.apache.fineract.currentaccount.enums.transaction;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.TransactionEntryType;

/**
 * An enumeration of different transactions that can occur on a
 * {@link org.apache.fineract.currentaccount.domain.account.CurrentAccount}.
 */
public enum CurrentTransactionType {

    INVALID(0, "currentAccountTransactionType.invalid"), //
    DEPOSIT(1, "currentAccountTransactionType.deposit", TransactionEntryType.CREDIT), //
    WITHDRAWAL(2, "currentAccountTransactionType.withdrawal", TransactionEntryType.DEBIT), //
    AMOUNT_HOLD(3, "currentAccountTransactionType.onHold", TransactionEntryType.DEBIT), //
    AMOUNT_RELEASE(4, "currentAccountTransactionType.release", TransactionEntryType.CREDIT); //

    public static final CurrentTransactionType[] VALUES = values();

    private static final Map<Integer, CurrentTransactionType> BY_ID = Arrays.stream(VALUES)
            .collect(Collectors.toMap(CurrentTransactionType::getValue, v -> v));

    private final int value;
    private final String code;
    private final TransactionEntryType entryType;

    CurrentTransactionType(final Integer value, final String code, TransactionEntryType entryType) {
        this.value = value;
        this.code = code;
        this.entryType = entryType;
    }

    CurrentTransactionType(final Integer value, final String code) {
        this(value, code, null);
    }

    public static CurrentTransactionType fromInt(final Integer value) {
        CurrentTransactionType transactionType = BY_ID.get(value);
        return transactionType == null ? INVALID : transactionType;
    }

    @NotNull
    public static List<CurrentTransactionType> getFiltered(Predicate<CurrentTransactionType> filter) {
        return Arrays.stream(VALUES).filter(filter).toList();
    }

    public int getId() {
        return this.value;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public TransactionEntryType getEntryType() {
        return entryType;
    }

    public boolean isCreditEntryType() {
        return entryType != null && entryType.isCredit();
    }

    public boolean isDebitEntryType() {
        return entryType != null && entryType.isDebit();
    }

    public boolean isValid() {
        return this != INVALID;
    }

    public boolean isDeposit() {
        return this == DEPOSIT;
    }

    public boolean isWithdrawal() {
        return this == WITHDRAWAL;
    }

    public boolean isAmountOnHold() {
        return this == AMOUNT_HOLD;
    }

    public boolean isAmountRelease() {
        return this == AMOUNT_RELEASE;
    }

    public boolean isCredit() {
        // AMOUNT_RELEASE is not credit, because the account balance is not changed
        return isCreditEntryType() && !isAmountRelease();
    }

    public boolean isDebit() {
        // AMOUNT_HOLD is not debit, because the account balance is not changed
        return isDebitEntryType() && !isAmountOnHold();
    }

    public EnumOptionData toEnumOptionData() {
        return new EnumOptionData(getValue().longValue(), getCode(), name());
    }
}
