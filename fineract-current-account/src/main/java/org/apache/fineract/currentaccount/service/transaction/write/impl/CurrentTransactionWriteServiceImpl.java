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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.enforceParamName;
import org.apache.fineract.currentaccount.assembler.account.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.exception.account.CurrentAccountNotFoundException;
import org.apache.fineract.currentaccount.exception.transaction.CurrentTransactionNotFoundException;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.springframework.dao.DataAccessException;
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
    public CommandProcessingResult deposit(UUID accountId, JsonCommand command) {
        currentTransactionDataValidator.validateDeposit(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction depositTransaction = currentTransactionAssembler.deposit(account, command, changes);
        persistTransaction(command, depositTransaction);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(depositTransaction.getId()) //
                .withEntityExternalId(depositTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult withdraw(UUID accountId, JsonCommand command) {
        currentTransactionDataValidator.validateWithdraw(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction withdrawTransaction = currentTransactionAssembler.withdraw(account, command, changes);
        boolean enforce = command.booleanPrimitiveValueOfParameterNamed(enforceParamName);
        testBalance(account, withdrawTransaction, enforce);
        persistTransaction(command, withdrawTransaction);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(withdrawTransaction.getId()) //
                .withEntityExternalId(withdrawTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult hold(UUID accountId, JsonCommand command) {
        currentTransactionDataValidator.validateHold(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction holdTransaction = currentTransactionAssembler.hold(account, command, changes);
        boolean enforce = command.booleanPrimitiveValueOfParameterNamed(enforceParamName);
        testBalance(account, holdTransaction, enforce);

        persistTransaction(command, holdTransaction);
        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(holdTransaction.getId()) //
                .withEntityExternalId(holdTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult release(UUID accountId, JsonCommand command) {
        currentTransactionDataValidator.validateRelease(command);
        final UUID transactionId = command.getTransactionUUID();
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        final CurrentTransaction holdTransaction = currentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new CurrentTransactionNotFoundException(accountId, transactionId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction releaseTransaction = currentTransactionAssembler.release(account, holdTransaction,
                changes);
        persistTransaction(command, releaseTransaction);
        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentXXXBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(releaseTransaction.getId()) //
                .withEntityExternalId(releaseTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    private void testBalance(CurrentAccount account, CurrentTransaction debitTransaction, boolean enforce) {
        if(!enforce) {
            final CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(account.getId());
            BigDecimal newAvailableBalance = currentAccountBalanceData.getAvailableBalance().subtract(debitTransaction.getTransactionAmount());
            if (newAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
                if (account.isAllowOverdraft() && newAvailableBalance.negate().compareTo(account.getOverdraftLimit()) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.overdraft.limit.reached", "Reached overdraft limit!");
                } else {
                    throw new GeneralPlatformDomainRuleException("error.msg.overdraft.not.allowed", "Overdraft is not allowed!");
                }
            } else if (account.isEnforceMinRequiredBalance() && account.getMinRequiredBalance().compareTo(newAvailableBalance) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.minimum.required.balance.violated", "Violated minimum required balance!");
            }
        }
    }

    private void persistTransaction(JsonCommand command, CurrentTransaction transaction) {
        try {
            currentTransactionRepository.saveAndFlush(transaction);
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final Exception dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        String msgCode = "error.msg." + CurrentAccountApiConstants.CURRENT_ACCOUNT_TRANSACTION_RESOURCE_NAME;
        String msg = "Unknown data integrity issue with current account.";
        String param = null;
        Object[] msgArgs;
        Throwable checkEx = realCause == null ? dve : realCause;
        if (checkEx.getMessage().contains("m_current_account_transaction_external_id_key")) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            msgCode += ".duplicate.externalId";
            msg = "Current account transaction with externalId " + externalId + " already exists";
            param = "externalId";
            msgArgs = new Object[] { externalId, dve };
        } else {
            msgCode += ".unknown.data.integrity.issue";
            msgArgs = new Object[] { dve };
        }
        log.error("Error occurred.", dve);
        throw ErrorHandler.getMappable(dve, msgCode, msg, param, msgArgs);
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
