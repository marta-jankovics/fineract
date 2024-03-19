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

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

/**
 * Enum representation of {@link CurrentAccount} status states.
 */
@Getter
public enum CurrentAccountStatus {

    SUBMITTED("currentAccountStatus.submitted", "Current account is submitted") {

        @Override
        public CurrentAccountStatus getNextStatus(@NotNull CurrentAccountAction action) {
            return switch (action) {
                case UPDATE -> this;
                case ACTIVATE -> ACTIVE;
                case CANCEL -> CANCELLED;
                default -> null;
            };
        }
    }, //
    ACTIVE("currentAccountStatus.active", "Current account is active") {

        @Override
        public CurrentAccountStatus getNextStatus(@NotNull CurrentAccountAction action) {
            if (action.isTransaction()) {
                return this;
            }
            return switch (action) {
                case UPDATE, BALANCE_CALCULATION, STATEMENT_GENERATION, METADATA_GENERATION -> this;
                case CLOSE -> CLOSED;
                default -> null;
            };
        }
    }, //
    CANCELLED("currentAccountStatus.cancelled", "Current account is cancelled"), //
    CLOSED("currentAccountStatus.closed", "Current account is closed") {

        @Override
        public CurrentAccountStatus getNextStatus(@NotNull CurrentAccountAction action) {
            return switch (action) {
                case METADATA_GENERATION -> this;
                default -> null;
            };
        }
    }; //

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
        return this == CurrentAccountStatus.CLOSED;
    }

    public boolean isOpen() {
        return isSubmitted() || isActive();
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), getDescription());
    }

    public static List<CurrentAccountStatus> getFiltered(Predicate<? super CurrentAccountStatus> predicate) {
        return Arrays.stream(VALUES).filter(predicate).toList();
    }

    // ----- Lifecycle -----
    public boolean isEnabled(CurrentAccountAction action) {
        return action != null && getNextStatus(action) != null;
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

    public CurrentAccountStatus getNextStatus(@NotNull CurrentAccountAction action) {
        return null;
    }
}
