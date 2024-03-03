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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_ENTITY_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_TRANSACTION_ENTITY_NAME;

import com.google.common.base.Splitter;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
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
    private final CurrentAccountRepository currentAccountRepository;
    private final CurrentTransactionRepository currentTransactionRepository;

    public NoteWritePlatformServiceJpaRepositoryImpl(final NoteRepository noteRepository, final ClientRepositoryWrapper clientRepository,
            final GroupRepository groupRepository, final LoanRepositoryWrapper loanRepository,
            final LoanTransactionRepository loanTransactionRepository, final NoteCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final SavingsAccountRepository savingsAccountRepository, CurrentAccountRepository currentAccountRepository,
            CurrentTransactionRepository currentTransactionRepository) {
        this.noteRepository = noteRepository;
        this.clientRepository = clientRepository;
        this.groupRepository = groupRepository;
        this.loanRepository = loanRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.savingsAccountRepository = savingsAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.currentTransactionRepository = currentTransactionRepository;
    }

    @Override
    public CommandProcessingResult createNote(final JsonCommand command) {
        this.fromApiJsonDeserializer.validateNote(command.json());
        final NoteType type = getNoteTypeFromCommand(command);
        switch (type) {
            case CLIENT: {
                return createClientNote(command);
            }
            case GROUP: {
                return createGroupNote(command);
            }
            case LOAN: {
                return createLoanNote(command);
            }
            case LOAN_TRANSACTION: {
                return createLoanTransactionNote(command);
            }
            case SAVING_ACCOUNT: {
                return createSavingAccountNote(command);
            }
            case CURRENT_ACCOUNT:
            case CURRENT_TRANSACTION: {
                return saveEntityNote(type, command.getResourceIdentifier(), null, command);
            }
            default:
                throw new NoteResourceNotSupportedException(command.getUrl());
        }
    }

    @Override
    public CommandProcessingResult updateNote(final JsonCommand command) {
        this.fromApiJsonDeserializer.validateNote(command.json());
        final NoteType type = getNoteTypeFromCommand(command);
        switch (type) {
            case CLIENT: {
                return updateClientNote(command);
            }
            case GROUP: {
                return updateGroupNote(command);
            }
            case LOAN: {
                return updateLoanNote(command);
            }
            case LOAN_TRANSACTION: {
                return updateLoanTransactionNote(command);
            }
            case SAVING_ACCOUNT: {
                return updateSavingAccountNote(command);
            }
            case CURRENT_ACCOUNT:
            case CURRENT_TRANSACTION: {
                return saveEntityNote(type, command.getResourceIdentifier(), command.entityId(), command);
            }
            default:
                throw new NoteResourceNotSupportedException(command.getUrl());
        }
    }

    @Override
    public CommandProcessingResult deleteNote(final JsonCommand command) {
        final Note noteForDelete = getNoteForDelete(command);
        this.noteRepository.delete(noteForDelete);
        return new CommandProcessingResultBuilder() //
                .withCommandId(null) //
                .withEntityId(command.entityId()) //
                .build();
    }

    @Override
    public void createAndPersistClientNote(final Long clientId, final JsonCommand command) {
        final String note = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(note)) {
            createClientNote(clientId, command);
        }
    }

    @Override
    public Long createEntityNote(@NotNull NoteType type, @NotNull String entityIdentifier, @NotNull JsonCommand command) {
        final String note = command.stringValueOfParameterNamed("note");
        return StringUtils.isNotBlank(note) ? saveEntityNote(type, entityIdentifier, null, command).getResourceId() : null;
    }

    private CommandProcessingResult createClientNote(final JsonCommand command) {
        final Long clientId = command.getClientId();
        return createClientNote(clientId, command);
    }

    private CommandProcessingResult createClientNote(Long clientId, JsonCommand command) {
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        if (client == null) {
            throw new ClientNotFoundException(clientId);
        }
        final Note newNote = Note.clientNoteFromJson(client, command);
        this.noteRepository.saveAndFlush(newNote);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newNote.getId()) //
                .withClientId(client.getId()) //
                .withOfficeId(client.officeId()) //
                .build();
    }

    private CommandProcessingResult createGroupNote(final JsonCommand command) {

        final Long resourceId = command.getGroupId();

        final Group group = this.groupRepository.findById(resourceId).orElseThrow(() -> new GroupNotFoundException(resourceId));
        final Note newNote = Note.groupNoteFromJson(group, command);

        this.noteRepository.saveAndFlush(newNote);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newNote.getId()) //
                .withGroupId(group.getId()) //
                .withOfficeId(group.officeId()) //
                .build();
    }

    private CommandProcessingResult createLoanNote(final JsonCommand command) {

        final Long resourceId = command.getLoanId();

        final Loan loan = this.loanRepository.findOneWithNotFoundDetection(resourceId);
        final String note = command.stringValueOfParameterNamed("note");
        final Note newNote = Note.loanNote(loan, note);

        this.noteRepository.saveAndFlush(newNote);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newNote.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withLoanId(loan.getId()) //
                .build();
    }

    private CommandProcessingResult createLoanTransactionNote(final JsonCommand command) {

        final Long resourceId = command.subentityId();

        final LoanTransaction loanTransaction = this.loanTransactionRepository.findById(resourceId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(resourceId));

        final Loan loan = loanTransaction.getLoan();

        final String note = command.stringValueOfParameterNamed("note");
        final Note newNote = Note.loanTransactionNote(loan, loanTransaction, note);

        this.noteRepository.saveAndFlush(newNote);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newNote.getId()) //
                .withOfficeId(loan.getOfficeId())//
                .withLoanId(loan.getId())// Loan can be associated
                .build();
    }

    private CommandProcessingResult createSavingAccountNote(final JsonCommand command) {
        final Long resourceId = command.getSavingsId();
        final SavingsAccount savingAccount = this.savingsAccountRepository.findById(resourceId)
                .orElseThrow(() -> new SavingsAccountNotFoundException(resourceId));

        final String note = command.stringValueOfParameterNamed("note");
        final Note newNote = Note.savingNote(savingAccount, note);

        this.noteRepository.saveAndFlush(newNote);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(newNote.getId()) //
                .withOfficeId(savingAccount.getClient().getOffice().getId()) //
                .withSavingsId(savingAccount.getId()) //
                .build();
    }

    private CommandProcessingResult saveEntityNote(@NotNull NoteType type, @NotNull String entityIdentifier, Long noteId,
            @NotNull JsonCommand command) {
        switch (type) {
            case CURRENT_ACCOUNT -> currentAccountRepository.findById(entityIdentifier)
                    .orElseThrow(() -> new ResourceNotFoundException(CURRENT_ACCOUNT_ENTITY_NAME, entityIdentifier));
            case CURRENT_TRANSACTION -> currentTransactionRepository.findById(entityIdentifier)
                    .orElseThrow(() -> new ResourceNotFoundException(CURRENT_TRANSACTION_ENTITY_NAME, entityIdentifier));
            default -> throw new NoteResourceNotSupportedException(type.name());
        }
        final String noteS = command.stringValueOfParameterNamed("note");
        Note note;
        Map<String, Object> changes = null;
        if (noteId == null) {
            note = Note.entityNote(type, entityIdentifier, noteS);
            this.noteRepository.saveAndFlush(note);
        } else {
            note = this.noteRepository.getByNoteTypeIdAndEntityIdentifierAndId(type.getValue(), entityIdentifier, noteId);
            if (note == null) {
                throw new ResourceNotFoundException(type.name(), String.valueOf(noteId));
            }
            changes = note.update(command);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(note.getId()) //
                .withResourceIdentifier(entityIdentifier) //
                .with(changes) //
                .build();
    }

    private CommandProcessingResult updateClientNote(final JsonCommand command) {
        final Long resourceId = command.getClientId();
        final Long noteId = command.entityId();

        final NoteType type = NoteType.CLIENT;

        final Client client = this.clientRepository.findOneWithNotFoundDetection(resourceId);

        final Note noteForUpdate = this.noteRepository.findByClientAndId(client, noteId);
        if (noteForUpdate == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }

        final Map<String, Object> changes = noteForUpdate.update(command);

        if (!changes.isEmpty()) {
            this.noteRepository.saveAndFlush(noteForUpdate);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(noteForUpdate.getId()) //
                .withClientId(client.getId()) //
                .withOfficeId(client.officeId()) //
                .with(changes) //
                .build();
    }

    private CommandProcessingResult updateGroupNote(final JsonCommand command) {
        final Long resourceId = command.getGroupId();
        final Long noteId = command.entityId();

        final NoteType type = NoteType.GROUP;

        final Group group = this.groupRepository.findById(resourceId).orElseThrow(() -> new GroupNotFoundException(resourceId));

        final Note noteForUpdate = this.noteRepository.findByGroupAndId(group, noteId);

        if (noteForUpdate == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }

        final Map<String, Object> changes = noteForUpdate.update(command);

        if (!changes.isEmpty()) {
            this.noteRepository.saveAndFlush(noteForUpdate);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(noteForUpdate.getId()) //
                .withGroupId(group.getId()) //
                .withOfficeId(group.officeId()) //
                .with(changes).build();
    }

    private CommandProcessingResult updateLoanNote(final JsonCommand command) {
        final Long resourceId = command.getLoanId();
        final Long noteId = command.entityId();

        final NoteType type = NoteType.LOAN;

        final Loan loan = this.loanRepository.findOneWithNotFoundDetection(resourceId);
        final Note noteForUpdate = this.noteRepository.findByLoanAndId(loan, noteId);
        if (noteForUpdate == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }

        final Map<String, Object> changes = noteForUpdate.update(command);

        if (!changes.isEmpty()) {
            this.noteRepository.saveAndFlush(noteForUpdate);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(noteForUpdate.getId())
                .withLoanId(loan.getId()).withOfficeId(loan.getOfficeId()).with(changes).build();
    }

    private CommandProcessingResult updateLoanTransactionNote(final JsonCommand command) {
        final Long resourceId = command.subentityId();
        final Long noteId = command.entityId();

        final NoteType type = NoteType.LOAN_TRANSACTION;

        final LoanTransaction loanTransaction = this.loanTransactionRepository.findById(resourceId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(resourceId));
        final Loan loan = loanTransaction.getLoan();

        final Note noteForUpdate = this.noteRepository.findByLoanTransactionAndId(loanTransaction, noteId);

        if (noteForUpdate == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }

        final Map<String, Object> changes = noteForUpdate.update(command);

        if (!changes.isEmpty()) {
            this.noteRepository.saveAndFlush(noteForUpdate);
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(noteForUpdate.getId())
                .withLoanId(loan.getId()).withOfficeId(loan.getOfficeId()).with(changes).build();
    }

    private CommandProcessingResult updateSavingAccountNote(final JsonCommand command) {
        final Long resourceId = command.getSavingsId();
        final Long noteId = command.entityId();
        final NoteType type = NoteType.SAVING_ACCOUNT;
        final SavingsAccount savingAccount = this.savingsAccountRepository.findById(resourceId)
                .orElseThrow(() -> new SavingsAccountNotFoundException(resourceId));

        final Note noteForUpdate = this.noteRepository.findBySavingsAccountAndId(savingAccount, noteId);
        if (noteForUpdate == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }
        final Map<String, Object> changes = noteForUpdate.update(command);
        if (!changes.isEmpty()) {
            this.noteRepository.saveAndFlush(noteForUpdate);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(noteForUpdate.getId()) //
                .withOfficeId(savingAccount.getClient().getOffice().getId()) //
                .withSavingsId(savingAccount.getId()) //
                .with(changes) //
                .build();
    }

    private Note getNoteForDelete(final JsonCommand command) {
        final NoteType type = getNoteTypeFromCommand(command);
        final Long noteId = command.entityId();
        Long resourceId = null;
        Note note = null;
        switch (type) {
            case CLIENT: {
                resourceId = command.getClientId();
                final Client client = this.clientRepository.findOneWithNotFoundDetection(resourceId);
                note = this.noteRepository.findByClientAndId(client, noteId);
            }
            break;
            case GROUP: {
                final Long groupId = command.getGroupId();
                resourceId = groupId;
                Group group = this.groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
                note = this.noteRepository.findByGroupAndId(group, noteId);
            }
            break;
            case LOAN: {
                resourceId = command.getLoanId();
                final Loan loan = this.loanRepository.findOneWithNotFoundDetection(resourceId);
                note = this.noteRepository.findByLoanAndId(loan, noteId);
            }
            break;
            case LOAN_TRANSACTION: {
                resourceId = command.subentityId();
                final Long loanTransactionId = resourceId;
                final LoanTransaction loanTransaction = this.loanTransactionRepository.findById(loanTransactionId)
                        .orElseThrow(() -> new LoanTransactionNotFoundException(loanTransactionId));
                note = this.noteRepository.findByLoanTransactionAndId(loanTransaction, noteId);
            }
            break;
            case SAVING_ACCOUNT: {
                final Long savinsAccountId = command.getSavingsId();
                final SavingsAccount savingAccount = this.savingsAccountRepository.findById(savinsAccountId)
                        .orElseThrow(() -> new SavingsAccountNotFoundException(savinsAccountId));

                note = this.noteRepository.findBySavingsAccountAndId(savingAccount, noteId);
            }
            break;
            case SHARE_ACCOUNT:
                log.error("TODO Implement getNoteForDelete for SHARE_ACCOUNT");
            break;
            case SAVINGS_TRANSACTION:
                log.error("TODO Implement getNoteForDelete for SAVINGS_TRANSACTION");
            break;
            case CURRENT_ACCOUNT:
            case CURRENT_TRANSACTION:
                final String entityIdentifier = command.getResourceIdentifier();
                note = noteRepository.getByNoteTypeIdAndEntityIdentifierAndId(type.getValue(), entityIdentifier, noteId);
            break;
        }
        if (note == null) {
            throw new NoteNotFoundException(noteId, resourceId, type.name().toLowerCase());
        }
        return note;
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
