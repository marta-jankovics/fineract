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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.validator.account.CurrentAccountDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountWriteServiceImpl implements CurrentAccountWriteService {

    private final CurrentAccountDataValidator currentAccountDataValidator;
    private final CurrentAccountAssembler currentAccountAssembler;
    private final CurrentAccountRepository currentAccountRepository;
    // TODO: use service eventually
    private final ClientRepository clientRepository;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult submitApplication(final JsonCommand command) {
        currentAccountDataValidator.validateForSubmit(command);
        final CurrentAccount account = currentAccountAssembler.assemble(command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new CurrentAccountCreateBusinessEvent(account));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withClientId(account.getClientId()) //
                .withResourceIdentifier(account.getId()) //
                .withEntityExternalId(account.getExternalId()).withClientId(account.getClientId()) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult update(final String accountId, final JsonCommand command) {

        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        currentAccountDataValidator.validateForUpdate(command, account);
        checkClientActive(account);
        Map<String, Object> changes = currentAccountAssembler.update(account, command);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult cancelApplication(final String accountId, final JsonCommand command) {
        currentAccountDataValidator.validateCancellation(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.cancelApplication(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult activate(final String accountId, final JsonCommand command) {
        currentAccountDataValidator.validateActivation(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.activate(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult close(final String accountId, final JsonCommand command) {
        currentAccountDataValidator.validateClosing(command);
        final CurrentAccount account = currentAccountRepository.findById(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        checkClientActive(account);
        final Map<String, Object> changes = currentAccountAssembler.close(account, command);

        // TODO: Business event handling
        // businessEventNotifierService.notifyPostBusinessEvent(new
        // CurrentAccountRejectApplicationBusinessEvent(currentAccount));

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdentifier(account.getId()) //
                .withEntityExternalId(account.getExternalId()) //
                .withClientId(account.getClientId()) //
                .with(changes) //
                .build();
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
