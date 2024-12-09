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
package org.apache.fineract.portfolio.loanaccount.domain;

import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.ACTIVE;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.APPROVED;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.CLOSED_OBLIGATIONS_MET;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.CLOSED_RESCHEDULE_OUTSTANDING_AMOUNT;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.CLOSED_WRITTEN_OFF;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.OVERPAID;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.REJECTED;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.SUBMITTED_AND_PENDING_APPROVAL;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.TRANSFER_IN_PROGRESS;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.TRANSFER_ON_HOLD;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanStatus.WITHDRAWN_BY_CLIENT;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.event.business.domain.journalentry.LoanSameStatusBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanStatusChangedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// TODO: introduce tests for the state machine
@Component
@RequiredArgsConstructor
public class DefaultLoanLifecycleStateMachine implements LoanLifecycleStateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLoanLifecycleStateMachine.class);

    private static final List<LoanStatus> ALLOWED_LOAN_STATUSES = List.of(LoanStatus.values());
    private final BusinessEventNotifierService businessEventNotifierService;

    @Override
    public LoanStatus dryTransition(final LoanEvent loanEvent, final Loan loan) {
        LoanStatus newStatus = getNextStatus(loanEvent, loan);
        return newStatus != null ? newStatus : loan.getStatus();
    }

    @Override
    public void transition(final LoanEvent loanEvent, final Loan loan) {
        loan.updateLoanSummaryDerivedFields();
        boolean loanCreation = isLoanCreation(loanEvent);

        LoanStatus oldStatus = loan.getStatus();
        LoanStatus newStatus = getNextStatus(loanEvent, loan);
        if (oldStatus == newStatus) {
            if (!loanCreation) {
                businessEventNotifierService.notifyPostBusinessEvent(new LoanSameStatusBusinessEvent(loan, loanEvent));
            }
        } else if (newStatus != null) {
            Integer newPlainStatus = newStatus.getValue();
            loan.setLoanStatus(newPlainStatus);

            if (!loanCreation) {
                // in case of Loan creation, a LoanCreatedBusinessEvent is also raised, no need to send a status change
                businessEventNotifierService.notifyPostBusinessEvent(new LoanStatusChangedBusinessEvent(loan, oldStatus));
            }

            // set mandatory field states based on new status after the transition
            LOG.debug("Transitioning loan {} status from {} to {}", loan.getId(), oldStatus, newStatus);
            switch (newStatus) {
                case SUBMITTED_AND_PENDING_APPROVAL -> {
                    loan.setApprovedOnDate(null);
                    loan.setApprovedBy(null);
                }
                case APPROVED -> {
                    loan.setDisbursedBy(null);
                    loan.setActualDisbursementDate(null);
                }
                case ACTIVE -> {
                    loan.setClosedBy(null);
                    loan.setClosedOnDate(null);
                    loan.setOverpaidOnDate(null);
                }
                default -> { // no fields need to get cleared
                }
            }
        }
    }

    private boolean isLoanCreation(LoanEvent loanEvent) {
        return LoanEvent.LOAN_CREATED == loanEvent;
    }

    private LoanStatus getNextStatus(LoanEvent loanEvent, Loan loan) {
        Integer plainFrom = loan.getPlainStatus();
        if (loanEvent.equals(LoanEvent.LOAN_CREATED) && plainFrom == null) {
            return submittedTransition();
        }

        LoanStatus from = loan.getStatus();
        return switch (loanEvent) {
            case LOAN_REJECTED -> from.hasStateOf(SUBMITTED_AND_PENDING_APPROVAL) ? rejectedTransition() : null;
            case LOAN_APPROVED -> from.hasStateOf(SUBMITTED_AND_PENDING_APPROVAL) ? approvedTransition() : null;
            case LOAN_WITHDRAWN -> anyOfAllowedWhenComingFrom(from, SUBMITTED_AND_PENDING_APPROVAL) ? withdrawnByClientTransition() : null;
            case LOAN_DISBURSED -> balanceTransition(loan, ACTIVE, APPROVED, CLOSED_OBLIGATIONS_MET, OVERPAID);
            case LOAN_APPROVAL_UNDO -> from.hasStateOf(APPROVED) ? submittedTransition() : null;
            case LOAN_DISBURSAL_UNDO -> anyOfAllowedWhenComingFrom(from, ACTIVE) ? approvedTransition() : null;
            case LOAN_DISBURSAL_UNDO_LAST -> balanceTransition(loan, ACTIVE, ACTIVE, CLOSED_OBLIGATIONS_MET, OVERPAID);
            case LOAN_CHARGE_PAYMENT, LOAN_REPAYMENT_OR_WAIVER -> balanceTransition(loan, ACTIVE, ACTIVE, CLOSED_OBLIGATIONS_MET, OVERPAID);
            case REPAID_IN_FULL ->
                anyOfAllowedWhenComingFrom(from, ACTIVE, CLOSED_OBLIGATIONS_MET, OVERPAID) ? closeObligationsMetTransition() : null;
            case WRITE_OFF_OUTSTANDING -> anyOfAllowedWhenComingFrom(from, ACTIVE) ? closedWrittenOffTransition() : null;
            case LOAN_RESCHEDULE -> anyOfAllowedWhenComingFrom(from, ACTIVE) ? closedRescheduleOutstandingAmountTransition() : null;
            case LOAN_OVERPAYMENT ->
                anyOfAllowedWhenComingFrom(from, CLOSED_OBLIGATIONS_MET, OVERPAID, ACTIVE) ? overpaidTransition() : null;
            case LOAN_ADJUST_TRANSACTION -> balanceTransition(loan, ACTIVE, ACTIVE, CLOSED_OBLIGATIONS_MET, CLOSED_WRITTEN_OFF,
                    CLOSED_RESCHEDULE_OUTSTANDING_AMOUNT, OVERPAID);
            case LOAN_INITIATE_TRANSFER -> transferInProgress();
            case LOAN_REJECT_TRANSFER -> anyOfAllowedWhenComingFrom(from, TRANSFER_IN_PROGRESS) ? transferOnHold() : null;
            case LOAN_WITHDRAW_TRANSFER -> anyOfAllowedWhenComingFrom(from, TRANSFER_IN_PROGRESS) ? activeTransition() : null;
            case WRITE_OFF_OUTSTANDING_UNDO -> anyOfAllowedWhenComingFrom(from, CLOSED_WRITTEN_OFF) ? activeTransition() : null;
            case LOAN_CREDIT_BALANCE_REFUND -> balanceTransition(loan, ACTIVE, OVERPAID);
            case LOAN_CHARGE_ADDED ->
                balanceTransition(loan, (from == APPROVED ? APPROVED : ACTIVE), ACTIVE, CLOSED_OBLIGATIONS_MET, OVERPAID);
            case LOAN_CHARGEBACK -> balanceTransition(loan, ACTIVE, ACTIVE, CLOSED_OBLIGATIONS_MET, OVERPAID);
            case LOAN_CHARGE_ADJUSTMENT -> balanceTransition(loan, ACTIVE, ACTIVE, CLOSED_OBLIGATIONS_MET, CLOSED_WRITTEN_OFF,
                    CLOSED_RESCHEDULE_OUTSTANDING_AMOUNT, OVERPAID);
            default -> null;
        };
    }

    private LoanStatus balanceTransition(@NotNull Loan loan, LoanStatus activeState, LoanStatus... allowedStates) {
        LoanStatus from = loan.getStatus();
        if (!anyOfAllowedWhenComingFrom(from, allowedStates)) {
            return null;
        }
        if (loan.isOverPaid()) {
            return overpaidTransition();
        } else if (loan.getSummary().isRepaidInFull(loan.getCurrency())) {
            return closeObligationsMetTransition();
        } else {
            return activeState;
        }
    }

    private LoanStatus transferOnHold() {
        return stateOf(TRANSFER_ON_HOLD, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus transferInProgress() {
        return stateOf(TRANSFER_IN_PROGRESS, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus overpaidTransition() {
        return stateOf(OVERPAID, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus closedRescheduleOutstandingAmountTransition() {
        return stateOf(CLOSED_RESCHEDULE_OUTSTANDING_AMOUNT, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus closedWrittenOffTransition() {
        return stateOf(CLOSED_WRITTEN_OFF, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus closeObligationsMetTransition() {
        return stateOf(CLOSED_OBLIGATIONS_MET, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus activeTransition() {
        return stateOf(ACTIVE, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus withdrawnByClientTransition() {
        return stateOf(WITHDRAWN_BY_CLIENT, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus approvedTransition() {
        return stateOf(APPROVED, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus rejectedTransition() {
        return stateOf(REJECTED, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus submittedTransition() {
        return stateOf(SUBMITTED_AND_PENDING_APPROVAL, ALLOWED_LOAN_STATUSES);
    }

    private LoanStatus stateOf(final LoanStatus state, final List<LoanStatus> allowedLoanStatuses) {
        LoanStatus match = null;
        for (final LoanStatus loanStatus : allowedLoanStatuses) {
            if (loanStatus.hasStateOf(state)) {
                match = loanStatus;
                break;
            }
        }
        return match;
    }

    private boolean anyOfAllowedWhenComingFrom(final LoanStatus state, final LoanStatus... allowedStates) {
        boolean allowed = false;

        for (final LoanStatus allowedState : allowedStates) {
            if (state.hasStateOf(allowedState)) {
                allowed = true;
                break;
            }
        }

        return allowed;
    }
}
