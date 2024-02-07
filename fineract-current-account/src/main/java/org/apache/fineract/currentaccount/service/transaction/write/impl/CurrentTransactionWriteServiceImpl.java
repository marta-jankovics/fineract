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
package org.apache.fineract.currentaccount.service.transaction.write.impl;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction;
import org.apache.fineract.currentaccount.enumeration.product.BalanceCalculationType;
import org.apache.fineract.currentaccount.exception.transaction.CurrentTransactionNotFoundException;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionWriteServiceImpl implements CurrentTransactionWriteService {

    private final CurrentTransactionDataValidator currentTransactionDataValidator;
    private final CurrentTransactionAssembler currentTransactionAssembler;
    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentTransactionRepository currentTransactionRepository;
    // TODO: use service eventually
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final CurrentAccountBalanceWriteService currentAccountBalanceWriteService;
    private final CurrentAccountWriteService currentAccountWriteService;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult deposit(String accountId, JsonCommand command) {
        currentTransactionDataValidator.validateDeposit(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        currentAccountWriteService.checkEnabled(account, true);
        CurrentAccountBalanceData balanceData = calculateCreditBalance(account);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction depositTransaction = currentTransactionAssembler.deposit(account, command, changes);
        postBalance(account, depositTransaction, balanceData, false);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(depositTransaction.getId()) //
                .withEntityExternalId(depositTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult withdrawal(String accountId, JsonCommand command, boolean force) {
        currentTransactionDataValidator.validateWithdrawal(command);

        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        currentAccountWriteService.checkEnabled(account, true);
        if (force && account.isAllowForceTransaction()) {
            throw new GeneralPlatformDomainRuleException("error.msg.force.not.allowed", "Force withdrawal action is not allowed!");
        }

        CurrentAccountBalanceData balanceData = calculateAndCheckDebitBalance(account, force);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction withdrawalTransaction = currentTransactionAssembler.withdrawal(account, command, changes);
        postBalance(account, withdrawalTransaction, balanceData, force);

        // TODO: CURRENT! external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(withdrawalTransaction.getId()) //
                .withEntityExternalId(withdrawalTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult hold(String accountId, JsonCommand command) {
        currentTransactionDataValidator.validateHold(command);

        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        currentAccountWriteService.checkEnabled(account, true);
        CurrentAccountBalanceData balanceData = calculateAndCheckDebitBalance(account, false);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction holdTransaction = currentTransactionAssembler.hold(account, command, changes);
        postBalance(account, holdTransaction, balanceData, false);

        // TODO: CURRENT! external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(holdTransaction.getId()) //
                .withEntityExternalId(holdTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult release(String accountId, JsonCommand command) {
        currentTransactionDataValidator.validateRelease(command);
        final String transactionId = command.getTransactionId();

        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        currentAccountWriteService.checkEnabled(account, true);
        CurrentAccountBalanceData balanceData = calculateCreditBalance(account);

        final CurrentTransaction holdTransaction = currentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new CurrentTransactionNotFoundException(accountId, transactionId));
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction releaseTransaction = currentTransactionAssembler.release(account, holdTransaction, changes);
        postBalance(account, releaseTransaction, balanceData, false);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(releaseTransaction.getId()) //
                .withEntityExternalId(releaseTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @NotNull
    private CurrentAccountBalanceData calculateCreditBalance(@NotNull CurrentAccount account) {
        CurrentAccountAction action = CurrentAccountAction.forActionName(ThreadLocalContextUtil.getCommandAction());
        BalanceCalculationType balanceType = account.getBalanceCalculationType();
        String accountId = account.getId();
        return balanceType.isStrict(action)
                ? currentAccountBalanceWriteService.calculateBalance(accountId, balanceType, action,
                        currentAccountBalanceReadService.getBalanceCalculationTill())
                : new CurrentAccountBalanceData(null, accountId, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    @NotNull
    private CurrentAccountBalanceData calculateAndCheckDebitBalance(@NotNull CurrentAccount account, boolean force) {
        String accountId = account.getId();
        CurrentAccountAction action = CurrentAccountAction.forActionName(ThreadLocalContextUtil.getCommandAction());
        BalanceCalculationType balanceType = account.getBalanceCalculationType();
        CurrentAccountBalanceData balanceData = currentAccountBalanceWriteService.calculateBalance(accountId, balanceType, action,
                currentAccountBalanceReadService.getBalanceCalculationTill());
        if (balanceData == null) {
            balanceData = new CurrentAccountBalanceData(null, accountId, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }
        checkBalance(account, balanceData, force, true);
        return balanceData;
    }

    private void postBalance(@NotNull CurrentAccount account, @NotNull CurrentTransaction transaction,
            @NotNull CurrentAccountBalanceData balanceData, boolean force) {
        balanceData.applyTransaction(transaction);
        if (transaction.getTransactionType().isDebit()) {
            checkBalance(account, balanceData, force, false);
        }
        if (account.getBalanceCalculationType().isStrict()) {
            balanceData.setCalculatedTillTransactionId(transaction.getId());
            currentAccountBalanceWriteService.saveBalance(balanceData);
        }
    }

    private void checkBalance(@NotNull CurrentAccount account, @NotNull CurrentAccountBalanceData balanceData, boolean force, boolean pre) {
        // TODO CURRENT! add context information id, balance..
        BigDecimal accountBalance = balanceData.getAccountBalance();
        if (!(pre && force) && MathUtil.isLessThanZero(accountBalance) && !account.isAllowOverdraft()) {
            throw new GeneralPlatformDomainRuleException("error.msg.overdraft.not.allowed", "Overdraft is not allowed!");
        }
        if (force) {
            return;
        }
        BigDecimal availableBalance = account.getAvailableBalance(balanceData.getAvailableBalance(), true);
        if (MathUtil.isLessThanZero(availableBalance)) {
            throw new GeneralPlatformDomainRuleException("error.msg.available.balance.violated", "Violated available balance!");
        }
    }
}
