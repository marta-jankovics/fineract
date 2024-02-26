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
package org.apache.fineract.currentaccount.service.accounting.write.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingConstants;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccount;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMapping;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMappingRepository;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.domain.accounting.GLAccountingHistory;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductCashBasedAccount;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accounting.CurrentAccountAccountingRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.accounting.write.CurrentAccountAccountingWriteService;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepository;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class CurrentAccountAccountingWriteServiceImpl implements CurrentAccountAccountingWriteService {

    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentTransactionRepository currentTransactionRepository;
    private final CurrentAccountAccountingRepository currentAccountAccountingRepository;
    private final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepository;
    private final ProductToGLAccountMappingRepository accountMappingRepository;
    private final OfficeRepository officeRepository;
    private final JournalEntryRepository glJournalEntryRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createGLEntriesInNewTransaction(String accountId, OffsetDateTime tillDateTime) {
        createGLEntries(accountId, tillDateTime);
    }

    @Override
    public void createGLEntries(String accountId, OffsetDateTime tillDateTime) {
        CurrentAccountData currentAccountData = currentAccountRepository.getAccountDataById(accountId);
        if (!currentAccountData.getAccountingRuleType().equals(AccountingRuleType.CASH_BASED)) {
            return;
        }
        GLAccountingHistory accountingHistory = currentAccountAccountingRepository
                .findByAccountTypeAndAccountId(PortfolioAccountType.CURRENT, accountId)
                .orElse(new GLAccountingHistory(accountId, PortfolioAccountType.CURRENT, BigDecimal.ZERO, null));

        boolean allStrict = currentAccountData.getBalanceCalculationType().isStrict();
        List<CurrentTransaction> transactionList;
        // TODO CURRENT! no need to load the transactions
        if (accountingHistory.isNew()) {
            transactionList = allStrict ? currentTransactionRepository.getTransactionsSorted(accountId)
                    : currentTransactionRepository.getTransactionsTillSorted(accountId, tillDateTime);
        } else {
            CurrentTransaction currentTransaction = currentTransactionRepository.findById(accountingHistory.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("current.transaction", "Current transaction with id {} does not found",
                            accountingHistory.getTransactionId()));
            OffsetDateTime createdDateTime = currentTransaction.getCreatedDateTime();
            transactionList = allStrict ? currentTransactionRepository.getTransactionsFromSorted(accountId, createdDateTime)
                    : currentTransactionRepository.getTransactionsFromAndTillSorted(accountId, createdDateTime, tillDateTime);
        }

        Office office = officeRepository.getReferenceById(currentAccountData.getOfficeId());
        transactionList.forEach(transaction -> {
            // TODO CURRENT! could be already filtered for allowed transaction types
            if (transaction.getTransactionType().isMonetary()) {
                createJournalEntryForTransaction(office, currentAccountData, transaction, accountingHistory);
                accountingHistory.setTransactionId(transaction.getId());
                accountingHistory.setAccountBalance(calculateBalance(accountingHistory, transaction));
            }
        });
        if (!transactionList.isEmpty()) {
            currentAccountAccountingRepository.save(accountingHistory);
        }
    }

    private BigDecimal calculateBalance(GLAccountingHistory accountingHistory, CurrentTransaction transaction) {
        CurrentTransactionType transactionType = transaction.getTransactionType();
        BigDecimal balance = accountingHistory.getAccountBalance();
        if (transactionType.isMonetaryCredit()) { // non-monetary transactions do not change the balance
            return MathUtil.add(balance, transaction.getAmount());
        } else if (transactionType.isMonetaryDebit()) {
            return MathUtil.subtract(balance, transaction.getAmount());
        }
        return balance;
    }

    private void createJournalEntryForTransaction(Office office, CurrentAccountData accountData, CurrentTransaction transaction,
            GLAccountingHistory accountingHistory) {
        CurrentProductCashBasedAccount debitAccountReferenceId;
        CurrentProductCashBasedAccount creditAccountReferenceId;
        CurrentProductCashBasedAccount overdraftDebitReferenceId;
        CurrentProductCashBasedAccount overdraftCreditReferenceId;
        switch (transaction.getTransactionType()) {
            case DEPOSIT -> {
                debitAccountReferenceId = CurrentProductCashBasedAccount.REFERENCE;
                creditAccountReferenceId = CurrentProductCashBasedAccount.CONTROL;
                overdraftDebitReferenceId = CurrentProductCashBasedAccount.REFERENCE;
                overdraftCreditReferenceId = CurrentProductCashBasedAccount.OVERDRAFT_CONTROL;
            }
            case WITHDRAWAL -> {
                debitAccountReferenceId = CurrentProductCashBasedAccount.CONTROL;
                creditAccountReferenceId = CurrentProductCashBasedAccount.REFERENCE;
                overdraftDebitReferenceId = CurrentProductCashBasedAccount.OVERDRAFT_CONTROL;
                overdraftCreditReferenceId = CurrentProductCashBasedAccount.REFERENCE;
            }
            case WITHDRAWAL_FEE -> {
                debitAccountReferenceId = CurrentProductCashBasedAccount.CONTROL;
                creditAccountReferenceId = CurrentProductCashBasedAccount.INCOME_FROM_FEES;
                overdraftDebitReferenceId = CurrentProductCashBasedAccount.OVERDRAFT_CONTROL;
                overdraftCreditReferenceId = CurrentProductCashBasedAccount.INCOME_FROM_FEES;
            }
            default -> throw new UnsupportedOperationException(
                    String.format("Unhandled transaction type for Current account accounting: %s", transaction.getTransactionType()));
        }
        createJournalEntryForTransactionType(office, accountData, transaction, accountingHistory, debitAccountReferenceId,
                creditAccountReferenceId, overdraftDebitReferenceId, overdraftCreditReferenceId);
    }

    private void createJournalEntryForTransactionType(Office office, CurrentAccountData accountData, CurrentTransaction transaction,
            GLAccountingHistory accountingHistory, CurrentProductCashBasedAccount debitAccountReferenceId,
            CurrentProductCashBasedAccount creditAccountReferenceId, CurrentProductCashBasedAccount overdraftDebitReferenceId,
            CurrentProductCashBasedAccount overdraftCreditReferenceId) {
        BigDecimal overdraftAmount = MathUtil.isLessThanZero(accountingHistory.getAccountBalance())
                ? accountingHistory.getAccountBalance().negate()
                : BigDecimal.ZERO;
        BigDecimal transactionBalance = transaction.getAmount();
        if (MathUtil.isGreaterThanZero(overdraftAmount)) {
            BigDecimal internalTransactionBalance = MathUtil.isGreaterThan(transactionBalance, overdraftAmount) ? overdraftAmount
                    : transactionBalance;

            createJournalEntries(office, accountData.getCurrencyCode(), overdraftDebitReferenceId.getId(),
                    overdraftCreditReferenceId.getId(), accountData.getProductId(), transaction.getPaymentTypeId(), transaction.getId(),
                    transaction.getTransactionDate(), transaction.getSubmittedOnDate(), internalTransactionBalance);

            transactionBalance = transactionBalance.subtract(internalTransactionBalance);
        }
        if (MathUtil.isGreaterThanZero(transactionBalance)) {
            createJournalEntries(office, accountData.getCurrencyCode(), debitAccountReferenceId.getId(), creditAccountReferenceId.getId(),
                    accountData.getProductId(), transaction.getPaymentTypeId(), transaction.getId(), transaction.getTransactionDate(),
                    transaction.getSubmittedOnDate(), transactionBalance);
        }
    }

    private void createJournalEntries(final Office office, final String currencyCode, final int accountTypeToDebitId,
            final int accountTypeToCreditId, final String currentProductId, final Long paymentTypeId, final String transactionId,
            final LocalDate transactionDate, final LocalDate submittedOnDate, final BigDecimal amount) {
        final GLAccount debitAccount = getLinkedGLAccountForSavingsProduct(currentProductId, accountTypeToDebitId, paymentTypeId);
        final GLAccount creditAccount = getLinkedGLAccountForSavingsProduct(currentProductId, accountTypeToCreditId, paymentTypeId);
        createJournalEntry(office, currencyCode, JournalEntryType.DEBIT, debitAccount, transactionId, transactionDate, submittedOnDate,
                amount);
        createJournalEntry(office, currencyCode, JournalEntryType.CREDIT, creditAccount, transactionId, transactionDate, submittedOnDate,
                amount);
    }

    private GLAccount getLinkedGLAccountForSavingsProduct(final String currentProductId, final int accountMappingTypeId,
            final Long paymentTypeId) {
        GLAccount glAccount;
        if (isOrganizationAccount(accountMappingTypeId)) {
            FinancialActivityAccount financialActivityAccount = this.financialActivityAccountRepository
                    .findByFinancialActivityTypeWithNotFoundDetection(accountMappingTypeId);
            glAccount = financialActivityAccount.getGlAccount();
        } else {
            ProductToGLAccountMapping accountMapping = this.accountMappingRepository.findCoreProductToFinAccountMapping(currentProductId,
                    PortfolioProductType.CURRENT.getValue(), accountMappingTypeId);
            /****
             * Get more specific mapping for FUND source accounts (based on payment channels). Note that fund source
             * placeholder ID would be same for both cash and accrual accounts
             ***/
            if (accountMappingTypeId == AccountingConstants.CashAccountsForSavings.SAVINGS_REFERENCE.getValue()) {
                final ProductToGLAccountMapping paymentChannelSpecificAccountMapping = this.accountMappingRepository
                        .findByProductIdentifierAndProductTypeAndFinancialAccountTypeAndPaymentTypeId(currentProductId,
                                PortfolioProductType.CURRENT.getValue(), accountMappingTypeId, paymentTypeId);
                if (paymentChannelSpecificAccountMapping != null) {
                    accountMapping = paymentChannelSpecificAccountMapping;
                }
            }
            glAccount = accountMapping.getGlAccount();
        }
        return glAccount;
    }

    private boolean isOrganizationAccount(final int accountMappingTypeId) {
        return AccountingConstants.FinancialActivity.fromInt(accountMappingTypeId) != null;
    }

    private void createJournalEntry(final Office office, final String currencyCode, final JournalEntryType entryType,
            final GLAccount account, final String transactionId, final LocalDate transactionDate, final LocalDate submittedOnDate,
            final BigDecimal amount) {
        final boolean manualEntry = false;
        final JournalEntry journalEntry = JournalEntry.createNewForCurrentAccount(office, account, currencyCode, transactionId, manualEntry,
                transactionDate, entryType, amount, PortfolioProductType.CURRENT.getValue(), submittedOnDate);

        glJournalEntryRepository.save(journalEntry);
    }
}
