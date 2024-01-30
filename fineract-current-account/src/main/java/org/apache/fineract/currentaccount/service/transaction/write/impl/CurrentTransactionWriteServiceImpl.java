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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceParamName;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.exception.transaction.CurrentTransactionNotFoundException;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionWriteServiceImpl implements CurrentTransactionWriteService {

    private final CurrentTransactionDataValidator currentTransactionDataValidator;
    private final CurrentTransactionAssembler currentTransactionAssembler;
    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentTransactionRepository currentTransactionRepository;
    // TODO: use service eventually
    private final ClientRepository clientRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult deposit(String accountId, JsonCommand command) {
        currentTransactionDataValidator.validateDeposit(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction depositTransaction = currentTransactionAssembler.deposit(account, command, changes);

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
    public CommandProcessingResult withdrawal(String accountId, JsonCommand command) {
        currentTransactionDataValidator.validateWithdrawal(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction withdrawalTransaction = currentTransactionAssembler.withdrawal(account, command, changes);
        boolean enforce = command.booleanPrimitiveValueOfParameterNamed(enforceParamName);
        testBalance(account, withdrawalTransaction, enforce);

        // TODO: accounting and external event emitting
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
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction holdTransaction = currentTransactionAssembler.hold(account, command, changes);
        boolean enforce = command.booleanPrimitiveValueOfParameterNamed(enforceParamName);
        testBalance(account, holdTransaction, enforce);

        // TODO: accounting and external event emitting
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
        final CurrentTransaction holdTransaction = currentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new CurrentTransactionNotFoundException(accountId, transactionId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction releaseTransaction = currentTransactionAssembler.release(account, holdTransaction, changes);

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

    private void testBalance(CurrentAccount account, CurrentTransaction debitTransaction, boolean enforce) {
        if (!enforce) {
            final CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(account.getId());
            BigDecimal newAvailableBalance = currentAccountBalanceData.getAccountBalance()
                    .subtract(currentAccountBalanceData.getHoldAmount()).subtract(debitTransaction.getTransactionAmount());
            if (newAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
                if (account.isAllowOverdraft() && newAvailableBalance.negate().compareTo(account.getOverdraftLimit()) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.overdraft.limit.reached", "Reached overdraft limit!");
                } else {
                    throw new GeneralPlatformDomainRuleException("error.msg.overdraft.not.allowed", "Overdraft is not allowed!");
                }
            } else if (account.getMinimumRequiredBalance() != null
                    && account.getMinimumRequiredBalance().compareTo(newAvailableBalance) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.minimum.required.balance.violated",
                        "Violated minimum required balance!");
            }
        }
    }

    private void checkClientActive(final CurrentAccount account) {
        final Client client = clientRepository.findById(account.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(account.getClientId()));
        if (client != null) {
            if (client.isNotActive()) {
                throw new ClientNotActiveException(client.getId());
            }
        }
    }
}
