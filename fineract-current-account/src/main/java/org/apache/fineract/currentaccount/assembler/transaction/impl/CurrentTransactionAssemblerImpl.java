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
package org.apache.fineract.currentaccount.assembler.transaction.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_CODE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.EXTERNAL_ID_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_AMOUNT_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_DATE_PARAM;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.TRANSACTION_AMOUNT_RELEASE;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.AMOUNT_HOLD;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.AMOUNT_RELEASE;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.DEPOSIT;
import static org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType.WITHDRAWAL;
import static org.apache.fineract.infrastructure.core.filters.CorrelationHeaderFilter.CORRELATION_ID_KEY;
import static org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants.DATATABLES_PARAM;
import static org.apache.fineract.infrastructure.dataqueries.data.EntityTables.CURRENT_TRANSACTION;

import com.google.gson.JsonArray;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.exception.UnsupportedCommandException;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.data.account.BalanceCalculationData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.paymentdetail.PaymentDetailConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

@RequiredArgsConstructor
@Slf4j
public class CurrentTransactionAssemblerImpl implements CurrentTransactionAssembler {

    private final ExternalIdFactory externalIdFactory;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final CurrentProductRepository currentProductRepository;
    private final CurrentTransactionRepository currentTransactionRepository;
    private final CurrentAccountBalanceReadService accountBalanceReadService;
    private final CurrentAccountBalanceWriteService accountBalanceWriteService;

    @Override
    public CurrentTransaction assemble(JsonCommand command) {
        throw new UnsupportedCommandException("assemble", "Transaction can not be assembled");
    }

    @Override
    public Map<String, Object> update(CurrentTransaction account, JsonCommand command) {
        throw new UnsupportedCommandException("update", "Transaction can not be updated");
    }

    @Override
    public CurrentTransaction deposit(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        return assemble(command, account, DEPOSIT, false);
    }

    @Override
    public CurrentTransaction withdrawal(CurrentAccount account, JsonCommand command, Map<String, Object> changes, boolean force) {
        return assemble(command, account, WITHDRAWAL, force);
    }

    @Override
    public CurrentTransaction hold(CurrentAccount account, JsonCommand command, Map<String, Object> changes) {
        return assemble(command, account, AMOUNT_HOLD, false);
    }

