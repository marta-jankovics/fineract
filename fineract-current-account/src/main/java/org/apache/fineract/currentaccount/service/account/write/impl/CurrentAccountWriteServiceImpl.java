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
package org.apache.fineract.currentaccount.service.account.write.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.exception.account.CurrentAccountNotFoundException;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.validator.account.CurrentAccountDataValidator;
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
public class CurrentAccountWriteServiceImpl implements CurrentAccountWriteService {

    private final CurrentAccountDataValidator currentAccountDataValidator;
    private final CurrentAccountAssembler currentAccountAssembler;
    private final CurrentAccountRepository currentAccountRepository;
    // TODO: use service eventually
    private final ClientRepository clientRepository;

    @Transactional
    @Override
    public CommandProcessingResult submitApplication(final JsonCommand command) {
        try {
            currentAccountDataValidator.validateForSubmit(command);
            final CurrentAccount account = currentAccountAssembler.assemble(command);
            currentAccountRepository.saveAndFlush(account);

            // TODO: Business event handling
            // businessEventNotifierService.notifyPostBusinessEvent(new CurrentAccountCreateBusinessEvent(account));

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withClientId(account.getClientId()) //
                    .withEntityUUID(account.getId()) //
                    .withEntityExternalId(account.getExternalId()).withClientId(account.getClientId()) //
                    .build();
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final Exception dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult modifyApplication(final UUID accountId, final JsonCommand command) {
        try {
            final CurrentAccount account = currentAccountRepository.findById(accountId)
                    .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
            currentAccountDataValidator.validateForUpdate(command, account);
            checkClientActive(account);
            Map<String, Object> changes = currentAccountAssembler.update(account, command);
            if (!changes.isEmpty()) {
                currentAccountRepository.saveAndFlush(account);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityUUID(account.getId()) //
                    .withEntityExternalId(account.getExternalId()) //
                    .withClientId(account.getClientId()) //
                    .with(changes) //
                    .build();
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.resourceResult(-1L);
        } catch (final Exception dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult cancelApplication(final UUID accountId, final JsonCommand command) {
        currentAccountDataValidator.validateCancellation(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.cancelApplication(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult activate(final UUID accountId, final JsonCommand command) {
        currentAccountDataValidator.validateActivation(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.activate(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult close(final UUID accountId, final JsonCommand command) {
        currentAccountDataValidator.validateClosing(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId)
                .orElseThrow(() -> new CurrentAccountNotFoundException(accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.close(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityUUID(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        String msgCode = "error.msg." + CURRENT_ACCOUNT_RESOURCE_NAME;
        String msg = "Unknown data integrity issue with current account.";
        String param = null;
        Object[] msgArgs;
        Throwable checkEx = realCause == null ? dve : realCause;
        if (checkEx.getMessage().contains("m_current_account_account_no_key")) {
            final String accountNo = command.stringValueOfParameterNamed("accountNo");
            msgCode += ".duplicate.accountNo";
            msg = "Current account with accountNo " + accountNo + " already exists";
            param = "accountNo";
            msgArgs = new Object[] { accountNo, dve };
        } else if (checkEx.getMessage().contains("m_current_account_external_id_key")) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            msgCode += ".duplicate.externalId";
            msg = "Current account with externalId " + externalId + " already exists";
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
