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
    SUBMITTED_AND_PENDING_APPROVAL(100, "currentAccountStatusType.submitted.and.pending.approval"), //
    APPROVED(200, "currentAccountStatusType.approved"), //
    ACTIVE(300, "currentAccountStatusType.active"), //
    TRANSFER_IN_PROGRESS(303, "currentAccountStatusType.transfer.in.progress"), //
    TRANSFER_ON_HOLD(304, "currentAccountStatusType.transfer.on.hold"), //
    WITHDRAWN_BY_APPLICANT(400, "currentAccountStatusType.withdrawn.by.applicant"), //
    REJECTED(500, "currentAccountStatusType.rejected"), //
    CLOSED(600, "currentAccountStatusType.closed"), //
    PRE_MATURE_CLOSURE(700, "currentAccountStatusType.pre.mature.closure"), //
    MATURED(800, "currentAccountStatusType.matured"); //

    private final Integer value;
    private final String code;

    CurrentAccountStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public static CurrentAccountStatus fromInt(final Integer type) {
        return switch (type) {
            case 100 -> CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL;
            case 200 -> CurrentAccountStatus.APPROVED;
            case 300 -> CurrentAccountStatus.ACTIVE;
            case 303 -> CurrentAccountStatus.TRANSFER_IN_PROGRESS;
            case 304 -> CurrentAccountStatus.TRANSFER_ON_HOLD;
            case 400 -> CurrentAccountStatus.WITHDRAWN_BY_APPLICANT;
            case 500 -> CurrentAccountStatus.REJECTED;
            case 600 -> CurrentAccountStatus.CLOSED;
            case 700 -> CurrentAccountStatus.PRE_MATURE_CLOSURE;
            case 800 -> CurrentAccountStatus.MATURED;
            default -> CurrentAccountStatus.INVALID;
        };
    }

    public boolean hasStateOf(final CurrentAccountStatus state) {
        return this == state;
    }

    public boolean isSubmittedAndPendingApproval() {
        return this == CurrentAccountStatus.SUBMITTED_AND_PENDING_APPROVAL;
    }

    public boolean isApproved() {
        return this == CurrentAccountStatus.APPROVED;
    }

    public boolean isRejected() {
        return this == CurrentAccountStatus.REJECTED;
    }

    public boolean isApplicationWithdrawnByApplicant() {
        return this == CurrentAccountStatus.WITHDRAWN_BY_APPLICANT;
    }

    public boolean isActive() {
        return this == CurrentAccountStatus.ACTIVE;
    }

    public boolean isActiveOrAwaitingApprovalOrDisbursal() {
        return isApproved() || isSubmittedAndPendingApproval() || isActive();
    }

    public boolean isClosed() {
        return this == CurrentAccountStatus.CLOSED || isRejected() || isApplicationWithdrawnByApplicant();
    }

    public boolean isTransferInProgress() {
        return this == CurrentAccountStatus.TRANSFER_IN_PROGRESS;
    }

    public boolean isTransferOnHold() {
        return this == CurrentAccountStatus.TRANSFER_ON_HOLD;
    }

    public boolean isUnderTransfer() {
        return isTransferInProgress() || isTransferOnHold();
    }

    public boolean isMatured() {
        return this == CurrentAccountStatus.MATURED;
    }

    public boolean isPreMatureClosure() {
        return this == CurrentAccountStatus.PRE_MATURE_CLOSURE;
    }

    public Object toEnumOptionData() {
        return new EnumOptionData((long) getValue(), getCode(), name());
    }
}
