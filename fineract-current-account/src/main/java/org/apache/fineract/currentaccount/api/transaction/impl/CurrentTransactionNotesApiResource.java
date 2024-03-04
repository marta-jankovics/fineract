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
package org.apache.fineract.currentaccount.api.transaction.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_IDENTIFIER_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_ID_TYPE_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_ID_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_SUB_IDENTIFIER_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACCOUNT_SUB_IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_TRANSACTION_NOTE_ENTITY_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_IDENTIFIER_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_ID_TYPE_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.TRANSACTION_ID_TYPE_PARAM;
import static org.apache.fineract.portfolio.note.domain.NoteType.CURRENT_TRANSACTION;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.api.transaction.CurrentTransactionNotesApi;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.currentaccount.service.transaction.CurrentTransactionResolver;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.service.NoteReadPlatformService;
import org.springframework.stereotype.Component;

@Path("/v1/current-accounts")
@Component
@Tag(name = "Notes", description = "Notes API allows to enter notes for supported resources.")
@RequiredArgsConstructor
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class CurrentTransactionNotesApiResource implements CurrentTransactionNotesApi {

    private final PlatformSecurityContext context;
    private final CurrentTransactionReadService transactionReadService;
    private final NoteReadPlatformService noteReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;

    @GET
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve a notes/transaction", description = "Retrieves a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve notes/transaction by alternative id", description = "Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve notes/transaction by alternative id\", description = \"Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve a notes/transaction", description = "Retrieves a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve notes/transaction by alternative id", description = "Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentTransactionNotes", summary = "Retrieve notes/transaction by alternative id\", description = \"Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier) {
        return retrieveNotes(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier));
    }

    @GET
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve a note/account", description = "Retrieves a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public NoteData retrieveNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve note/account by alternative id", description = "Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve note/account by alternative id\", description = \"Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @GET
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve a note/account", description = "Retrieves a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public NoteData retrieveNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve note/account by alternative id", description = "Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    @GET
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentTransactionNote", summary = "Retrieve note/account by alternative id\", description = \"Retrieves a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    @POST
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create a note/account", description = "Creates a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @Override
    public CommandProcessingResult createNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), requestJson);
    }

    @POST
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create note/account by alternative id", description = "Creates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), requestJson);
    }

    @POST
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create note/account by alternative id\", description = \"Creates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), requestJson);
    }

    @POST
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create a note/account", description = "Creates a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @Override
    public CommandProcessingResult createNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), requestJson);
    }

    @POST
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create note/account by alternative id", description = "Creates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), requestJson);
    }

    @POST
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentTransactionNote", summary = "Create note/account by alternative id\", description = \"Creates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), requestJson);
    }

    @PUT
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update a note/account", description = "Updates a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId, requestJson);
    }

    @PUT
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update note/account by alternative id", description = "Updates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId, requestJson);
    }

    @PUT
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update note/account by alternative id\", description = \"Updates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId, requestJson);
    }

    @PUT
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update a note/account", description = "Updates a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId, requestJson);
    }

    @PUT
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update note/account by alternative id", description = "Updates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId, requestJson);
    }

    @PUT
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentTransactionNote", summary = "Update note/account by alternative id\", description = \"Updates a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId, requestJson);
    }

    @DELETE
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete a note/account", description = "Deletes a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @DELETE
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete note/account by alternative id", description = "Deletes a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @DELETE
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete note/account by alternative id\", description = \"Deletes a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), noteId);
    }

    @DELETE
    @Path(ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX
            + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete a note/account", description = "Deletes a current notes/transaction\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdentifier(
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    @DELETE
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/transactions/" + TRANSACTION_ID_TYPE_API_REGEX + "/"
            + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete note/account by alternative id", description = "Deletes a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    @DELETE
    @Path(ACCOUNT_ID_TYPE_API_REGEX + "/" + ACCOUNT_IDENTIFIER_API_REGEX + "/" + ACCOUNT_SUB_IDENTIFIER_API_REGEX + "/transactions/"
            + TRANSACTION_ID_TYPE_API_REGEX + "/" + TRANSACTION_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentTransactionNote", summary = "Delete note/account by alternative id\", description = \"Deletes a current notes/transaction by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ACCOUNT_ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String accountIdType,
            @PathParam(ACCOUNT_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String accountIdentifier,
            @PathParam(ACCOUNT_SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String accountSubIdentifier,
            @PathParam(TRANSACTION_ID_TYPE_PARAM) @Parameter(description = "Identifier type of the transaction", example = "id | external-id", required = true) final String transactionIdType,
            @PathParam(TRANSACTION_IDENTIFIER_PARAM) @Parameter(description = "Identifier of the transaction", required = true) final String transactionIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), noteId);
    }

    private List<NoteData> retrieveNotes(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver) {
        String transactionId = getResolvedTransactionId(accountResolver, transactionResolver);
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_NOTE_ENTITY_NAME);
        return noteReadPlatformService.retrieveNotesByResource(transactionId, CURRENT_TRANSACTION);
    }

    private NoteData retrieveNote(@NotNull CurrentAccountResolver accountResolver, @NotNull CurrentTransactionResolver transactionResolver,
            @NotNull Long noteId) {
        String transactionId = getResolvedTransactionId(accountResolver, transactionResolver);
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_NOTE_ENTITY_NAME);
        return noteReadPlatformService.retrieveNote(noteId, transactionId, CURRENT_TRANSACTION);
    }

    private CommandProcessingResult createNote(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver, String requestJson) {
        String transactionId = getResolvedTransactionId(accountResolver, transactionResolver);
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(transactionId)
                .withEntityName(CURRENT_TRANSACTION_NOTE_ENTITY_NAME).build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createNote(details, CURRENT_TRANSACTION.getApiUrl(), transactionId).withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult updateNote(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver, @NotNull Long noteId, String requestJson) {
        String transactionId = getResolvedTransactionId(accountResolver, transactionResolver);
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(transactionId)
                .withEntityName(CURRENT_TRANSACTION_NOTE_ENTITY_NAME).build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .updateNote(details, CURRENT_TRANSACTION.getApiUrl(), transactionId, noteId).withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult deleteNote(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver, @NotNull Long noteId) {
        String transactionId = getResolvedTransactionId(accountResolver, transactionResolver);
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(transactionId)
                .withEntityName(CURRENT_TRANSACTION_NOTE_ENTITY_NAME).build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .deleteNote(details, CURRENT_TRANSACTION.getApiUrl(), transactionId, noteId).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private String getResolvedTransactionId(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver) {
        return transactionReadService.retrieveId(accountResolver, transactionResolver);
    }
}
