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
package org.apache.fineract.portfolio.loanaccount.service;

import static org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper.fetchFirstNormalInstallmentNumber;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper.isAfterPeriod;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper.isBeforePeriod;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper.isInPeriod;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction.accrualAdjustment;
import static org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction.accrueTransaction;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.MultiException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.event.business.domain.loan.transaction.LoanAccrualTransactionCreatedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepository;
import org.apache.fineract.portfolio.loanaccount.data.AccrualAmountsData;
import org.apache.fineract.portfolio.loanaccount.data.AccrualChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanScheduleAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInterestRecalcualtionAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInterestRecalculationDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionComparator;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionToRepaymentScheduleMapping;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanproduct.domain.InterestRecalculationCompoundingMethod;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRelatedDetail;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanAccrualsProcessingServiceImpl implements LoanAccrualsProcessingService {

    private static final String ACCRUAL_ON_CHARGE_DUE_DATE = "due-date";
    private static final String ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE = "submitted-date";
    private final LoanChargeReadPlatformService loanChargeReadPlatformService;
    private final ExternalIdFactory externalIdFactory;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final ConfigurationDomainService configurationDomainService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAccrualTransactionBusinessEventService loanAccrualTransactionBusinessEventService;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final LoanTransactionRepository loanTransactionRepository;
    private final PlatformSecurityContext context;
    private final LoanRepository loanRepository;
    private final OfficeRepository officeRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;

    /**
     * method adds accrual for batch job "Add Periodic Accrual Transactions" and add accruals api for Loan
     */
    @Override
    @Transactional
    public void addPeriodicAccruals(@NotNull LocalDate tillDate) throws JobExecutionException {
        List<Loan> loans = loanRepositoryWrapper.findLoansForAccrual(AccountingRuleType.ACCRUAL_PERIODIC.getValue(), tillDate);
        List<Throwable> errors = new ArrayList<>();
        for (Loan loan : loans) {
            try {
                addPeriodicAccruals(tillDate, loan);
            } catch (Exception e) {
                log.error("Failed to add accrual for loan {}", loan.getId(), e);
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            throw new JobExecutionException(errors);
        }
    }

    /**
     * method adds accrual for Loan COB business step
     */
    @Override
    @Transactional
    public void addPeriodicAccruals(@NotNull LocalDate tillDate, @NotNull Loan loan) throws JobExecutionException {
        if (!loan.isOpen() || loan.isNpa() || loan.isChargedOff() || !loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
            return;
        }
        LoanInterestRecalculationDetails recalculationDetails = loan.getLoanInterestRecalculationDetails();
        if (recalculationDetails != null && recalculationDetails.isCompoundingToBePostedAsTransaction()) {
            return;
        }
        boolean progressiveAccrual = isProgressiveAccrual(loan);
        MonetaryCurrency currency = loan.getLoanProductRelatedDetail().getCurrency();

        List<AccrualAmountsData> accrualAmountsList = calculateAccrualAmounts(loan, tillDate);
        List<LoanTransaction> accrualTransactions = new ArrayList<>();
        LoanTransaction progressiveAccrualTransaction = null;
        LoanTransaction progressiveAdjustTransaction = null;
        for (AccrualAmountsData accrualAmounts : accrualAmountsList) {
            Money interestAccruable = accrualAmounts.getInterestAccruable();
            Money interestPortion = MathUtil.minus(interestAccruable, accrualAmounts.getInterestAccrued());
            Money feeAccruable = accrualAmounts.getFeeAccruable();
            Money feePortion = MathUtil.minus(feeAccruable, accrualAmounts.getFeeAccrued());
            Money penaltyAccruable = accrualAmounts.getPenaltyAccruable();
            Money penaltyPortion = MathUtil.minus(penaltyAccruable, accrualAmounts.getPenaltyAccrued());
            if (MathUtil.isEmpty(interestPortion) && MathUtil.isEmpty(feePortion) && MathUtil.isEmpty(penaltyPortion)) {
                continue;
            }
            if (progressiveAccrual) {
                Money interestAdjustmentPortion = MathUtil.negate(interestPortion);
                Money feeAdjustmentPortion = MathUtil.negate(feePortion);
                Money penaltyAdjustmentPortion = MathUtil.negate(penaltyPortion);
                if (progressiveAdjustTransaction == null) {
                    progressiveAdjustTransaction = addAccrualTransaction(loan, tillDate, accrualAmounts, interestAdjustmentPortion,
                            feeAdjustmentPortion, penaltyAdjustmentPortion, true);
                    if (progressiveAdjustTransaction != null) {
                        accrualTransactions.add(progressiveAdjustTransaction);
                    }
                } else {
                    mergeAccrualTransaction(progressiveAdjustTransaction, accrualAmounts, interestAdjustmentPortion, feeAdjustmentPortion,
                            penaltyAdjustmentPortion, true);
                }
                if (progressiveAccrualTransaction == null) {
                    progressiveAccrualTransaction = addAccrualTransaction(loan, tillDate, accrualAmounts, interestPortion, feePortion,
                            penaltyPortion, false);
                    if (progressiveAccrualTransaction != null) {
                        accrualTransactions.add(progressiveAccrualTransaction);
                    }
                } else {
                    mergeAccrualTransaction(progressiveAccrualTransaction, accrualAmounts, interestPortion, feePortion, penaltyPortion,
                            false);
                }
            } else {
                LoanTransaction accrualTransaction = addAccrualTransaction(loan, tillDate, accrualAmounts, interestPortion, feePortion,
                        penaltyPortion, false);
                if (accrualTransaction != null) {
                    accrualTransactions.add(accrualTransaction);
                }
            }
            LoanRepaymentScheduleInstallment installment = loan.fetchRepaymentScheduleInstallment(accrualAmounts.getInstallmentNumber());
            installment.updateAccrualPortion(interestAccruable, feeAccruable, penaltyAccruable);
        }
        ArrayList<Map<String, Object>> newTransactionMapping = new ArrayList<>();
        for (LoanTransaction accrualTransaction : accrualTransactions) {
            loanTransactionRepository.save(accrualTransaction);
            newTransactionMapping.add(accrualTransaction.toMapData(currency.getCode()));
            businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(accrualTransaction));
        }
        loan.setAccruedTill(tillDate);
        loanRepository.saveAndFlush(loan);

        Map<String, Object> accountingBridgeData = deriveAccountingBridgeData(loan, newTransactionMapping);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    private List<AccrualAmountsData> calculateAccrualAmounts(@NotNull Loan loan, @NotNull LocalDate tillDate) {
        final String chargeAccrualDateType = configurationDomainService.getAccrualDateConfigForCharge();
        boolean chargeOnDueDate = ACCRUAL_ON_CHARGE_DUE_DATE.equalsIgnoreCase(chargeAccrualDateType);

        LoanProductRelatedDetail productDetail = loan.getLoanProductRelatedDetail();
        MonetaryCurrency currency = productDetail.getCurrency();
        LoanScheduleGenerator scheduleGenerator = loanScheduleFactory.create(productDetail.getLoanScheduleType(),
                productDetail.getInterestMethod());
        int firstInstallmentNumber = fetchFirstNormalInstallmentNumber(loan.getRepaymentScheduleInstallments());
        List<LoanRepaymentScheduleInstallment> installments = getInstallmentsToAccrue(loan, tillDate);
        ArrayList<AccrualAmountsData> accrualAmounts = new ArrayList<>(installments.size());
        for (LoanRepaymentScheduleInstallment installment : installments) {
            Integer installmentNumber = installment.getInstallmentNumber();
            AccrualAmountsData accrualData = new AccrualAmountsData(installmentNumber, currency);
            accrualAmounts.add(accrualData);
            boolean isFirst = installmentNumber.equals(firstInstallmentNumber);
            addInterestAccrual(loan, tillDate, scheduleGenerator, installment, isFirst, accrualData);
            addChargeAccrual(loan, tillDate, chargeOnDueDate, installment, isFirst, accrualData);
        }
        return accrualAmounts;
    }

    @NotNull
    private List<LoanRepaymentScheduleInstallment> getInstallmentsToAccrue(@NotNull Loan loan, @NotNull LocalDate tillDate) {
        LocalDate organisationStartDate = this.configurationDomainService.retrieveOrganisationStartDate();
        int firstInstallmentNumber = fetchFirstNormalInstallmentNumber(loan.getRepaymentScheduleInstallments());
        return loan
                .getRepaymentScheduleInstallments(i -> !isBeforePeriod(tillDate, i, i.getInstallmentNumber().equals(firstInstallmentNumber))
                        && !isAfterPeriod(organisationStartDate, i)
                        && (!MathUtil.isEqualTo(i.getInterestCharged(), i.getInterestAccrued())
                                || !MathUtil.isEqualTo(i.getFeeChargesCharged(), i.getFeeAccrued())
                                || !MathUtil.isEqualTo(i.getPenaltyCharges(), i.getPenaltyAccrued())));
    }

    private void addInterestAccrual(@NotNull Loan loan, @NotNull LocalDate tillDate, LoanScheduleGenerator scheduleGenerator,
            LoanRepaymentScheduleInstallment installment, boolean isFirstPeriod, AccrualAmountsData accrualData) {
        Money interest = null;
        if (isInPeriod(tillDate, installment, isFirstPeriod)) {
            interest = scheduleGenerator.getDueInterest(loan, installment, tillDate);
        } else if (isAfterPeriod(tillDate, installment)) {
            interest = installment.getInterestCharged(accrualData.getCurrency());
        }
        accrualData.setInterestAmount(interest);
        MonetaryCurrency currency = accrualData.getCurrency();
        Money waived = Money.of(currency, calcInterestWaivedAmount(installment, tillDate));
        accrualData.setInterestAccruable(MathUtil.minusToZero(accrualData.getInterestAmount(), waived));
        accrualData.setInterestAccrued(Money.of(currency, calcInterestAccruedAmount(installment)));
    }

    @NotNull
    private static BigDecimal calcInterestAccruedAmount(@NotNull LoanRepaymentScheduleInstallment installment) {
        return installment.getLoanTransactionToRepaymentScheduleMappings().stream().filter(tm -> {
            LoanTransaction t = tm.getLoanTransaction();
            return !t.isReversed() && (t.isAccrual() || t.isAccrualAdjustment());
        }).map(tm -> tm.getLoanTransaction().isAccrual() ? tm.getInterestPortion() : MathUtil.negate(tm.getInterestPortion()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @NotNull
    private static BigDecimal calcInterestWaivedAmount(@NotNull LoanRepaymentScheduleInstallment installment, @NotNull LocalDate tillDate) {
        return installment.getLoanTransactionToRepaymentScheduleMappings().stream().filter(tm -> {
            LoanTransaction t = tm.getLoanTransaction();
            return !t.isReversed() && t.isInterestWaiver() && !DateUtils.isAfter(t.getTransactionDate(), tillDate);
        }).map(tm -> tm.getLoanTransaction().isAccrual() ? tm.getInterestPortion() : MathUtil.negate(tm.getInterestPortion()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addChargeAccrual(@NotNull Loan loan, @NotNull LocalDate tillDate, boolean chargeOnDueDate,
            LoanRepaymentScheduleInstallment installment, boolean isFirstPeriod, AccrualAmountsData accrualData) {
        LocalDate dueDate = installment.getDueDate();
        List<LoanCharge> loanCharges = loan.getLoanCharges(lc -> lc.isInstalmentFee() ? DateUtils.isEqual(tillDate, dueDate)
                : isChargeDue(lc, tillDate, chargeOnDueDate, installment, isFirstPeriod));
        for (LoanCharge loanCharge : loanCharges) {
            addChargeAccrual(loanCharge, loanCharge.isInstalmentFee() ? installment : null, accrualData);
        }
    }

    private void addChargeAccrual(@NotNull LoanCharge loanCharge, LoanRepaymentScheduleInstallment installment,
            @NotNull AccrualAmountsData accrualData) {
        MonetaryCurrency currency = accrualData.getCurrency();
        Money chargeAmount;
        Collection<LoanChargePaidBy> paidBy;
        Long installmentChargeId = null;
        if (installment == null) {
            chargeAmount = loanCharge.getAmount(currency);
            paidBy = loanCharge.getLoanChargePaidBySet();
        } else {
            LoanInstallmentCharge installmentCharge = loanCharge.getInstallmentLoanCharge(installment.getInstallmentNumber());
            if (installmentCharge == null) {
                return;
            }
            chargeAmount = installmentCharge.getAmount(currency);
            paidBy = loanCharge.getLoanChargePaidBy(pb -> installment.getInstallmentNumber().equals(pb.getInstallmentNumber()));
            installmentChargeId = installmentCharge.getId();
        }
        AccrualChargeData chargeData = new AccrualChargeData(loanCharge.getId(), installmentChargeId, loanCharge.isPenaltyCharge())
                .setChargeAmount(chargeAmount);
        accrualData.addCharge(chargeData);
        Money unrecognized = Money.of(currency, calcChargeUnrecognizedWaiverAmount(paidBy));
        chargeData.setChargeAccruable(MathUtil.minusToZero(chargeData.getChargeAmount(), unrecognized));
        chargeData.setChargeAccrued(Money.of(currency, calcChargeAccruedAmount(paidBy)));
    }

    @NotNull
    private static BigDecimal calcChargeUnrecognizedWaiverAmount(@NotNull Collection<LoanChargePaidBy> loanChargePaidBy) {
        return loanChargePaidBy.stream() // TODO only for cumulative Loan
                .filter(pb -> {
                    LoanTransaction t = pb.getLoanTransaction();
                    return !t.isReversed() && t.isWaiveCharge();
                }).map(pb -> pb.getLoanTransaction().getUnrecognizedIncomePortion()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @NotNull
    private static BigDecimal calcChargeAccruedAmount(@NotNull Collection<LoanChargePaidBy> loanChargePaidBy) {
        return loanChargePaidBy.stream().filter(pb -> {
            LoanTransaction t = pb.getLoanTransaction();
            return !t.isReversed() && (t.isAccrual() || t.isAccrualAdjustment());
        }).map(pb -> pb.getLoanTransaction().isAccrual() ? pb.getAmount() : MathUtil.negate(pb.getAmount())).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    private static boolean isChargeDue(@NotNull LoanCharge loanCharge, @NotNull LocalDate tillDate, boolean chargeOnDueDate,
            LoanRepaymentScheduleInstallment installment, boolean isFirstPeriod) {
        LocalDate fromDate = installment.getFromDate();
        LocalDate dueDate = installment.getDueDate();
        LocalDate toDate = DateUtils.isBefore(dueDate, tillDate) ? dueDate : tillDate;
        if (chargeOnDueDate) {
            return loanCharge.isDueInPeriod(fromDate, toDate, isFirstPeriod);
        } else {
            LocalDate submittedOnDate = loanCharge.getSubmittedOnDate();
            // TODO not correct, what if submittedOnDate and chargeDueDate are in different periods
            // LocalDate chargeDueDate = loanCharge.getDueDate();
            // return ((isFirstPeriod && DateUtils.isEqual(submittedOnDate, fromDate) &&
            // DateUtils.isEqual(chargeDueDate, fromDate)) || DateUtils.isAfter(chargeDueDate, fromDate))
            // && !DateUtils.isAfter(submittedOnDate, toDate)
            // && !DateUtils.isAfter(chargeDueDate, dueDate);
            return isInPeriod(submittedOnDate, fromDate, toDate, isFirstPeriod);
        }
    }

    private LoanTransaction addAccrualTransaction(@NotNull Loan loan, @NotNull LocalDate tillDate, AccrualAmountsData accrualAmounts,
            Money interestPortion, Money feePortion, Money penaltyPortion, boolean adjustment) {
        interestPortion = MathUtil.negativeToZero(interestPortion);
        BigDecimal interest = MathUtil.toBigDecimal(interestPortion);
        feePortion = MathUtil.negativeToZero(feePortion);
        BigDecimal fee = MathUtil.toBigDecimal(feePortion);
        penaltyPortion = MathUtil.negativeToZero(penaltyPortion);
        BigDecimal penalty = MathUtil.toBigDecimal(penaltyPortion);
        BigDecimal amount = MathUtil.add(interest, fee, penalty);
        if (!MathUtil.isGreaterThanZero(amount)) {
            return null;
        }
        LoanTransaction transaction = adjustment
                ? accrualAdjustment(loan, loan.getOffice(), tillDate, amount, interest, fee, penalty, externalIdFactory.create())
                : accrueTransaction(loan, loan.getOffice(), tillDate, amount, interest, fee, penalty, externalIdFactory.create());
        loan.addLoanTransaction(transaction);

        // update repayment schedule portions
        addTransactionMappings(transaction, accrualAmounts, interestPortion, feePortion, penaltyPortion, adjustment);
        return transaction;
    }

    private void mergeAccrualTransaction(@NotNull LoanTransaction transaction, AccrualAmountsData accrualAmounts, Money interestPortion,
            Money feePortion, Money penaltyPortion, boolean adjustment) {
        interestPortion = MathUtil.negativeToZero(interestPortion);
        feePortion = MathUtil.negativeToZero(feePortion);
        penaltyPortion = MathUtil.negativeToZero(penaltyPortion);

        transaction.updateComponentsAndTotal(null, interestPortion, feePortion, penaltyPortion);
        addTransactionMappings(transaction, accrualAmounts, interestPortion, feePortion, penaltyPortion, adjustment);
    }

    private static void addTransactionMappings(@NotNull LoanTransaction transaction, AccrualAmountsData accrualAmounts,
            Money interestPortion, Money feePortion, Money penaltyPortion, boolean adjustment) {
        Loan loan = transaction.getLoan();
        // update repayment schedule portions
        Integer installmentNumber = accrualAmounts.getInstallmentNumber();
        LoanRepaymentScheduleInstallment installment = loan.fetchRepaymentScheduleInstallment(installmentNumber);

        // add installment mapping
        LoanTransactionToRepaymentScheduleMapping installmentMapping = LoanTransactionToRepaymentScheduleMapping.createFrom(transaction,
                installment, null, interestPortion, feePortion, penaltyPortion);
        installment.getLoanTransactionToRepaymentScheduleMappings().add(installmentMapping);

        // add charges paid by mappings
        for (AccrualChargeData accrualCharge : accrualAmounts.getCharges()) {
            Money chargeAccruable = accrualCharge.getChargeAccruable();
            Money chargePortion = MathUtil.minus(chargeAccruable, accrualCharge.getChargeAccrued());
            chargePortion = MathUtil.negativeToZero(adjustment ? MathUtil.negate(chargePortion) : chargePortion);
            if (MathUtil.isEmpty(chargePortion)) {
                continue;
            }
            BigDecimal chargeAmount = MathUtil.toBigDecimal(chargePortion);
            LoanCharge loanCharge = loan.fetchLoanChargesById(accrualCharge.getLoanChargeId());
            transaction.getLoanChargesPaid().add(new LoanChargePaidBy(transaction, loanCharge, chargeAmount, installmentNumber));
            Long installmentChargeId = accrualCharge.getLoanInstallmentChargeId();
            if (installmentChargeId != null) {
                loanCharge.getLoanInstallmentCharge().add(new LoanInstallmentCharge(chargeAmount, loanCharge, installment));
            }
        }
    }

    /**
     * method adds accrual for batch job "Add Accrual Transactions"
     */
    @Override
    @Transactional
    public void addAccrualAccounting(@NotNull Long loanId, @NotNull List<LoanScheduleAccrualData> loanScheduleAccrualData) {
        // TODO
        Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);

        List<LoanChargeData> chargeData = this.loanChargeReadPlatformService.retrieveLoanChargesForAccrual(loanId);
        List<LoanSchedulePeriodData> loanWaiverScheduleData = new ArrayList<>(1);
        List<LoanTransactionData> loanWaiverTransactionData = new ArrayList<>(1);
        for (final LoanScheduleAccrualData accrualData : loanScheduleAccrualData) {
            if (accrualData.getWaivedInterestIncome() != null && loanWaiverScheduleData.isEmpty()) {
                loanWaiverScheduleData = this.loanReadPlatformService.fetchWaiverInterestRepaymentData(accrualData.getLoanId());
                loanWaiverTransactionData = this.loanReadPlatformService.retrieveWaiverLoanTransactions(accrualData.getLoanId());
            }
            updateCharges(chargeData, accrualData, accrualData.getFromDateAsLocaldate(), accrualData.getDueDateAsLocaldate());
            updateInterestIncome(loan, accrualData, loanWaiverTransactionData, loanWaiverScheduleData, accrualData.getDueDateAsLocaldate());
            calculateFinalAccrualsForScheduleAndAddAccrualAccounting(loan, accrualData);
        }
    }

    @Transactional
    @Override
    public void addIncomeAndAccrualTransactions(Long loanId) throws LoanNotFoundException {
        // TODO
        if (loanId != null) {
            Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
            processIncomePostingAndAccruals(loan);
            this.loanRepositoryWrapper.saveAndFlush(loan);
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
            loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);
        }
    }

    /**
     * method updates accrual derived fields on installments and reverse the unprocessed transactions for loan
     * reschedule
     */
    @Override
    public void reprocessExistingAccruals(@NotNull Loan loan) {
        if (isProgressiveAccrual(loan)) {
            return;
        }
        List<LoanTransaction> accruals = retrieveListOfAccrualTransactions(loan);
        if (!accruals.isEmpty()) {
            if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
                reprocessPeriodicAccruals(loan, accruals);
            } else if (loan.isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct()) {
                reprocessNonPeriodicAccruals(loan, accruals);
            }
        }
    }

    /**
     * method calculates accruals for loan with interest recalculation on loan schedule when interest is recalculated
     */
    @Override
    @Transactional
    public void processAccrualsForInterestRecalculation(@NotNull Loan loan, boolean isInterestRecalculationEnabled) {
        if (isProgressiveAccrual(loan)) {
            return;
        }
        LocalDate accruedTill = loan.getAccruedTill();
        if (!isInterestRecalculationEnabled || accruedTill == null) {
            return;
        }
        try {
            addPeriodicAccruals(accruedTill, loan);
        } catch (MultiException e) {
            String globalisationMessageCode = "error.msg.accrual.exception";
            throw new GeneralPlatformDomainRuleException(globalisationMessageCode, e.getMessage(), e);
        }
    }

    /**
     * method calculates accruals for loan with interest recalculation and compounding to be posted as income
     */
    @Override
    public void processIncomePostingAndAccruals(@NotNull Loan loan) {
        if (isProgressiveAccrual(loan)) {
            return;
        }
        LoanInterestRecalculationDetails recalculationDetails = loan.getLoanInterestRecalculationDetails();
        if (recalculationDetails == null || !recalculationDetails.isCompoundingToBePostedAsTransaction()) {
            return;
        }
        LocalDate lastCompoundingDate = loan.getDisbursementDate();
        List<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails = extractInterestRecalculationAdditionalDetails(loan);
        List<LoanTransaction> incomeTransactions = retrieveListOfIncomePostingTransactions(loan);
        List<LoanTransaction> accrualTransactions = retrieveListOfAccrualTransactions(loan);
        for (LoanInterestRecalcualtionAdditionalDetails compoundingDetail : compoundingDetails) {
            if (!DateUtils.isBeforeBusinessDate(compoundingDetail.getEffectiveDate())) {
                break;
            }
            LoanTransaction incomeTransaction = getTransactionForDate(incomeTransactions, compoundingDetail.getEffectiveDate());
            LoanTransaction accrualTransaction = getTransactionForDate(accrualTransactions, compoundingDetail.getEffectiveDate());
            addUpdateIncomeAndAccrualTransaction(loan, compoundingDetail, lastCompoundingDate, incomeTransaction, accrualTransaction);
            lastCompoundingDate = compoundingDetail.getEffectiveDate();
        }
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        LoanRepaymentScheduleInstallment lastInstallment = LoanRepaymentScheduleInstallment.getLastNonDownPaymentInstallment(installments);
        reverseTransactionsPostEffectiveDate(incomeTransactions, lastInstallment.getDueDate());
        reverseTransactionsPostEffectiveDate(accrualTransactions, lastInstallment.getDueDate());
    }

    /**
     * method calculates accruals for loan on loan closure
     */
    @Override
    public void processAccrualsForLoanClosure(@NotNull Loan loan) {
        // check and process accruals for loan WITHOUT interest recalculation details and compounding posted as income
        processAccrualTransactionsOnLoanClosure(loan);

        // check and process accruals for loan WITH interest recalculation details and compounding posted as income
        processIncomeAndAccrualTransactionOnLoanClosure(loan);
    }

    /**
     * method calculates accruals for loan on loan fore closure
     */
    @Override
    public void processAccrualsForLoanForeClosure(@NotNull Loan loan, @NotNull LocalDate foreClosureDate,
            @NotNull List<LoanTransaction> newAccrualTransactions) {
        // TODO analyze progressive accrual case
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()
                && (loan.getAccruedTill() == null || !DateUtils.isEqual(foreClosureDate, loan.getAccruedTill()))) {
            final LoanRepaymentScheduleInstallment foreCloseDetail = loan.fetchLoanForeclosureDetail(foreClosureDate);
            MonetaryCurrency currency = loan.getCurrency();
            reverseTransactionsPostEffectiveDate(retrieveListOfAccrualTransactions(loan), foreClosureDate);

            HashMap<String, Object> incomeDetails = new HashMap<>();

            determineReceivableIncomeForeClosure(loan, foreClosureDate, incomeDetails);

            Money interestPortion = foreCloseDetail.getInterestCharged(currency).minus((Money) incomeDetails.get(Loan.INTEREST));
            Money feePortion = foreCloseDetail.getFeeChargesCharged(currency).minus((Money) incomeDetails.get(Loan.FEE));
            Money penaltyPortion = foreCloseDetail.getPenaltyChargesCharged(currency).minus((Money) incomeDetails.get(Loan.PENALTIES));
            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);

            if (total.isGreaterThanZero()) {
                createAccrualTransactionAndUpdateChargesPaidBy(loan, foreClosureDate, newAccrualTransactions, currency, interestPortion,
                        feePortion, penaltyPortion, total);
            }
        }
    }

    private void calculateFinalAccrualsForScheduleAndAddAccrualAccounting(@NotNull Loan loan, LoanScheduleAccrualData accrualData) {
        // interest
        BigDecimal newAccruedInterest = MathUtil.zeroToNull(accrualData.getAccruableIncome());
        BigDecimal interestPortion = MathUtil.zeroToNull(MathUtil.subtract(newAccruedInterest, accrualData.getAccruedInterestIncome()));

        // fee
        BigDecimal newAccruedFee = MathUtil.zeroToNull(accrualData.getDueDateFeeIncome());
        BigDecimal feePortion = MathUtil
                .zeroToNull(MathUtil.subtract(newAccruedFee, accrualData.getAccruedFeeIncome(), accrualData.getCreditedFee()));

        // penalty
        BigDecimal newAccruedPenalty = MathUtil.zeroToNull(accrualData.getDueDatePenaltyIncome());
        BigDecimal penaltyPortion = MathUtil
                .zeroToNull(MathUtil.subtract(newAccruedPenalty, accrualData.getAccruedPenaltyIncome(), accrualData.getCreditedPenalty()));

        LocalDate accruedTill = ACCRUAL_ON_CHARGE_DUE_DATE.equalsIgnoreCase(configurationDomainService.getAccrualDateConfigForCharge())
                ? accrualData.getDueDateAsLocaldate()
                : DateUtils.getBusinessLocalDate();
        addAccrualAccounting(loan, accrualData, interestPortion, newAccruedInterest, feePortion, newAccruedFee, penaltyPortion,
                newAccruedPenalty, accruedTill);
    }

    private void addAccrualAccounting(@NotNull Loan loan, LoanScheduleAccrualData accrualData, BigDecimal interestPortion,
            BigDecimal newAccruedInterest, BigDecimal feePortion, BigDecimal newAccruedFee, BigDecimal penaltyPortion,
            BigDecimal newAccruedPenalty, final LocalDate accruedTill) throws DataAccessException {
        AppUser user = context.authenticatedUser();
        Office office = officeRepository.getReferenceById(accrualData.getOfficeId());
        MonetaryCurrency currency = loan.getCurrency();
        LoanTransaction adjustTransaction = null;
        if (isProgressiveAccrual(loan)) {
            BigDecimal interestAdjustment = MathUtil.negativeToZero(MathUtil.negate(interestPortion));
            interestPortion = MathUtil.negativeToZero(interestPortion);
            BigDecimal feeAdjustment = MathUtil.negativeToZero(MathUtil.negate(feePortion));
            feePortion = MathUtil.negativeToZero(feePortion);
            BigDecimal penaltyAdjustment = MathUtil.negativeToZero(MathUtil.negate(penaltyPortion));
            penaltyPortion = MathUtil.negativeToZero(penaltyPortion);
            BigDecimal totalAdjustment = MathUtil.add(interestAdjustment, feeAdjustment, penaltyAdjustment);
            if (MathUtil.isGreaterThanZero(totalAdjustment)) {
                adjustTransaction = accrualAdjustment(loan, office, accruedTill, totalAdjustment, interestAdjustment, feeAdjustment,
                        penaltyAdjustment, externalIdFactory.create());
            }
        }

        // create accrual Transaction
        LoanTransaction accrualTransaction = null;
        BigDecimal amount = MathUtil.add(interestPortion, feePortion, penaltyPortion);
        if (MathUtil.isGreaterThanZero(amount)) {
            accrualTransaction = accrueTransaction(loan, office, accruedTill, amount, interestPortion, feePortion, penaltyPortion,
                    externalIdFactory.create());
        }

        if (adjustTransaction == null && accrualTransaction == null) {
            return;
        }

        // update repayment schedule portions
        LoanRepaymentScheduleInstallment loanScheduleInstallment = loan
                .fetchLoanRepaymentScheduleInstallmentByDueDate(accrualData.getDueDate());
        loanScheduleInstallment.updateAccrualPortion(Money.of(currency, newAccruedInterest), Money.of(currency, newAccruedFee),
                Money.of(currency, newAccruedPenalty));
        // update loan accrued till date
        loan.setAccruedTill(accruedTill);
        loan.setLastModifiedBy(user.getId());
        loan.setLastModifiedDate(DateUtils.getAuditOffsetDateTime());

        // update charges paid by
        Integer installmentNumber = accrualData.getInstallmentNumber();
        Map<LoanChargeData, BigDecimal> applicableCharges = accrualData.getApplicableCharges();
        for (Map.Entry<LoanChargeData, BigDecimal> entry : applicableCharges.entrySet()) {
            LoanCharge loanCharge = loanChargeRepository.getReferenceById(entry.getKey().getId());
            if (adjustTransaction != null) {
                adjustTransaction.getLoanChargesPaid()
                        .add(new LoanChargePaidBy(adjustTransaction, loanCharge, entry.getValue(), installmentNumber));
            }
            if (accrualTransaction != null) {
                accrualTransaction.getLoanChargesPaid()
                        .add(new LoanChargePaidBy(accrualTransaction, loanCharge, entry.getValue(), installmentNumber));
            }
        }

        ArrayList<Map<String, Object>> newLoanTransactions = new ArrayList<>(2);
        if (adjustTransaction != null) {
            loanTransactionRepository.save(adjustTransaction);
            loan.addLoanTransaction(adjustTransaction);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(adjustTransaction));
            newLoanTransactions.add(adjustTransaction.toMapData(currency.getCode()));
        }
        if (accrualTransaction != null) {
            loanTransactionRepository.save(accrualTransaction);
            loan.addLoanTransaction(accrualTransaction);
            businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(accrualTransaction));
            newLoanTransactions.add(accrualTransaction.toMapData(currency.getCode()));
        }

        loanRepository.saveAndFlush(loan);

        final Map<String, Object> accountingBridgeData = deriveAccountingBridgeData(accrualData, newLoanTransactions);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    private Map<String, Object> deriveAccountingBridgeData(@NotNull LoanScheduleAccrualData loanScheduleAccrualData,
            @NotNull List<Map<String, Object>> newLoanTransactions) {
        final Map<String, Object> accountingBridgeData = new LinkedHashMap<>();
        accountingBridgeData.put("loanId", loanScheduleAccrualData.getLoanId());
        accountingBridgeData.put("loanProductId", loanScheduleAccrualData.getLoanProductId());
        accountingBridgeData.put("officeId", loanScheduleAccrualData.getOfficeId());
        accountingBridgeData.put("currencyCode", loanScheduleAccrualData.getCurrencyData().getCode());
        accountingBridgeData.put("cashBasedAccountingEnabled", false);
        accountingBridgeData.put("upfrontAccrualBasedAccountingEnabled", false);
        accountingBridgeData.put("periodicAccrualBasedAccountingEnabled", true);
        accountingBridgeData.put("isAccountTransfer", false);
        accountingBridgeData.put("isChargeOff", false);
        accountingBridgeData.put("isFraud", false);

        accountingBridgeData.put("newLoanTransactions", newLoanTransactions);
        return accountingBridgeData;
    }

    private Map<String, Object> deriveAccountingBridgeData(@NotNull Loan loan, List<Map<String, Object>> newLoanTransactions) {
        final Map<String, Object> accountingBridgeData = new LinkedHashMap<>();
        accountingBridgeData.put("loanId", loan.getId());
        accountingBridgeData.put("loanProductId", loan.getLoanProduct().getId());
        accountingBridgeData.put("officeId", loan.getOfficeId());
        accountingBridgeData.put("currencyCode", loan.getCurrencyCode());
        accountingBridgeData.put("cashBasedAccountingEnabled", loan.isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("upfrontAccrualBasedAccountingEnabled", loan.isUpfrontAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("periodicAccrualBasedAccountingEnabled", loan.isPeriodicAccrualAccountingEnabledOnLoanProduct());
        accountingBridgeData.put("isAccountTransfer", false);
        accountingBridgeData.put("isChargeOff", false);
        accountingBridgeData.put("isFraud", false);
        accountingBridgeData.put("newLoanTransactions", newLoanTransactions);
        return accountingBridgeData;
    }

    private void updateCharges(final List<LoanChargeData> chargesData, final LoanScheduleAccrualData accrualData, final LocalDate startDate,
            final LocalDate endDate) {
        final String chargeAccrualDateCriteria = configurationDomainService.getAccrualDateConfigForCharge();
        if (chargeAccrualDateCriteria.equalsIgnoreCase(ACCRUAL_ON_CHARGE_DUE_DATE)) {
            updateChargeForDueDate(chargesData, accrualData, startDate, endDate);
        } else if (chargeAccrualDateCriteria.equalsIgnoreCase(ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE)) {
            updateChargeForSubmittedOnDate(chargesData, accrualData, startDate, endDate);
        }
    }

    private void updateChargeForSubmittedOnDate(List<LoanChargeData> chargesData, LoanScheduleAccrualData accrualData, LocalDate startDate,
            LocalDate endDate) {
        final Map<LoanChargeData, BigDecimal> applicableCharges = new HashMap<>();
        BigDecimal submittedDateFeeIncome = BigDecimal.ZERO;
        BigDecimal submittedDatePenaltyIncome = BigDecimal.ZERO;
        LocalDate scheduleEndDate = accrualData.getDueDateAsLocaldate();
        for (LoanChargeData loanCharge : chargesData) {
            BigDecimal chargeAmount = BigDecimal.ZERO;
            if (isChargeSubmittedDateAndDueDateInRange(accrualData, startDate, endDate, scheduleEndDate, loanCharge)) {
                chargeAmount = loanCharge.getAmount();
                chargeAmount = calculateDueDateCharges(applicableCharges, loanCharge, chargeAmount);
            }
            if (loanCharge.isPenalty()) {
                submittedDatePenaltyIncome = submittedDatePenaltyIncome.add(chargeAmount);
            } else {
                submittedDateFeeIncome = submittedDateFeeIncome.add(chargeAmount);
            }

        }

        if (submittedDateFeeIncome.compareTo(BigDecimal.ZERO) == 0) {
            submittedDateFeeIncome = null;
        }

        if (submittedDatePenaltyIncome.compareTo(BigDecimal.ZERO) == 0) {
            submittedDatePenaltyIncome = null;
        }

        accrualData.updateChargeDetails(applicableCharges, submittedDateFeeIncome, submittedDatePenaltyIncome);
    }

    private boolean isChargeSubmittedDateAndDueDateInRange(LoanScheduleAccrualData accrualData, LocalDate startDate, LocalDate endDate,
            LocalDate scheduleEndDate, LoanChargeData loanCharge) {
        return ((accrualData.getInstallmentNumber() == 1 && DateUtils.isEqual(startDate, loanCharge.getSubmittedOnDate())
                && DateUtils.isEqual(startDate, loanCharge.getDueDate())) || DateUtils.isBefore(startDate, loanCharge.getDueDate()))
                && !DateUtils.isBefore(endDate, loanCharge.getSubmittedOnDate())
                && !DateUtils.isBefore(scheduleEndDate, loanCharge.getDueDate());
    }

    private void updateChargeForDueDate(List<LoanChargeData> chargesData, LoanScheduleAccrualData accrualData, LocalDate startDate,
            LocalDate endDate) {
        final Map<LoanChargeData, BigDecimal> applicableCharges = new HashMap<>();
        BigDecimal dueDateFeeIncome = BigDecimal.ZERO;
        BigDecimal dueDatePenaltyIncome = BigDecimal.ZERO;
        for (LoanChargeData loanCharge : chargesData) {
            BigDecimal chargeAmount = BigDecimal.ZERO;
            if (loanCharge.getDueDate() == null) {
                if (loanCharge.isInstallmentFee() && DateUtils.isEqual(endDate, accrualData.getDueDateAsLocaldate())) {
                    chargeAmount = calculateInstallmentFeeCharges(accrualData, applicableCharges, loanCharge, chargeAmount);
                }
            } else if (isChargeDueDateInRange(accrualData, startDate, endDate, loanCharge)) {
                chargeAmount = loanCharge.getAmount();
                chargeAmount = calculateDueDateCharges(applicableCharges, loanCharge, chargeAmount);
            }

            if (loanCharge.isPenalty()) {
                dueDatePenaltyIncome = dueDatePenaltyIncome.add(chargeAmount);
            } else {
                dueDateFeeIncome = dueDateFeeIncome.add(chargeAmount);
            }
        }

        if (dueDateFeeIncome.compareTo(BigDecimal.ZERO) == 0) {
            dueDateFeeIncome = null;
        }

        if (dueDatePenaltyIncome.compareTo(BigDecimal.ZERO) == 0) {
            dueDatePenaltyIncome = null;
        }

        accrualData.updateChargeDetails(applicableCharges, dueDateFeeIncome, dueDatePenaltyIncome);
    }

    private boolean isChargeDueDateInRange(LoanScheduleAccrualData accrualData, LocalDate startDate, LocalDate endDate,
            LoanChargeData loanCharge) {
        return ((accrualData.getInstallmentNumber() == 1 && DateUtils.isEqual(loanCharge.getDueDate(), startDate))
                || DateUtils.isAfter(loanCharge.getDueDate(), startDate)) && !DateUtils.isAfter(loanCharge.getDueDate(), endDate);
    }

    private BigDecimal calculateDueDateCharges(Map<LoanChargeData, BigDecimal> applicableCharges, LoanChargeData loanCharge,
            BigDecimal chargeAmount) {
        BigDecimal dueDateChargeAmount = chargeAmount;
        if (loanCharge.getAmountUnrecognized() != null) {
            dueDateChargeAmount = dueDateChargeAmount.subtract(loanCharge.getAmountUnrecognized());
        }
        boolean canAddCharge = dueDateChargeAmount.compareTo(BigDecimal.ZERO) > 0;
        if (canAddCharge && (loanCharge.getAmountAccrued() == null || chargeAmount.compareTo(loanCharge.getAmountAccrued()) != 0)) {
            BigDecimal amountForAccrual = dueDateChargeAmount;
            if (loanCharge.getAmountAccrued() != null) {
                amountForAccrual = dueDateChargeAmount.subtract(loanCharge.getAmountAccrued());
            }
            applicableCharges.put(loanCharge, amountForAccrual);
        }
        return dueDateChargeAmount;
    }

    private BigDecimal calculateInstallmentFeeCharges(LoanScheduleAccrualData accrualData,
            Map<LoanChargeData, BigDecimal> applicableCharges, LoanChargeData loanCharge, BigDecimal chargeAmount) {
        BigDecimal installmentFeeChargeAmount = chargeAmount;
        Collection<LoanInstallmentChargeData> installmentData = loanCharge.getInstallmentChargeData();
        for (LoanInstallmentChargeData installmentChargeData : installmentData) {
            if (installmentChargeData.getInstallmentNumber().equals(accrualData.getInstallmentNumber())) {
                BigDecimal accruableForInstallment = installmentChargeData.getAmount();
                if (installmentChargeData.getAmountUnrecognized() != null) {
                    accruableForInstallment = accruableForInstallment.subtract(installmentChargeData.getAmountUnrecognized());
                }
                installmentFeeChargeAmount = accruableForInstallment;
                boolean canAddCharge = installmentFeeChargeAmount.compareTo(BigDecimal.ZERO) > 0;
                if (canAddCharge && (installmentChargeData.getAmountAccrued() == null
                        || installmentFeeChargeAmount.compareTo(installmentChargeData.getAmountAccrued()) != 0)) {
                    BigDecimal amountForAccrual = installmentFeeChargeAmount;
                    if (installmentChargeData.getAmountAccrued() != null) {
                        amountForAccrual = installmentFeeChargeAmount.subtract(installmentChargeData.getAmountAccrued());
                    }
                    applicableCharges.put(loanCharge, amountForAccrual);
                    BigDecimal amountAccrued = installmentFeeChargeAmount;
                    if (loanCharge.getAmountAccrued() != null) {
                        amountAccrued = amountAccrued.add(loanCharge.getAmountAccrued());
                    }
                    loanCharge.updateAmountAccrued(amountAccrued);
                }
                break;
            }
        }
        return installmentFeeChargeAmount;
    }

    private void updateInterestIncome(Loan loan, @NotNull LoanScheduleAccrualData accrualData,
            @NotNull List<LoanTransactionData> loanWaiverTransactions, @NotNull List<LoanSchedulePeriodData> loanSchedulePeriodDataList,
            @NotNull LocalDate tillDate) {
        BigDecimal accruableIncome = accrualData.getAccruableIncome() == null ? accrualData.getInterestIncome()
                : accrualData.getAccruableIncome();
        if (accrualData.getWaivedInterestIncome() != null) {
            List<LoanTransactionData> loanTransactionDatas = new ArrayList<>();

            getLoanWaiverTransactionsInRange(accrualData, loanWaiverTransactions, tillDate, loanTransactionDatas);

            BigDecimal recognized = getWaivedInterestIncome(accrualData, loanSchedulePeriodDataList, loanTransactionDatas);

            BigDecimal interestWaived = accrualData.getWaivedInterestIncome();
            if (interestWaived.compareTo(recognized) > 0) {
                accruableIncome = accruableIncome.subtract(interestWaived.subtract(recognized));
            }
        }

        accrualData.updateAccruableIncome(accruableIncome);
    }

    private BigDecimal getWaivedInterestIncome(LoanScheduleAccrualData accrualData, List<LoanSchedulePeriodData> waivedPeriodDataList,
            List<LoanTransactionData> loanTransactionDatas) {
        BigDecimal recognized = BigDecimal.ZERO;
        BigDecimal unrecognized = BigDecimal.ZERO;
        BigDecimal remainingAmt = BigDecimal.ZERO;

        Iterator<LoanTransactionData> iterator = loanTransactionDatas.iterator();
        for (LoanSchedulePeriodData waivedPeriodData : waivedPeriodDataList) {
            if (MathUtil.isLessThanOrEqualZero(recognized) && MathUtil.isLessThanOrEqualZero(unrecognized) && iterator.hasNext()) {
                LoanTransactionData loanTransactionData = iterator.next();
                recognized = recognized.add(loanTransactionData.getInterestPortion());
                unrecognized = unrecognized.add(loanTransactionData.getUnrecognizedIncomePortion());
            }
            if (DateUtils.isBefore(waivedPeriodData.getDueDate(), accrualData.getDueDateAsLocaldate())) {
                remainingAmt = remainingAmt.add(waivedPeriodData.getInterestWaived());
                if (recognized.compareTo(remainingAmt) > 0) {
                    recognized = recognized.subtract(remainingAmt);
                    remainingAmt = BigDecimal.ZERO;
                } else {
                    remainingAmt = remainingAmt.subtract(recognized);
                    recognized = BigDecimal.ZERO;
                    if (unrecognized.compareTo(remainingAmt) >= 0) {
                        unrecognized = unrecognized.subtract(remainingAmt);
                        remainingAmt = BigDecimal.ZERO;
                    } else if (iterator.hasNext()) {
                        remainingAmt = remainingAmt.subtract(unrecognized);
                        unrecognized = BigDecimal.ZERO;
                    }
                }

            }
        }
        return recognized;
    }

    private void getLoanWaiverTransactionsInRange(LoanScheduleAccrualData accrualData, List<LoanTransactionData> loanWaiverTransactions,
            LocalDate tillDate, List<LoanTransactionData> loanTransactionDatas) {
        for (LoanTransactionData loanTransactionData : loanWaiverTransactions) {
            LocalDate transactionDate = loanTransactionData.getDate();
            if (!DateUtils.isAfter(transactionDate, accrualData.getFromDateAsLocaldate())
                    || (DateUtils.isAfter(transactionDate, accrualData.getFromDateAsLocaldate())
                            && !DateUtils.isAfter(transactionDate, accrualData.getDueDateAsLocaldate())
                            && !DateUtils.isAfter(transactionDate, tillDate))) {
                loanTransactionDatas.add(loanTransactionData);
            }
        }
    }

    private void postJournalEntries(final Loan loan, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds) {
        final MonetaryCurrency currency = loan.getCurrency();
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                existingReversedTransactionIds, isAccountTransfer);
        journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    private void reprocessPeriodicAccruals(Loan loan, final List<LoanTransaction> accruals) {
        if (loan.isChargedOff() || isProgressiveAccrual(loan)) {
            return;
        }
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        boolean isBasedOnSubmittedOnDate = configurationDomainService.getAccrualDateConfigForCharge()
                .equalsIgnoreCase(ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE);
        for (LoanRepaymentScheduleInstallment installment : installments) {
            checkAndUpdateAccrualsForInstallment(loan, accruals, installments, isBasedOnSubmittedOnDate, installment);
        }
        // reverse accruals after last installment
        LoanRepaymentScheduleInstallment lastInstallment = loan.getLastLoanRepaymentScheduleInstallment();
        reverseTransactionsPostEffectiveDate(accruals, lastInstallment.getDueDate());
    }

    private void checkAndUpdateAccrualsForInstallment(Loan loan, List<LoanTransaction> accruals,
            List<LoanRepaymentScheduleInstallment> installments, boolean isBasedOnSubmittedOnDate,
            LoanRepaymentScheduleInstallment installment) {
        Money interest = Money.zero(loan.getCurrency());
        Money fee = Money.zero(loan.getCurrency());
        Money penalty = Money.zero(loan.getCurrency());
        for (LoanTransaction loanTransaction : accruals) {
            LocalDate transactionDateForRange = getDateForRangeCalculation(loanTransaction, isBasedOnSubmittedOnDate);
            boolean isInPeriod = LoanRepaymentScheduleProcessingWrapper.isInPeriod(transactionDateForRange, installment, installments);
            if (isInPeriod) {
                interest = interest.plus(loanTransaction.getInterestPortion(loan.getCurrency()));
                fee = fee.plus(loanTransaction.getFeeChargesPortion(loan.getCurrency()));
                penalty = penalty.plus(loanTransaction.getPenaltyChargesPortion(loan.getCurrency()));
                if (hasIncomeAmountChangedForInstallment(loan, installment, interest, fee, penalty, loanTransaction)) {
                    interest = interest.minus(loanTransaction.getInterestPortion(loan.getCurrency()));
                    fee = fee.minus(loanTransaction.getFeeChargesPortion(loan.getCurrency()));
                    penalty = penalty.minus(loanTransaction.getPenaltyChargesPortion(loan.getCurrency()));
                    loanTransaction.reverse();
                }

            }
        }
        installment.updateAccrualPortion(interest, fee, penalty);
    }

    private boolean hasIncomeAmountChangedForInstallment(Loan loan, LoanRepaymentScheduleInstallment installment, Money interest, Money fee,
            Money penalty, LoanTransaction loanTransaction) {
        // if installment income amount is changed or if loan is interest bearing and interest income not accrued
        return installment.getFeeChargesCharged(loan.getCurrency()).isLessThan(fee)
                || installment.getInterestCharged(loan.getCurrency()).isLessThan(interest)
                || installment.getPenaltyChargesCharged(loan.getCurrency()).isLessThan(penalty)
                || (loan.isInterestBearing() && DateUtils.isEqual(loan.getAccruedTill(), loanTransaction.getTransactionDate())
                        && !DateUtils.isEqual(loan.getAccruedTill(), installment.getDueDate()));
    }

    private LocalDate getDateForRangeCalculation(LoanTransaction loanTransaction, boolean isChargeAccrualBasedOnSubmittedOnDate) {
        // check config for charge accrual date and return date
        return isChargeAccrualBasedOnSubmittedOnDate && !loanTransaction.getLoanChargesPaid().isEmpty()
                ? loanTransaction.getLoanChargesPaid().stream().findFirst().get().getLoanCharge().getEffectiveDueDate()
                : loanTransaction.getTransactionDate();
    }

    private void reprocessNonPeriodicAccruals(Loan loan, final List<LoanTransaction> accruals) {
        if (isProgressiveAccrual(loan)) {
            return;
        }
        final Money interestApplied = Money.of(loan.getCurrency(), loan.getSummary().getTotalInterestCharged());
        ExternalId externalId = ExternalId.empty();
        boolean isExternalIdAutoGenerationEnabled = configurationDomainService.isExternalIdAutoGenerationEnabled();

        for (LoanTransaction loanTransaction : accruals) {
            if (loanTransaction.getInterestPortion(loan.getCurrency()).isGreaterThanZero()) {
                if (loanTransaction.getInterestPortion(loan.getCurrency()).isNotEqualTo(interestApplied)) {
                    loanTransaction.reverse();
                    if (isExternalIdAutoGenerationEnabled) {
                        externalId = ExternalId.generate();
                    }
                    final LoanTransaction interestAppliedTransaction = LoanTransaction.accrueInterest(loan.getOffice(), loan,
                            interestApplied, loan.getDisbursementDate(), externalId);
                    loan.addLoanTransaction(interestAppliedTransaction);
                }
            } else {
                Set<LoanChargePaidBy> chargePaidBies = loanTransaction.getLoanChargesPaid();
                for (final LoanChargePaidBy chargePaidBy : chargePaidBies) {
                    LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                    Money chargeAmount = loanCharge.getAmount(loan.getCurrency());
                    if (chargeAmount.isNotEqualTo(loanTransaction.getAmount(loan.getCurrency()))) {
                        loanTransaction.reverse();
                        loan.handleChargeAppliedTransaction(loanCharge, loanTransaction.getTransactionDate());
                    }
                }
            }
        }
    }

    private void createAccrualTransactionAndUpdateChargesPaidBy(Loan loan, LocalDate foreClosureDate,
            List<LoanTransaction> newAccrualTransactions, MonetaryCurrency currency, Money interestPortion, Money feePortion,
            Money penaltyPortion, Money total) {
        ExternalId accrualExternalId = externalIdFactory.create();
        LoanTransaction accrualTransaction = LoanTransaction.accrueTransaction(loan, loan.getOffice(), foreClosureDate, total.getAmount(),
                interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(), accrualExternalId);
        LocalDate fromDate = loan.getDisbursementDate();
        if (loan.getAccruedTill() != null) {
            fromDate = loan.getAccruedTill();
        }
        newAccrualTransactions.add(accrualTransaction);
        loan.addLoanTransaction(accrualTransaction);
        Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();
        for (LoanCharge loanCharge : loan.getActiveCharges()) {
            boolean isDue = loanCharge.isDueInPeriod(fromDate, foreClosureDate, DateUtils.isEqual(fromDate, loan.getDisbursementDate()));
            if (loanCharge.isActive() && !loanCharge.isPaid() && (isDue || loanCharge.isInstalmentFee())) {
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                        loanCharge.getAmountOutstanding(currency).getAmount(), null);
                accrualCharges.add(loanChargePaidBy);
                loanCharge.getLoanChargePaidBySet().add(loanChargePaidBy);
            }
        }
    }

    private void determineReceivableIncomeForeClosure(Loan loan, final LocalDate tillDate, Map<String, Object> incomeDetails) {
        MonetaryCurrency currency = loan.getCurrency();
        Money receivableInterest = Money.zero(currency);
        Money receivableFee = Money.zero(currency);
        Money receivablePenalty = Money.zero(currency);
        for (final LoanTransaction transaction : loan.getLoanTransactions()) {
            if (transaction.isNotReversed() && !transaction.isRepaymentAtDisbursement() && !transaction.isDisbursement()
                    && !DateUtils.isAfter(transaction.getTransactionDate(), tillDate)) {
                if (transaction.isAccrual()) {
                    receivableInterest = receivableInterest.plus(transaction.getInterestPortion(currency));
                    receivableFee = receivableFee.plus(transaction.getFeeChargesPortion(currency));
                    receivablePenalty = receivablePenalty.plus(transaction.getPenaltyChargesPortion(currency));
                } else if (transaction.isRepaymentLikeType() || transaction.isChargePayment() || transaction.isAccrualAdjustment()) {
                    receivableInterest = receivableInterest.minus(transaction.getInterestPortion(currency));
                    receivableFee = receivableFee.minus(transaction.getFeeChargesPortion(currency));
                    receivablePenalty = receivablePenalty.minus(transaction.getPenaltyChargesPortion(currency));
                }
            }
            if (receivableInterest.isLessThanZero()) {
                receivableInterest = receivableInterest.zero();
            }
            if (receivableFee.isLessThanZero()) {
                receivableFee = receivableFee.zero();
            }
            if (receivablePenalty.isLessThanZero()) {
                receivablePenalty = receivablePenalty.zero();
            }
        }

        incomeDetails.put(Loan.INTEREST, receivableInterest);
        incomeDetails.put(Loan.FEE, receivableFee);
        incomeDetails.put(Loan.PENALTIES, receivablePenalty);
    }

    private List<LoanTransaction> retrieveListOfAccrualTransactions(Loan loan) {
        return loan.getLoanTransactions().stream()
                .filter(transaction -> transaction.isNotReversed() && (transaction.isAccrual() || transaction.isAccrualAdjustment()))
                .sorted(LoanTransactionComparator.INSTANCE).collect(Collectors.toList());
    }

    private List<LoanInterestRecalcualtionAdditionalDetails> extractInterestRecalculationAdditionalDetails(Loan loan) {
        List<LoanInterestRecalcualtionAdditionalDetails> retDetails = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> repaymentSchedule = loan.getRepaymentScheduleInstallments();
        if (null != repaymentSchedule) {
            for (LoanRepaymentScheduleInstallment installment : repaymentSchedule) {
                if (null != installment.getLoanCompoundingDetails()) {
                    retDetails.addAll(installment.getLoanCompoundingDetails());
                }
            }
        }
        retDetails.sort(Comparator.comparing(LoanInterestRecalcualtionAdditionalDetails::getEffectiveDate));
        return retDetails;
    }

    private List<LoanTransaction> retrieveListOfIncomePostingTransactions(Loan loan) {
        return loan.getLoanTransactions().stream() //
                .filter(transaction -> transaction.isNotReversed() && transaction.isIncomePosting()) //
                .sorted(LoanTransactionComparator.INSTANCE).collect(Collectors.toList());
    }

    private LoanTransaction getTransactionForDate(List<LoanTransaction> transactions, LocalDate effectiveDate) {
        for (LoanTransaction loanTransaction : transactions) {
            if (DateUtils.isEqual(effectiveDate, loanTransaction.getTransactionDate())) {
                return loanTransaction;
            }
        }
        return null;
    }

    private void addUpdateIncomeAndAccrualTransaction(Loan loan, LoanInterestRecalcualtionAdditionalDetails compoundingDetail,
            LocalDate lastCompoundingDate, LoanTransaction existingIncomeTransaction, LoanTransaction existingAccrualTransaction) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalties = BigDecimal.ZERO;
        HashMap<String, Object> feeDetails = new HashMap<>();

        if (loan.getLoanInterestRecalculationDetails().getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.INTEREST)) {
            interest = compoundingDetail.getAmount();
        } else if (loan.getLoanInterestRecalculationDetails().getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.FEE)) {
            determineFeeDetails(loan, lastCompoundingDate, compoundingDetail.getEffectiveDate(), feeDetails);
            fee = (BigDecimal) feeDetails.get(Loan.FEE);
            penalties = (BigDecimal) feeDetails.get(Loan.PENALTIES);
        } else if (loan.getLoanInterestRecalculationDetails().getInterestRecalculationCompoundingMethod()
                .equals(InterestRecalculationCompoundingMethod.INTEREST_AND_FEE)) {
            determineFeeDetails(loan, lastCompoundingDate, compoundingDetail.getEffectiveDate(), feeDetails);
            fee = (BigDecimal) feeDetails.get(Loan.FEE);
            penalties = (BigDecimal) feeDetails.get(Loan.PENALTIES);
            interest = compoundingDetail.getAmount().subtract(fee).subtract(penalties);
        }

        ExternalId externalId = ExternalId.empty();
        if (configurationDomainService.isExternalIdAutoGenerationEnabled()) {
            externalId = ExternalId.generate();
        }

        createUpdateIncomePostingTransaction(loan, compoundingDetail, existingIncomeTransaction, interest, fee, penalties, externalId);
        createUpdateAccrualTransaction(loan, compoundingDetail, existingAccrualTransaction, interest, fee, penalties, feeDetails,
                externalId);
        loan.updateLoanOutstandingBalances();
    }

    private void createUpdateAccrualTransaction(Loan loan, LoanInterestRecalcualtionAdditionalDetails compoundingDetail,
            LoanTransaction existingAccrualTransaction, BigDecimal interest, BigDecimal fee, BigDecimal penalties,
            HashMap<String, Object> feeDetails, ExternalId externalId) {
        if (configurationDomainService.isExternalIdAutoGenerationEnabled()) {
            externalId = ExternalId.generate();
        }

        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
            if (existingAccrualTransaction == null
                    || !MathUtil.isEqualTo(existingAccrualTransaction.getAmount(), compoundingDetail.getAmount())) {
                if (existingAccrualTransaction != null) {
                    existingAccrualTransaction.reverse();
                }
                LoanTransaction accrual = LoanTransaction.accrueTransaction(loan, loan.getOffice(), compoundingDetail.getEffectiveDate(),
                        compoundingDetail.getAmount(), interest, fee, penalties, externalId);
                updateLoanChargesPaidBy(loan, accrual, feeDetails, null);
                loan.addLoanTransaction(accrual);
            }
        }
    }

    private void createUpdateIncomePostingTransaction(Loan loan, LoanInterestRecalcualtionAdditionalDetails compoundingDetail,
            LoanTransaction existingIncomeTransaction, BigDecimal interest, BigDecimal fee, BigDecimal penalties, ExternalId externalId) {
        if (existingIncomeTransaction == null) {
            LoanTransaction transaction = LoanTransaction.incomePosting(loan, loan.getOffice(), compoundingDetail.getEffectiveDate(),
                    compoundingDetail.getAmount(), interest, fee, penalties, externalId);
            loan.addLoanTransaction(transaction);
        } else if (existingIncomeTransaction.getAmount(loan.getCurrency()).getAmount().compareTo(compoundingDetail.getAmount()) != 0) {
            existingIncomeTransaction.reverse();
            LoanTransaction transaction = LoanTransaction.incomePosting(loan, loan.getOffice(), compoundingDetail.getEffectiveDate(),
                    compoundingDetail.getAmount(), interest, fee, penalties, externalId);
            loan.addLoanTransaction(transaction);
        }
    }

    private void determineFeeDetails(Loan loan, LocalDate fromDate, LocalDate toDate, Map<String, Object> feeDetails) {
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalties = BigDecimal.ZERO;

        List<Integer> installments = new ArrayList<>();
        List<LoanRepaymentScheduleInstallment> repaymentSchedule = loan.getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : repaymentSchedule) {
            if (DateUtils.isAfter(loanRepaymentScheduleInstallment.getDueDate(), fromDate)
                    && !DateUtils.isAfter(loanRepaymentScheduleInstallment.getDueDate(), toDate)) {
                installments.add(loanRepaymentScheduleInstallment.getInstallmentNumber());
            }
        }

        List<LoanCharge> loanCharges = new ArrayList<>();
        List<LoanInstallmentCharge> loanInstallmentCharges = new ArrayList<>();
        for (LoanCharge loanCharge : loan.getActiveCharges()) {
            boolean isDue = loanCharge.isDueInPeriod(fromDate, toDate, DateUtils.isEqual(fromDate, loan.getDisbursementDate()));
            if (isDue) {
                if (loanCharge.isPenaltyCharge() && !loanCharge.isInstalmentFee()) {
                    penalties = penalties.add(loanCharge.amount());
                    loanCharges.add(loanCharge);
                } else if (!loanCharge.isInstalmentFee()) {
                    fee = fee.add(loanCharge.amount());
                    loanCharges.add(loanCharge);
                }
            } else if (loanCharge.isInstalmentFee()) {
                for (LoanInstallmentCharge installmentCharge : loanCharge.installmentCharges()) {
                    if (installments.contains(installmentCharge.getRepaymentInstallment().getInstallmentNumber())) {
                        fee = fee.add(installmentCharge.getAmount());
                        loanInstallmentCharges.add(installmentCharge);
                    }
                }
            }
        }

        feeDetails.put(Loan.FEE, fee);
        feeDetails.put(Loan.PENALTIES, penalties);
        feeDetails.put("loanCharges", loanCharges);
        feeDetails.put("loanInstallmentCharges", loanInstallmentCharges);
    }

    private void updateLoanChargesPaidBy(Loan loan, LoanTransaction accrual, Map<String, Object> feeDetails,
            LoanRepaymentScheduleInstallment installment) {
        @SuppressWarnings("unchecked")
        List<LoanCharge> loanCharges = (List<LoanCharge>) feeDetails.get("loanCharges");
        @SuppressWarnings("unchecked")
        List<LoanInstallmentCharge> loanInstallmentCharges = (List<LoanInstallmentCharge>) feeDetails.get("loanInstallmentCharges");
        if (loanCharges != null) {
            for (LoanCharge loanCharge : loanCharges) {
                Integer installmentNumber = null == installment ? null : installment.getInstallmentNumber();
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrual, loanCharge,
                        loanCharge.getAmount(loan.getCurrency()).getAmount(), installmentNumber);
                accrual.getLoanChargesPaid().add(loanChargePaidBy);
            }
        }
        if (loanInstallmentCharges != null) {
            for (LoanInstallmentCharge loanInstallmentCharge : loanInstallmentCharges) {
                Integer installmentNumber = null == loanInstallmentCharge.getInstallment() ? null
                        : loanInstallmentCharge.getInstallment().getInstallmentNumber();
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrual, loanInstallmentCharge.getLoanCharge(),
                        loanInstallmentCharge.getAmount(loan.getCurrency()).getAmount(), installmentNumber);
                accrual.getLoanChargesPaid().add(loanChargePaidBy);
            }
        }
    }

    private void reverseTransactionsPostEffectiveDate(List<LoanTransaction> transactions, LocalDate effectiveDate) {
        for (LoanTransaction loanTransaction : transactions) {
            if (DateUtils.isAfter(loanTransaction.getTransactionDate(), effectiveDate)) {
                loanTransaction.reverse();
            }
        }
    }

    private void processAccrualTransactionsOnLoanClosure(Loan loan) {
        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()
                // to avoid collision with processIncomeAccrualTransactionOnLoanClosure()
                && !(loan.getLoanInterestRecalculationDetails() != null
                        && loan.getLoanInterestRecalculationDetails().isCompoundingToBePostedAsTransaction())
                && !loan.isNpa() && !loan.isChargedOff()) {
            HashMap<String, Object> incomeDetails = new HashMap<>();
            MonetaryCurrency currency = loan.getCurrency();
            Money interestPortion = Money.zero(currency);
            Money feePortion = Money.zero(currency);
            Money penaltyPortion = Money.zero(currency);

            determineReceivableIncomeDetailsForLoanClosure(loan, incomeDetails);

            interestPortion = interestPortion.plus((Money) incomeDetails.get(Loan.INTEREST));
            feePortion = feePortion.plus((Money) incomeDetails.get(Loan.FEE));
            penaltyPortion = penaltyPortion.plus((Money) incomeDetails.get(Loan.PENALTIES));

            Money total = interestPortion.plus(feePortion).plus(penaltyPortion);

            if (total.isGreaterThanZero()) {
                LocalDate accrualTransactionDate = getFinalAccrualTransactionDate(loan);
                LoanTransaction accrualTransaction = createAccrualTransaction(loan, interestPortion, feePortion, penaltyPortion, total,
                        accrualTransactionDate);
                updateLoanChargesAndInstallmentChargesPaidBy(loan, accrualTransaction);
                // TODO check if this is required
                // saveLoanTransactionWithDataIntegrityViolationChecks(accrualTransaction);
                accrualTransaction = loanTransactionRepository.saveAndFlush(accrualTransaction);
                loan.addLoanTransaction(accrualTransaction);
                businessEventNotifierService.notifyPostBusinessEvent(new LoanAccrualTransactionCreatedBusinessEvent(accrualTransaction));

                updateLoanInstallmentAccruedPortion(loan);
            }
        }
    }

    private void updateLoanInstallmentAccruedPortion(Loan loan) {
        MonetaryCurrency currency = loan.getCurrency();
        loan.getRepaymentScheduleInstallments().forEach(installment -> {
            installment.updateAccrualPortion(installment.getInterestCharged(currency).minus(installment.getInterestWaived(currency)),
                    installment.getFeeChargesCharged(currency).minus(installment.getFeeChargesWaived(currency)),
                    installment.getPenaltyChargesCharged(currency).minus(installment.getPenaltyChargesWaived(currency)));
        });
    }

    private void updateLoanChargesAndInstallmentChargesPaidBy(Loan loan, LoanTransaction accrualTransaction) {
        MonetaryCurrency currency = loan.getCurrency();
        Set<LoanChargePaidBy> accrualCharges = accrualTransaction.getLoanChargesPaid();

        Map<Long, Money> accrualDetails = loan.getActiveCharges().stream()
                .collect(Collectors.toMap(LoanCharge::getId, v -> Money.zero(currency)));

        loan.getLoanTransactions(e -> !e.isReversed() && (e.isAccrual() || e.isAccrualAdjustment()))
                .forEach(transaction -> transaction.getLoanChargesPaid().forEach(loanChargePaid -> {
                    accrualDetails.computeIfPresent(loanChargePaid.getLoanCharge().getId(), (mappedKey, mappedValue) -> {
                        Money amount = Money.of(currency, loanChargePaid.getAmount());
                        return transaction.isAccrual() ? mappedValue.add(amount) : mappedValue.minus(amount);
                    });
                }));

        loan.getActiveCharges().forEach(loanCharge -> {
            Money amount = loanCharge.getAmount(currency).minus(loanCharge.getAmountWaived(currency));
            if (!loanCharge.isInstalmentFee() && loanCharge.isActive() && accrualDetails.get(loanCharge.getId()).isLessThan(amount)) {
                Money amountToBeAccrued = amount.minus(accrualDetails.get(loanCharge.getId()));
                final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction, loanCharge,
                        amountToBeAccrued.getAmount(), null);
                accrualCharges.add(loanChargePaidBy);
                loanCharge.getLoanChargePaidBySet().add(loanChargePaidBy);
            }
        });

        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
            for (LoanInstallmentCharge installmentCharge : loanRepaymentScheduleInstallment.getInstallmentCharges()) {
                if (installmentCharge.getLoanCharge().isActive()) {
                    Money notWaivedAmount = installmentCharge.getAmount(currency).minus(installmentCharge.getAmountWaived(currency));
                    if (notWaivedAmount.isGreaterThanZero()) {
                        Money amountToBeAccrued = notWaivedAmount.minus(accrualDetails.get(installmentCharge.getLoanCharge().getId()));
                        if (amountToBeAccrued.isGreaterThanZero()) {
                            final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(accrualTransaction,
                                    installmentCharge.getLoanCharge(), amountToBeAccrued.getAmount(),
                                    installmentCharge.getInstallment().getInstallmentNumber());
                            accrualCharges.add(loanChargePaidBy);
                            installmentCharge.getLoanCharge().getLoanChargePaidBySet().add(loanChargePaidBy);
                            accrualDetails.computeIfPresent(installmentCharge.getLoanCharge().getId(),
                                    (mappedKey, mappedValue) -> mappedValue.add(amountToBeAccrued));
                        }
                        accrualDetails.computeIfPresent(installmentCharge.getLoanCharge().getId(), (mappedKey, mappedValue) -> MathUtil
                                .negativeToZero(mappedValue.minus(Money.of(currency, installmentCharge.getAmount()))));
                    }
                }
            }
        }
    }

    private LoanTransaction createAccrualTransaction(Loan loan, Money interestPortion, Money feePortion, Money penaltyPortion, Money total,
            LocalDate accrualTransactionDate) {
        ExternalId externalId = externalIdFactory.create();
        return LoanTransaction.accrueTransaction(loan, loan.getOffice(), accrualTransactionDate, total.getAmount(),
                interestPortion.getAmount(), feePortion.getAmount(), penaltyPortion.getAmount(), externalId);
    }

    private void determineReceivableIncomeDetailsForLoanClosure(Loan loan, Map<String, Object> incomeDetails) {
        MonetaryCurrency currency = loan.getCurrency();
        Money interestPortion = Money.zero(currency);
        Money feePortion = Money.zero(currency);
        Money penaltyPortion = Money.zero(currency);

        for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loan.getRepaymentScheduleInstallments()) {
            // TODO: test with interest waiving
            interestPortion = interestPortion.add(loanRepaymentScheduleInstallment.getInterestCharged(currency))
                    .minus(loanRepaymentScheduleInstallment.getInterestAccrued(currency))
                    .minus(loanRepaymentScheduleInstallment.getInterestWaived(currency));
        }

        List<LoanTransaction> chargeTransactions = loan
                .getLoanTransactions(t -> t.isAccrual() || t.isAccrualAdjustment() || t.isChargesWaiver());
        for (LoanCharge loanCharge : loan.getActiveCharges()) {
            BigDecimal accruedAmount = BigDecimal.ZERO;
            BigDecimal waivedAmount = BigDecimal.ZERO;
            for (LoanTransaction chargeTransaction : chargeTransactions) {
                for (LoanChargePaidBy loanChargePaidBy : chargeTransaction.getLoanChargesPaid()) {
                    if (loanChargePaidBy.getLoanCharge().getId().equals(loanCharge.getId())) {
                        BigDecimal amount = chargeTransaction.getAmount();
                        if (chargeTransaction.isAccrual()) {
                            accruedAmount = accruedAmount.add(amount);
                        } else if (chargeTransaction.isAccrualAdjustment()) {
                            waivedAmount = accruedAmount.subtract(amount);
                        } else if (chargeTransaction.isChargesWaiver()) {
                            waivedAmount = waivedAmount.add(amount);
                        }
                    }
                }
            }
            Money needToAccrueAmount = MathUtil.negativeToZero(loanCharge.getAmount(currency).minus(accruedAmount).minus(waivedAmount));
            if (loanCharge.isPenaltyCharge()) {
                penaltyPortion = penaltyPortion.add(needToAccrueAmount);
            } else if (loanCharge.isFeeCharge()) {
                feePortion = feePortion.add(needToAccrueAmount);
            }
        }

        incomeDetails.put(Loan.INTEREST, interestPortion);
        incomeDetails.put(Loan.FEE, feePortion);
        incomeDetails.put(Loan.PENALTIES, penaltyPortion);
    }

    private void processIncomeAndAccrualTransactionOnLoanClosure(Loan loan) {
        // TODO analyze progressive accrual case
        if (loan.getLoanInterestRecalculationDetails() != null
                && loan.getLoanInterestRecalculationDetails().isCompoundingToBePostedAsTransaction()
                && loan.getStatus().isClosedObligationsMet() && !loan.isNpa() && !loan.isChargedOff()) {

            LocalDate closedDate = loan.getClosedOnDate();
            reverseTransactionsOnOrAfter(retrieveListOfIncomePostingTransactions(loan), closedDate);
            reverseTransactionsOnOrAfter(retrieveListOfAccrualTransactions(loan), closedDate);

            HashMap<String, BigDecimal> cumulativeIncomeFromInstallments = new HashMap<>();
            determineCumulativeIncomeFromInstallments(loan, cumulativeIncomeFromInstallments);
            HashMap<String, BigDecimal> cumulativeIncomeFromIncomePosting = new HashMap<>();
            determineCumulativeIncomeDetails(loan, retrieveListOfIncomePostingTransactions(loan), cumulativeIncomeFromIncomePosting);

            BigDecimal interestToPost = cumulativeIncomeFromInstallments.get(Loan.INTEREST)
                    .subtract(cumulativeIncomeFromIncomePosting.get(Loan.INTEREST));
            BigDecimal feeToPost = cumulativeIncomeFromInstallments.get(Loan.FEE).subtract(cumulativeIncomeFromIncomePosting.get(Loan.FEE));
            BigDecimal penaltyToPost = cumulativeIncomeFromInstallments.get(Loan.PENALTY)
                    .subtract(cumulativeIncomeFromIncomePosting.get(Loan.PENALTY));
            BigDecimal amountToPost = interestToPost.add(feeToPost).add(penaltyToPost);

            createIncomePostingAndAccrualTransactionOnLoanClosure(loan, closedDate, interestToPost, feeToPost, penaltyToPost, amountToPost);
        }
        loan.updateLoanOutstandingBalances();
    }

    private void createIncomePostingAndAccrualTransactionOnLoanClosure(Loan loan, LocalDate closedDate, BigDecimal interestToPost,
            BigDecimal feeToPost, BigDecimal penaltyToPost, BigDecimal amountToPost) {
        ExternalId externalId = ExternalId.empty();
        boolean isExternalIdAutoGenerationEnabled = configurationDomainService.isExternalIdAutoGenerationEnabled();

        if (isExternalIdAutoGenerationEnabled) {
            externalId = ExternalId.generate();
        }
        LoanTransaction finalIncomeTransaction = LoanTransaction.incomePosting(loan, loan.getOffice(), closedDate, amountToPost,
                interestToPost, feeToPost, penaltyToPost, externalId);
        loan.addLoanTransaction(finalIncomeTransaction);

        if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
            List<LoanTransaction> updatedAccrualTransactions = retrieveListOfAccrualTransactions(loan);
            LocalDate lastAccruedDate = loan.getDisbursementDate();
            if (!updatedAccrualTransactions.isEmpty()) {
                lastAccruedDate = updatedAccrualTransactions.get(updatedAccrualTransactions.size() - 1).getTransactionDate();
            }
            HashMap<String, Object> feeDetails = new HashMap<>();
            determineFeeDetails(loan, lastAccruedDate, closedDate, feeDetails);
            if (isExternalIdAutoGenerationEnabled) {
                externalId = ExternalId.generate();
            }
            LoanTransaction finalAccrual = LoanTransaction.accrueTransaction(loan, loan.getOffice(), closedDate, amountToPost,
                    interestToPost, feeToPost, penaltyToPost, externalId);
            updateLoanChargesPaidBy(loan, finalAccrual, feeDetails, null);
            loan.addLoanTransaction(finalAccrual);
        }
    }

    private void reverseTransactionsOnOrAfter(List<LoanTransaction> transactions, LocalDate date) {
        for (LoanTransaction loanTransaction : transactions) {
            if (!DateUtils.isBefore(loanTransaction.getTransactionDate(), date)) {
                loanTransaction.reverse();
            }
        }
    }

    private void determineCumulativeIncomeFromInstallments(Loan loan, HashMap<String, BigDecimal> cumulativeIncomeFromInstallments) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment installment : installments) {
            interest = interest.add(installment.getInterestCharged(loan.getCurrency()).getAmount());
            fee = fee.add(installment.getFeeChargesCharged(loan.getCurrency()).getAmount());
            penalty = penalty.add(installment.getPenaltyChargesCharged(loan.getCurrency()).getAmount());
        }
        cumulativeIncomeFromInstallments.put(Loan.INTEREST, interest);
        cumulativeIncomeFromInstallments.put(Loan.FEE, fee);
        cumulativeIncomeFromInstallments.put(Loan.PENALTY, penalty);
    }

    private void determineCumulativeIncomeDetails(Loan loan, List<LoanTransaction> transactions,
            HashMap<String, BigDecimal> incomeDetailsMap) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal penalty = BigDecimal.ZERO;
        for (LoanTransaction transaction : transactions) {
            interest = interest.add(transaction.getInterestPortion(loan.getCurrency()).getAmount());
            fee = fee.add(transaction.getFeeChargesPortion(loan.getCurrency()).getAmount());
            penalty = penalty.add(transaction.getPenaltyChargesPortion(loan.getCurrency()).getAmount());
        }
        incomeDetailsMap.put(Loan.INTEREST, interest);
        incomeDetailsMap.put(Loan.FEE, fee);
        incomeDetailsMap.put(Loan.PENALTY, penalty);
    }

    private LocalDate getFinalAccrualTransactionDate(Loan loan) {
        return switch (loan.getStatus()) {
            case CLOSED_OBLIGATIONS_MET -> loan.getClosedOnDate();
            case OVERPAID -> loan.getOverpaidOnDate();
            default -> throw new IllegalStateException("Unexpected value: " + loan.getStatus());
        };
    }

    public static boolean isProgressiveAccrual(@NotNull Loan loan) {
        return loan.getLoanProductRelatedDetail().getLoanScheduleType() == LoanScheduleType.PROGRESSIVE;
    }
}
