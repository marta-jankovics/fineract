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
package org.apache.fineract.currentaccount.enums.account;

import lombok.Getter;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

/**
 * Enum representation of {@link CurrentAccount} status states.
 */
@Getter
public enum CurrentAccountStatus {

    INVALID(0, "currentAccountStatusType.invalid"), //
    SUBMITTED(100, "currentAccountStatusType.submitted"), //
    ACTIVE(300, "currentAccountStatusType.active"), //
    CANCELLED(400, "currentAccountStatusType.cancelled"), //
    CLOSED(600, "currentAccountStatusType.closed"); //

    private final Integer value;
    private final String code;

    CurrentAccountStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static CurrentAccountStatus fromInt(final Integer type) {
        return switch (type) {
            case 100 -> CurrentAccountStatus.SUBMITTED;
            case 300 -> CurrentAccountStatus.ACTIVE;
            case 600 -> CurrentAccountStatus.CLOSED;
            default -> CurrentAccountStatus.INVALID;
        };
    }

    public boolean hasStateOf(final CurrentAccountStatus state) {
        return this == state;
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

    public boolean isSubmittedOrActive() {
        return isSubmitted() || isActive();
    }

    public boolean isClosed() {
        return this == CurrentAccountStatus.CLOSED || isCancelled();
    }

    public Object toEnumOptionData() {
        return new EnumOptionData((long) getValue(), getCode(), name());
    }
}
