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

import lombok.Getter;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;

/**
 * Enum representation of {@link CurrentAccount} status states.
 */
@Getter
public enum CurrentAccountStatus {

    SUBMITTED("currentAccountStatus.submitted", "Current account is submitted"), //
    ACTIVE("currentAccountStatus.active", "Current account is active"), //
    CANCELLED("currentAccountStatus.cancelled", "Current account is cancelled"), //
    CLOSED("currentAccountStatus.closed", "Current account is closed"); //

    private final String code;
    private final String description;

    CurrentAccountStatus(final String code, final String description) {
        this.code = code;
        this.description = description;
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

    public boolean isClosed() {
        return this == CurrentAccountStatus.CLOSED || isCancelled();
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), getDescription());
    }
}
