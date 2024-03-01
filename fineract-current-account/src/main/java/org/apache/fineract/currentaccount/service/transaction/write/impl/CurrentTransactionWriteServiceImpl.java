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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.TRANSACTION_AMOUNT_HOLD;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.TRANSACTION_AMOUNT_RELEASE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.TRANSACTION_DEPOSIT;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.TRANSACTION_WITHDRAWAL;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionWriteServiceImpl implements CurrentTransactionWriteService {

    private final CurrentTransactionDataValidator transactionDataValidator;
    private final CurrentTransactionAssembler transactionAssembler;
    private final CurrentTransactionRepository transactionRepository;
    private final CurrentAccountRepository accountRepository;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult deposit(String accountId, JsonCommand command) {
        transactionDataValidator.validateDeposit(command);
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(TRANSACTION_DEPOSIT);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction depositTransaction = transactionAssembler.deposit(account, command, changes);

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
        transactionDataValidator.validateWithdrawal(command);
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(TRANSACTION_WITHDRAWAL);
        if (force && !account.isAllowForceTransaction()) {
            throw new GeneralPlatformDomainRuleException("error.msg.force.not.allowed", "Force withdrawal action is not allowed!");
        }

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction withdrawalTransaction = transactionAssembler.withdrawal(account, command, changes, force);

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
        transactionDataValidator.validateHold(command);
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(TRANSACTION_AMOUNT_HOLD);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction holdTransaction = transactionAssembler.hold(account, command, changes);

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
        transactionDataValidator.validateRelease(command);
        final String transactionId = command.getTransactionId();
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(TRANSACTION_AMOUNT_RELEASE);

        final CurrentTransaction holdTransaction = transactionRepository.findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("current.transaction",
                        "Current transaction with id: %s and account id: %s", transactionId, accountId));

        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction releaseTransaction = transactionAssembler.release(account, holdTransaction, command, changes);

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
}
