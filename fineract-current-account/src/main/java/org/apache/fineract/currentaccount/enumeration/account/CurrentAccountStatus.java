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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.ACTIVATE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.BALANCE_CALCULATION;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CANCEL;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CLOSE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.UPDATE;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

/**
 * Enum representation of {@link CurrentAccount} status states.
 */
@Getter
public enum CurrentAccountStatus {

    SUBMITTED("currentAccountStatus.submitted", "Current account is submitted"), //
    ACTIVE("currentAccountStatus.active", "Current account is active"), //
    CANCELLED("currentAccountStatus.cancelled", "Current account is cancelled"), //
    CLOSED("currentAccountStatus.closed", "Current account is closed"); //

    private static CurrentAccountStatus[] VALUES = values();

    private final String code;
    private final String description;

    CurrentAccountStatus(final String code, final String description) {
        this.code = code;
        this.description = description;
    }

    public boolean isSubmitted() {
        return this == CurrentAccountStatus.SUBMITTED;
    }

    public boolean isCancelled() {
        return this == CurrentAccountStatus.CANCELLED;
    }

    public boolean isActive() {
        return this == CurrentAccountStatus.ACTIVE;
    }

    public boolean isClosed() {
        return this == CurrentAccountStatus.CLOSED || isCancelled();
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), getDescription());
    }

    public boolean isEnabled(CurrentAccountAction action) {
        if (action == null) {
            return false;
        }
        return switch (this) {
            case SUBMITTED -> action == UPDATE || action == ACTIVATE || action == CANCEL;
            case ACTIVE -> action == UPDATE || action == CLOSE || action == BALANCE_CALCULATION || action.isTransaction();
            case CANCELLED, CLOSED -> false;
        };
    }

    public static List<CurrentAccountStatus> getEnabledStatusList(CurrentAccountAction action) {
        return Arrays.stream(VALUES).filter(e -> e.isEnabled(action)).toList();
    }

    public void checkEnabled(CurrentAccountAction action) {
        if (!isEnabled(action)) {
            throw new GeneralPlatformDomainRuleException("error.msg.current.action.not.allowed",
                    "Current Account action " + action + " is not allowed on status " + this);
        }
    }
}
