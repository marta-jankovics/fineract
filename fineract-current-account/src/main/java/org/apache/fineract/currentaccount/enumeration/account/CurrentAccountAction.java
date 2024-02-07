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
package org.apache.fineract.currentaccount.enumeration.account;

import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.AMOUNT_HOLD;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.AMOUNT_RELEASE;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.DEPOSIT;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.WITHDRAWAL;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_ACTIVATE;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_CANCEL;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_CLOSE;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_CREATE;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_DEPOSIT;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_FORCE_WITHDRAWAL;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_HOLDAMOUNT;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_RELEASE;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_UPDATE;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_WITHDRAWAL;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;

@Getter
@AllArgsConstructor
public enum CurrentAccountAction {

    CREATE(ACTION_CREATE), //
    UPDATE(ACTION_UPDATE), //
    ACTIVATE(ACTION_ACTIVATE), //
    CANCEL(ACTION_CANCEL), //
    CLOSE(ACTION_CLOSE), //
    BALANCE_CALCULATION("CALCULATE_BALANCE"), //
    TRANSACTION_DEPOSIT(ACTION_DEPOSIT), //
    TRANSACTION_WITHDRAWAL(ACTION_WITHDRAWAL), //
    TRANSACTION_FORCE_WITHDRAWAL(ACTION_FORCE_WITHDRAWAL), //
    TRANSACTION_AMOUNT_HOLD(ACTION_HOLDAMOUNT), //
    TRANSACTION_AMOUNT_RELEASE(ACTION_RELEASE), //
    ;

    private static CurrentAccountAction[] VALUES = values();
    private static final Map<String, CurrentAccountAction> BY_ACTION_NAME = Arrays.stream(VALUES)
            .collect(Collectors.toMap(CurrentAccountAction::getActionName, v -> v));

    private final String actionName;

    public static CurrentAccountAction forActionName(String actionName) {
        return BY_ACTION_NAME.get(actionName);
    }

    public boolean isTransaction() {
        return ordinal() >= TRANSACTION_DEPOSIT.ordinal();
    }

    public CurrentTransactionType getTransactionType() {
        return switch (this) {
            case TRANSACTION_DEPOSIT -> DEPOSIT;
            case TRANSACTION_WITHDRAWAL, TRANSACTION_FORCE_WITHDRAWAL -> WITHDRAWAL;
            case TRANSACTION_AMOUNT_HOLD -> AMOUNT_HOLD;
            case TRANSACTION_AMOUNT_RELEASE -> AMOUNT_RELEASE;
            default -> null;
        };
    }

    public boolean isForce() {
        return this == TRANSACTION_FORCE_WITHDRAWAL;
    }
}
