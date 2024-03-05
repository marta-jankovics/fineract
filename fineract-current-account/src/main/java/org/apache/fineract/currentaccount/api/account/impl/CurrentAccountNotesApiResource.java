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
package org.apache.fineract.currentaccount.api.account.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_NOTE_ENTITY_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIER_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SUB_IDENTIFIER_API_REGEX;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SUB_IDENTIFIER_PARAM;
import static org.apache.fineract.portfolio.note.domain.NoteType.CURRENT_ACCOUNT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.apache.fineract.currentaccount.api.account.CurrentAccountNotesApi;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
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
public class CurrentAccountNotesApiResource implements CurrentAccountNotesApi {

    private final PlatformSecurityContext context;
    private final CurrentAccountReadService accountReadService;
    private final NoteReadPlatformService noteReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;

    @GET
    @Path(IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentAccountNotes", summary = "Retrieve a notes/account", description = "Retrieves a current notes/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdentifier(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier) {
        return retrieveNotes(identifier);
    }

    @GET
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentAccountNotes", summary = "Retrieve notes/account by alternative id", description = "Retrieves a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier) {
        return retrieveNotes(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, null)));
    }

    @GET
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/" + SUB_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "retrieveCurrentAccountNotes", summary = "Retrieve notes/account by alternative id\", description = \"Retrieves a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @Override
    public List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String subIdentifier) {
        return retrieveNotes(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, subIdentifier)));
    }

    @GET
    @Path(IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentAccountNote", summary = "Retrieve a note/account", description = "Retrieves a current notes/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @Override
    public NoteData retrieveNoteByIdentifier(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(identifier, noteId);
    }

    @GET
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentAccountNote", summary = "Retrieve note/account by alternative id", description = "Retrieves a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, null)), noteId);
    }

    @GET
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/" + SUB_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "retrieveCurrentAccountNote", summary = "Retrieve note/account by alternative id\", description = \"Retrieves a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @Override
    public NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String subIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return retrieveNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, subIdentifier)), noteId);
    }

    @POST
    @Path(IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentAccountNote", summary = "Create a note/account", description = "Creates a current notes/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteCommandResponse.class))) })
    @Override
    public CommandProcessingResult createNoteByIdentifier(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(identifier, requestJson);
    }

    @POST
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentAccountNote", summary = "Create note/account by alternative id", description = "Creates a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteCommandResponse.class))) })
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, null)), requestJson);
    }

    @POST
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/" + SUB_IDENTIFIER_API_REGEX + "/notes")
    @Operation(operationId = "createCurrentAccountNote", summary = "Create note/account by alternative id\", description = \"Creates a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteCommandResponse.class))) })
    @Override
    public CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String subIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return createNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, subIdentifier)), requestJson);
    }

    @PUT
    @Path(IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentAccountNote", summary = "Update a note/account", description = "Updates a current notes/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult updateNoteByIdentifier(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(identifier, noteId, requestJson);
    }

    @PUT
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentAccountNote", summary = "Update note/account by alternative id", description = "Updates a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, null)), noteId, requestJson);
    }

    @PUT
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/" + SUB_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "updateCurrentAccountNote", summary = "Update note/account by alternative id\", description = \"Updates a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String subIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId, @Parameter(hidden = true) final String requestJson) {
        return updateNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, subIdentifier)), noteId, requestJson);
    }

    @DELETE
    @Path(IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentAccountNote", summary = "Delete a note/account", description = "Deletes a current notes/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/notes/20")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteDeleteCommandResponse.class))) })
    @Override
    public CommandProcessingResult deleteNoteByIdentifier(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(identifier, noteId);
    }

    @DELETE
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentAccountNote", summary = "Delete note/account by alternative id", description = "Deletes a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/notes/20")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteDeleteCommandResponse.class))) })
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, null)), noteId);
    }

    @DELETE
    @Path(ID_TYPE_API_REGEX + "/" + IDENTIFIER_API_REGEX + "/" + SUB_IDENTIFIER_API_REGEX + "/notes/{noteId}")
    @Operation(operationId = "deleteCurrentAccountNote", summary = "Delete note/account by alternative id\", description = \"Deletes a current notes/account by note\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/notes/20")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountNoteDeleteCommandResponse.class))) })
    @Override
    public CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = "Note type of the account", example = "id | external-id | account-number | msisdn | email | personal-id | business | device | account-id | iban | alias | bban", required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = "Identifier of the account", required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = "Sub-note of the account", required = true) final String subIdentifier,
            @PathParam("noteId") @Parameter(description = "noteId") final Long noteId) {
        return deleteNote(getResolvedAccountId(CurrentAccountResolver.resolve(idType, identifier, subIdentifier)), noteId);
    }

    private List<NoteData> retrieveNotes(@NotNull String accountId) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_NOTE_ENTITY_NAME);
        return noteReadPlatformService.retrieveNotesByResource(accountId, CURRENT_ACCOUNT);
    }

    private NoteData retrieveNote(@NotNull String accountId, @NotNull Long noteId) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_NOTE_ENTITY_NAME);
        return noteReadPlatformService.retrieveNote(noteId, accountId, CURRENT_ACCOUNT);
    }

    private CommandProcessingResult createNote(@NotNull String accountId, String requestJson) {
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(accountId).withEntityName(CURRENT_NOTE_ENTITY_NAME)
                .build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createNote(details, CURRENT_ACCOUNT.getApiUrl(), accountId)
                .withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult updateNote(@NotNull String accountId, @NotNull Long noteId, String requestJson) {
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(accountId).withEntityName(CURRENT_NOTE_ENTITY_NAME)
                .build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .updateNote(details, CURRENT_ACCOUNT.getApiUrl(), accountId, noteId).withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult deleteNote(@NotNull String accountId, @NotNull Long noteId) {
        CommandWrapper details = new CommandWrapperBuilder().withEntityIdentifier(accountId).withEntityName(CURRENT_NOTE_ENTITY_NAME)
                .build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .deleteNote(details, CURRENT_ACCOUNT.getApiUrl(), accountId, noteId).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private String getResolvedAccountId(@NotNull CurrentAccountResolver accountResolver) {
        return accountReadService.retrieveId(accountResolver);
    }
}
