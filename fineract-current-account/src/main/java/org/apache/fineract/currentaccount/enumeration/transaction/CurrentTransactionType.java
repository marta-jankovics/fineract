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

import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.TransactionEntryType;

/**
 * An enumeration of different transactions that can occur on a
 * {@link org.apache.fineract.currentaccount.domain.account.CurrentAccount}.
 */
@Getter
public enum CurrentTransactionType {

    DEPOSIT(1, "Deposit transaction", TransactionEntryType.CREDIT), //
    WITHDRAWAL(2, "Withdrawal transaction", TransactionEntryType.DEBIT), //
    AMOUNT_HOLD(3, "Hold amount transaction", TransactionEntryType.DEBIT), //
    AMOUNT_RELEASE(4, "Release amount transaction", TransactionEntryType.CREDIT); //

    private static final CurrentTransactionType[] VALUES = values();

    private final long id;
    private final String value;
    private final TransactionEntryType entryType;

    CurrentTransactionType(final long id, final String value, TransactionEntryType entryType) {
        this.id = id;
        this.value = value;
        this.entryType = entryType;
    }

    public boolean isCreditEntryType() {
        return entryType != null && entryType.isCredit();
    }

    public boolean isDebitEntryType() {
        return entryType != null && entryType.isDebit();
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
        return new EnumOptionData(getId(), name(), getValue());
    }
}
