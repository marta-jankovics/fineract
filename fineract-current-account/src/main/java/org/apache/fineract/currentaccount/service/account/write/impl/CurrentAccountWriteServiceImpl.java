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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.ACTIVATE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CANCEL;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.CLOSE;
import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.UPDATE;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.service.accounting.write.CurrentAccountAccountingWriteService;
import org.apache.fineract.currentaccount.validator.account.CurrentAccountDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountWriteServiceImpl implements CurrentAccountWriteService {

    private final CurrentAccountDataValidator accountDataValidator;
    private final CurrentAccountAssembler accountAssembler;
    private final CurrentAccountRepository accountRepository;
    // TODO: use service eventually
    private final CurrentAccountAccountingWriteService accountAccountingWriteService;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult submitApplication(@NotNull JsonCommand command) {
        accountDataValidator.validateForSubmit(command);
        final CurrentAccount account = accountAssembler.assemble(command);

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
    public CommandProcessingResult update(@NotNull String accountId, @NotNull JsonCommand command) {
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        accountDataValidator.validateForUpdate(command, account);
        account.checkEnabled(UPDATE);

        Map<String, Object> changes = accountAssembler.update(account, command);

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
    public CommandProcessingResult cancelApplication(@NotNull String accountId, @NotNull JsonCommand command) {
        accountDataValidator.validateCancellation(command);
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(CANCEL);

        final Map<String, Object> changes = accountAssembler.cancelApplication(account, command);

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
    public CommandProcessingResult activate(@NotNull String accountId, @NotNull JsonCommand command) {
        accountDataValidator.validateActivation(command);
        final CurrentAccount account = accountRepository.findById(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(ACTIVATE);

        final Map<String, Object> changes = accountAssembler.activate(account, command);

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
    public CommandProcessingResult close(@NotNull String accountId, @NotNull JsonCommand command) {
        accountDataValidator.validateClosing(command);
        final CurrentAccount account = accountRepository.findAccountByIdWithExclusiveLock(accountId).orElseThrow(
                () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        account.checkEnabled(CLOSE);

        final Map<String, Object> changes = accountAssembler.close(account, command);

        accountAccountingWriteService.createGLEntries(accountId, DateUtils.getOffsetDateTimeOfTenant().plusMinutes(1));
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
}
