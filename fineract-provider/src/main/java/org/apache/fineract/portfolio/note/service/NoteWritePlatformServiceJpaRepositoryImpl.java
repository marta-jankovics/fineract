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
package org.apache.fineract.portfolio.note.service;

import com.google.common.base.Splitter;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepository;
import org.apache.fineract.portfolio.group.exception.GroupNotFoundException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.apache.fineract.portfolio.note.exception.NoteNotFoundException;
import org.apache.fineract.portfolio.note.exception.NoteResourceNotSupportedException;
import org.apache.fineract.portfolio.note.serialization.NoteCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;

@Slf4j
public class NoteWritePlatformServiceJpaRepositoryImpl implements NoteWritePlatformService {

    private final NoteRepository noteRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final GroupRepository groupRepository;
    private final LoanRepositoryWrapper loanRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final NoteCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final SavingsAccountRepository savingsAccountRepository;

    public NoteWritePlatformServiceJpaRepositoryImpl(final NoteRepository noteRepository, final ClientRepositoryWrapper clientRepository,
            final GroupRepository groupRepository, final LoanRepositoryWrapper loanRepository,
            final LoanTransactionRepository loanTransactionRepository, final NoteCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final SavingsAccountRepository savingsAccountRepository) {
        this.noteRepository = noteRepository;
        this.clientRepository = clientRepository;
        this.groupRepository = groupRepository;
        this.loanRepository = loanRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    @Override
    public CommandProcessingResult createNote(final JsonCommand command) {
        this.fromApiJsonDeserializer.validateNote(command.json());
        final NoteType type = getNoteTypeFromCommand(command);
        return saveNote(type, command, false);
    }

    @Override
    public CommandProcessingResult updateNote(final JsonCommand command) {
        this.fromApiJsonDeserializer.validateNote(command.json());
        final NoteType type = getNoteTypeFromCommand(command);
        return saveNote(type, command, true);
    }

    @Override
    public CommandProcessingResult deleteNote(final JsonCommand command) {
        final NoteType type = getNoteTypeFromCommand(command);
        Serializable resourceId = getResourceId(type, command);
        final Note noteForDelete = getExistingNote(type, getResource(type, resourceId), command.entityId());
        this.noteRepository.delete(noteForDelete);
        return new CommandProcessingResultBuilder() //
                .withCommandId(null) //
                .withEntityId(command.entityId()) //
                .build();
    }

    @Override
    public Long createEntityNote(@NotNull NoteType type, @NotNull Serializable resourceId, @NotNull JsonCommand command) {
        final String note = command.stringValueOfParameterNamed("note");
        return StringUtils.isNotBlank(note) ? saveNote(type, resourceId, command, false).getResourceId() : null;
    }

    private CommandProcessingResult saveNote(@NotNull NoteType type, @NotNull JsonCommand command, boolean update) {
        return saveNote(type, getResourceId(type, command), command, update);
    }

    private CommandProcessingResult saveNote(@NotNull NoteType type, @NotNull Serializable resourceId, @NotNull JsonCommand command,
            boolean update) {
        AbstractPersistableCustom resource = getResource(type, resourceId);
        Note note = update ? getExistingNote(type, resource, command.entityId()) : createNote(type, resource, command);
        Map<String, Object> changes = update ? note.update(command) : null;
        if (!update || !changes.isEmpty()) {
            note = noteRepository.saveAndFlush(note);
        }
        CommandProcessingResultBuilder result = new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(note.getId()) //
                .with(changes); //
        switch (type) {
            case CLIENT -> {
                Client client = (Client) resource;
                result.withClientId(client.getId()).withOfficeId(client.officeId());
            }
            case GROUP -> {
                final Group group = (Group) resource;
                result.withGroupId(group.getId()).withOfficeId(group.officeId());
            }
            case LOAN -> {
                final Loan loan = (Loan) resource;
                result.withLoanId(loan.getId()).withOfficeId(loan.getOfficeId());
            }
            case LOAN_TRANSACTION -> {
                Loan loan = ((LoanTransaction) resource).getLoan();
                result.withLoanId(loan.getId()).withOfficeId(loan.getOfficeId());
            }
            case SAVING_ACCOUNT -> {
                final SavingsAccount savingAccount = (SavingsAccount) resource;
                result.withSavingsId(savingAccount.getId()).withOfficeId(savingAccount.getClient().getOffice().getId());
            }
            default -> throw new NoteResourceNotSupportedException(command.getUrl());
        }
        return result.build();
    }

    private static Serializable getResourceId(@org.jetbrains.annotations.NotNull NoteType type,
            @org.jetbrains.annotations.NotNull JsonCommand command) {
        Serializable resourceId;
        switch (type) {
            case CLIENT -> {
                resourceId = command.getClientId();
            }
            case GROUP -> {
                resourceId = command.getGroupId();
            }
            case LOAN -> {
                resourceId = command.getLoanId();
            }
            case LOAN_TRANSACTION -> {
                resourceId = command.subentityId();
            }
            case SAVING_ACCOUNT -> {
                resourceId = command.getSavingsId();
            }
            default -> throw new NoteResourceNotSupportedException(command.getUrl());
        }
        return resourceId;
    }

    private AbstractPersistableCustom getResource(@NotNull NoteType type, @NotNull Serializable resourceId) {
        switch (type) {
            case CLIENT -> {
                return clientRepository.findOneWithNotFoundDetection((Long) resourceId);
            }
            case GROUP -> {
                final Long groupId = (Long) resourceId;
                return groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
            }
            case LOAN -> {
                return loanRepository.findOneWithNotFoundDetection((Long) resourceId);
            }
            case LOAN_TRANSACTION -> {
                final Long loanTransactionId = (Long) resourceId;
                return loanTransactionRepository.findById(loanTransactionId)
                        .orElseThrow(() -> new LoanTransactionNotFoundException(loanTransactionId));
            }
            case SAVING_ACCOUNT -> {
                final Long savinsAccountId = (Long) resourceId;
                return savingsAccountRepository.findById(savinsAccountId)
                        .orElseThrow(() -> new SavingsAccountNotFoundException(savinsAccountId));
            }
            default -> throw new NoteResourceNotSupportedException(type.getApiUrl());
        }
    }

    private Note getExistingNote(@NotNull NoteType type, @NotNull AbstractPersistableCustom resource, @NotNull Long noteId) {
        Note note = null;
        switch (type) {
            case CLIENT -> {
                note = noteRepository.findByClientAndId((Client) resource, noteId);
            }
            case GROUP -> {
                note = noteRepository.findByGroupAndId((Group) resource, noteId);
            }
            case LOAN -> {
                note = noteRepository.findByLoanAndId((Loan) resource, noteId);
            }
            case LOAN_TRANSACTION -> {
                note = noteRepository.findByLoanTransactionAndId((LoanTransaction) resource, noteId);
            }
            case SAVING_ACCOUNT -> {
                note = noteRepository.findBySavingsAccountAndId((SavingsAccount) resource, noteId);
            }
            default -> throw new NoteResourceNotSupportedException(type.getApiUrl());
        }
        if (note == null) {
            throw new NoteNotFoundException(noteId, resource.getId(), type.name().toLowerCase());
        }
        return note;
    }

    private Note createNote(@NotNull NoteType type, @NotNull AbstractPersistableCustom resource, @NotNull JsonCommand command) {
        final String noteS = command.stringValueOfParameterNamed("note");
        switch (type) {
            case CLIENT -> {
                return Note.clientNote((Client) resource, noteS);
            }
            case GROUP -> {
                return Note.groupNote((Group) resource, noteS);
            }
            case LOAN -> {
                return Note.loanNote((Loan) resource, noteS);
            }
            case LOAN_TRANSACTION -> {
                LoanTransaction loanTransaction = (LoanTransaction) resource;
                return Note.loanTransactionNote(loanTransaction.getLoan(), loanTransaction, noteS);
            }
            case SAVING_ACCOUNT -> {
                return Note.savingNote((SavingsAccount) resource, noteS);
            }
            default -> throw new NoteResourceNotSupportedException(type.getApiUrl());
        }
    }

    @NotNull
    private NoteType getNoteTypeFromCommand(JsonCommand command) {
        String url = command.getUrl();
        List<String> pathParams = Splitter.on('/').splitToList(url.startsWith("/") ? url.substring(1) : url);
        NoteType noteType;
        if (pathParams.isEmpty() || (noteType = NoteType.fromApiUrl(pathParams.get(0))) == null) {
            throw new NoteResourceNotSupportedException(url);
        }
        return noteType;
    }
}
