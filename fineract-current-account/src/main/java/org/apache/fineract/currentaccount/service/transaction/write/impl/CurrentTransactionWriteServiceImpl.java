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

import jakarta.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.assembler.account.transaction.CurrentAccountTransactionAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.exception.account.CurrentAccountNotFoundException;
import org.apache.fineract.currentaccount.exception.transaction.CurrentTransactionNotFoundException;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionWriteServiceImpl implements CurrentTransactionWriteService {

    private final CurrentTransactionDataValidator currentAccountTransactionDataValidator;
    private final CurrentAccountTransactionAssembler currentAccountTransactionAssembler;
    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentTransactionRepository currentAccountTransactionRepository;
    // TODO: use service eventually
    private final ClientRepository clientRepository;

    @Transactional
    @Override
    public CommandProcessingResult deposit(Long accountId, JsonCommand command) {
        this.currentAccountTransactionDataValidator.validateDeposit(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction depositTransaction = this.currentAccountTransactionAssembler.deposit(account, command, changes);
        persistTransaction(command, depositTransaction);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new SavingsDepositBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(depositTransaction.getId()) //
                .withEntityExternalId(depositTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    private void persistTransaction(JsonCommand command, CurrentTransaction transaction) {
        try {
            currentAccountTransactionRepository.saveAndFlush(transaction);
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult withdraw(Long accountId, JsonCommand command) {
        this.currentAccountTransactionDataValidator.validateWithdraw(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction withdrawTransaction = this.currentAccountTransactionAssembler.withdraw(account, command, changes);
        persistTransaction(command, withdrawTransaction);

        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new SavingsDepositBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(withdrawTransaction.getId()) //
                .withEntityExternalId(withdrawTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult hold(Long accountId, JsonCommand command) {
        this.currentAccountTransactionDataValidator.validateHold(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction holdTransaction = this.currentAccountTransactionAssembler.holdAmount(account, command, changes);
        persistTransaction(command, holdTransaction);
        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new SavingsDepositBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(holdTransaction.getId()) //
                .withEntityExternalId(holdTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult release(Long accountId, JsonCommand command) {
        this.currentAccountTransactionDataValidator.validateRelease(command);
        final Long transactionId = Long.valueOf(command.getTransactionId());
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        final CurrentTransaction holdTransaction = currentAccountTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new CurrentTransactionNotFoundException(accountId, transactionId));
        checkClientActive(account);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final CurrentTransaction releaseTransaction = this.currentAccountTransactionAssembler.releaseAmount(account, holdTransaction,
                changes);
        persistTransaction(command, releaseTransaction);
        // TODO: accounting and external event emitting
        // postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds, isAccountTransfer,
        // backdatedTxnsAllowedTill);
        // businessEventNotifierService.notifyPostBusinessEvent(new SavingsDepositBusinessEvent(deposit));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(releaseTransaction.getId()) //
                .withEntityExternalId(releaseTransaction.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
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