    @Override
    public CurrentTransaction release(CurrentAccount account, CurrentTransaction holdTransaction, Map<String, Object> changes) {
        ExternalId externalId = externalIdFactory.create();
        LocalDate actualDate = DateUtils.getBusinessLocalDate();

        if (!holdTransaction.getTransactionType().isAmountOnHold()) {
            throw new GeneralPlatformDomainRuleException("error.msg.release.amount.transaction",
                    "Only a hold transaction can be released!");
        }
        final boolean isAlreadyReleased = currentTransactionRepository
                .existsByTransactionTypeAndReferenceId(CurrentTransactionType.AMOUNT_RELEASE, holdTransaction.getId());

        if (isAlreadyReleased) {
            throw new GeneralPlatformDomainRuleException("error.msg.hold.transaction.is.already.released",
                    "Release cannot be performed: It was already released");
        }

        CurrentTransaction transaction = CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY),
                holdTransaction.getId(), holdTransaction.getPaymentTypeId(), AMOUNT_RELEASE, actualDate, actualDate,
                holdTransaction.getAmount());

        account.setNextStatus(TRANSACTION_AMOUNT_RELEASE);
        handleBalance(account, transaction, false, TRANSACTION_AMOUNT_RELEASE);

        return currentTransactionRepository.save(transaction);
    }

    @NotNull
    private CurrentTransaction assemble(JsonCommand command, CurrentAccount account, CurrentTransactionType transactionType,
            boolean force) {
        ExternalId externalId = externalIdFactory.createFromCommand(command, EXTERNAL_ID_PARAM);
        final Long paymentTypeId = command.longValueOfParameterNamed(PaymentDetailConstants.paymentTypeParamName);

        LocalDate transactionDate = command.localDateValueOfParameterNamed(TRANSACTION_DATE_PARAM);
        if (transactionDate == null) {
            transactionDate = DateUtils.getBusinessLocalDate();
        }
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(TRANSACTION_AMOUNT_PARAM);
        LocalDate submittedOnDate = DateUtils.getBusinessLocalDate();

        CurrentTransaction transaction = CurrentTransaction.newInstance(account.getId(), externalId, MDC.get(CORRELATION_ID_KEY), null,
                paymentTypeId, transactionType, transactionDate, submittedOnDate, transactionAmount);

        String currencyCode = command.stringValueOfParameterNamedAllowingNull(CURRENCY_CODE_PARAM);
        validateTransaction(account, transaction, currencyCode);

        CurrentAccountAction action = CurrentAccountAction.forTransactionType(transactionType);
        if (action == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.current.action.not.allowed",
                    "Current Account action is not allowed on transaction " + transactionType);
        }

        account.setNextStatus(action);
        handleBalance(account, transaction, force, action);

        JsonArray datatables = command.arrayOfParameterNamed(DATATABLES_PARAM);
        if (datatables != null && !datatables.isEmpty()) {
            transaction = currentTransactionRepository.saveAndFlush(transaction);
            persistDatatableEntries(CURRENT_TRANSACTION, transaction.getId(), datatables, false, readWriteNonCoreDataService);
        } else {
            transaction = currentTransactionRepository.save(transaction);
        }
        return transaction;
    }

    private void validateTransaction(CurrentAccount account, CurrentTransaction transaction, String currencyCode) {
        final DataValidatorBuilder dataValidator = new DataValidatorBuilder()
                .resource(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        if (currencyCode != null) {
            final CurrentProduct product = currentProductRepository.findById(account.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("current.product",
                            "Current product with provided id: %s cannot be found", account.getProductId()));
            if (!currencyCode.equals(product.getCurrency().getCode())) {
                dataValidator.reset().parameter(CURRENCY_CODE_PARAM).value(currencyCode).failWithCode("should.match.product.value");
            }
        }
        dataValidator.throwValidationErrors();
    }

    private BalanceCalculationData calculateBalance(@NotNull CurrentAccount account, @NotNull CurrentAccountAction action) {
        boolean hasDelay = account.hasBalanceDelay(action);
        return accountBalanceReadService.calculateBalance(account.getId(),
                hasDelay ? accountBalanceReadService.getBalanceCalculationTill() : null);
    }

    private BalanceCalculationData handleBalance(@NotNull CurrentAccount account, @NotNull CurrentTransaction transaction, boolean force,
            @NotNull CurrentAccountAction action) {
        CurrentTransactionType transactionType = transaction.getTransactionType();
        if (!transactionType.isDebit() && !account.isBalancePersist(action)) {
            return null;
        }
        // calculated before the transaction is persisted
        BalanceCalculationData balance = calculateBalance(account, action);

        // Balance has a FK reference on transaction, but we don't have reference on entity level,
        // so JPA cannot be sure about the order of saving balance and transaction
        transaction = currentTransactionRepository.saveAndFlush(transaction);
        balance.applyTransaction(transaction);
        checkBalance(account, balance, transactionType, force);
        if (account.isBalancePersist(action)) {
            boolean hasDelay = account.hasBalanceDelay(action);
            CurrentAccountBalanceData balanceData = hasDelay ? balance.getDelayData() : balance.getTotalData();
            if (balanceData.isChanged()) {
                accountBalanceWriteService.saveBalance(balanceData);
            }
        }
        return balance;
    }

    private void checkBalance(@NotNull CurrentAccount account, @NotNull BalanceCalculationData balance,
            CurrentTransactionType transactionType, boolean force) {
        if (!transactionType.isDebit() || force) {
            return;
        }
        BigDecimal accountBalance = balance.getAccountBalance();
        if (MathUtil.isLessThanZero(accountBalance) && !account.isAllowOverdraft()) {
            throw new GeneralPlatformDomainRuleException("error.msg.current.insufficient.funds.balance",
                    "Insufficient founds! Current balance: " + accountBalance);
        }
        BigDecimal availableBalance = account.getAvailableBalance(balance, true);
        if (MathUtil.isLessThanZero(availableBalance)) {
            String code = !MathUtil.isEmpty(account.getMinimumRequiredBalance()) ? "error.msg.current.insufficient.funds.minrequiredbalance"
                    : "error.msg.current.insufficient.funds.maxoverdraft";
            throw new GeneralPlatformDomainRuleException(code,
                    "Insufficient funds! Current balance: " + accountBalance + ", available balance: " + availableBalance);
        }
    }
}
