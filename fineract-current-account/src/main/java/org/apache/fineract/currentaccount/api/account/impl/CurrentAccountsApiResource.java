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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ACTIVATE_ACTION;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CANCEL_ACTION;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CLOSE_ACTION;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.SUB_IDENTIFIER_PARAM;

import com.google.gson.JsonObject;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.api.account.CurrentAccountsApi;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.account.IdentifiersResponseData;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountResponseDataMapper;
import org.apache.fineract.currentaccount.service.account.CurrentAccountIdTypeResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.search.data.AdvancedQueryRequest;
import org.apache.fineract.portfolio.search.data.ColumnFilterData;
import org.apache.fineract.portfolio.search.service.AdvancedQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Component;

@Path("/v2/current-accounts")
@Component
@Tag(name = "Current Accounts", description = "Current accounts are instances of a particular current product created for an individual. An application process around the creation of accounts is also supported.")
@RequiredArgsConstructor
public class CurrentAccountsApiResource implements CurrentAccountsApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;
    private final CurrentAccountReadService currentAccountReadService;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final CurrentAccountResponseDataMapper currentAccountResponseDataMapper;
    private final AdvancedQueryService advancedQueryService;

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "templateCurrentAccount", summary = "Retrieve Current Account Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "current-accounts/template\n\n")
    @Override
    public CurrentAccountTemplateResponseData template() {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_ENTITY_NAME);
        return currentAccountReadService.retrieveTemplate();
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveAllCurrentAccounts", summary = "List current applications/accounts", description = "Lists current applications/accounts\n\n"
            + "Example Requests:\n\n" + "currentaccounts")
    @Override
    public Page<CurrentAccountResponseData> retrieveAll(
            @Pagination @SortDefault("createdDate") @Parameter(hidden = true) Pageable pageable) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_ENTITY_NAME);
        return currentAccountResponseDataMapper.map(currentAccountReadService.retrieveAll(pageable),
                currentAccountBalanceReadService::getBalance);
    }

    @GET
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveOneCurrentAccount", summary = "Retrieve a current application/account", description = "Retrieves a current application/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1")
    @Override
    public CurrentAccountResponseData retrieveOne(@PathParam("accountId") @Parameter(description = "accountId") final String accountId) {
        return retrieveOne(CurrentAccountIdTypeResolver.resolveDefault(), accountId, null);
    }

    @GET
    @Path("{idType}/{identifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveOneCurrentAccountByIdentifier", summary = "Retrieve a current application/account by alternative id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1")
    @Override
    public CurrentAccountResponseData retrieveOne(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier) {
        return retrieveOne(CurrentAccountIdTypeResolver.resolve(idType), identifier, null);
    }

    @GET
    @Path("{idType}/{identifier}/{subIdentifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveOneCurrentAccountBySubIdentifier", summary = "Retrieve a current application/account by alternative id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1")
    @Override
    public CurrentAccountResponseData retrieveOne(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier) {
        return retrieveOne(CurrentAccountIdTypeResolver.resolve(idType), identifier, subIdentifier);
    }

    @GET
    @Path("{accountId}/identifiers")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveIdentifiersCurrentAccount", summary = "Retrieve a identifiers/account", description = "Retrieves a current identifiers/account\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/1/identifiers")
    @Override
    public IdentifiersResponseData retrieveIdentifiers(
            @PathParam("accountId") @Parameter(description = "accountId") final String accountId) {
        return retrieveIdentifiers(CurrentAccountIdTypeResolver.resolveDefault(), accountId, null);
    }

    @GET
    @Path("{idType}/{identifier}/identifiers")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveIdentifiersCurrentAccountByIdentifier", summary = "Retrieve identifiers/account by alternative id", description = "Retrieves a current identifiers/account by identifier\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/identifiers")
    @Override
    public IdentifiersResponseData retrieveIdentifiers(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier) {
        return retrieveIdentifiers(CurrentAccountIdTypeResolver.resolve(idType), identifier, null);
    }

    @GET
    @Path("{idType}/{identifier}/{subIdentifier}/identifiers")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "retrieveIdentifiersCurrentAccountBySubIdentifier", summary = "Retrieve identifiers/account by alternative id\", description = \"Retrieves a current identifiers/account by identifier\n\n"
            + "Example Requests :\n" + "\n" + "current-accounts/external-id/ExternalId1/S/identifiers")
    @Override
    public IdentifiersResponseData retrieveIdentifiers(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier) {
        return retrieveIdentifiers(CurrentAccountIdTypeResolver.resolve(idType), identifier, subIdentifier);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "createCurrentAccount", summary = "Create new current application", description = "Creates new current application\n\n"
            + "Mandatory Fields: clientId, productId, accountNumber, submittedOnDate\n\n"
            + "Optional Fields: externalId, submittedOnDate\n\n"
            + "Inherited from Product (if not provided): minimumRequiredBalance, allowOverdraft, overdraftLimit\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountSubmitRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountCommandResponse.class))) })
    @Override
    public CommandProcessingResult create(@Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentAccount().withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @POST
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "actionCurrentAccount", summary = "Cancel current application | Activate a current account | Close a current account", description = "Cancel current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an submitted current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountActionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult action(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String requestJson) {
        return handleCommands(accountId, commandParam, requestJson);
    }

    @POST
    @Path("{idType}/{identifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "actionCurrentAccountByIdentifier", summary = "Cancel current application | Activate a current account | Close a current account", description = "Cancel current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an submitted current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountActionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult action(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String requestJson) {
        return handleCommands(getResolvedAccountId(idType, identifier, null), commandParam, requestJson);
    }

    @POST
    @Path("{idType}/{identifier}/{subIdentifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "actionCurrentAccountBySubIdentifier", summary = "Cancel current application | Activate a current account | Close a current account", description = "Cancel current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an submitted current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountActionRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult action(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String requestJson) {
        return handleCommands(getResolvedAccountId(idType, identifier, subIdentifier), commandParam, requestJson);
    }

    @PUT
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "updateCurrentAccount", summary = "Modify a current application", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted' state. Once the application is activate, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountUpdateRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult update(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentAccount(accountId, requestJson);
    }

    @PUT
    @Path("{idType}/{identifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "updateCurrentAccountByIdentifier", summary = "Modify a current application", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted' state. Once the application is activate, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountUpdateRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult update(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentAccount(getResolvedAccountId(idType, identifier, null), requestJson);
    }

    @PUT
    @Path("{idType}/{identifier}/{subIdentifier}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "updateCurrentAccountBySubIdentifier", summary = "Modify a current application", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted' state. Once the application is activate, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountUpdateRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.CurrentAccountUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult update(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentAccount(getResolvedAccountId(idType, identifier, subIdentifier), requestJson);
    }

    @POST
    @Path("query")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "advancedQueryCurrentAccount", summary = "Advanced search Current Accounts", description = "Example Requests:\n\n"
            + "current-accounts/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(null, queryRequest);
    }

    @POST
    @Path("{accountId}/query")
    @Operation(operationId = "advancedQueryCurrentAccountById", summary = "Advanced search Current Account", description = "Example Requests:\n\n"
            + "current-accounts/1/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(accountId, queryRequest);
    }

    @POST
    @Path("{idType}/{identifier}/query")
    @Operation(operationId = "advancedQueryCurrentAccountByIdentifier", summary = "Advanced search Current Account", description = "Example Requests:\n\n"
            + "current-accounts/external-id/ExternalId1/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(getResolvedAccountId(idType, identifier, null), queryRequest);
    }

    @POST
    @Path("{idType}/{identifier}/{subIdentifier}/query")
    @Operation(operationId = "advancedQueryCurrentAccountBySubIdentifier", summary = "Advanced search Current Account", description = "Example Requests:\n\n"
            + "current-accounts/external-id/ExternalId1/S/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(getResolvedAccountId(idType, identifier, subIdentifier), queryRequest);
    }

    private CurrentAccountResponseData retrieveOne(@NotNull CurrentAccountIdTypeResolver idType, String identifier, String subIdentifier) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_ENTITY_NAME);
        currentAccountReadService.retrieveByIdTypeAndIdentifier(idType, identifier, subIdentifier);
        CurrentAccountData accountData = currentAccountReadService.retrieveByIdTypeAndIdentifier(idType, identifier, subIdentifier);
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(accountData.getId());
        return currentAccountResponseDataMapper.map(accountData, currentAccountBalanceData);
    }

    private IdentifiersResponseData retrieveIdentifiers(@NotNull CurrentAccountIdTypeResolver idType, String identifier,
            String subIdentifier) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_IDENTIFIER_ENTITY_NAME);
        return currentAccountReadService.retrieveIdentifiersByIdTypeAndIdentifier(idType, identifier, subIdentifier);
    }

    private CommandProcessingResult updateCurrentAccount(String accountId, String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentAccount(accountId).withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult handleCommands(String accountId, String commandParam, String requestJson) {
        String jsonApiRequest = requestJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, CurrentAccountApiConstants.CANCEL_ACTION)) {
            final CommandWrapper commandRequest = builder.cancelCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.ACTIVATE_ACTION)) {
            final CommandWrapper commandRequest = builder.activateCurrentAccount(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.CLOSE_ACTION)) {
            final CommandWrapper commandRequest = builder.closeCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        }
        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam, CANCEL_ACTION, ACTIVATE_ACTION, CLOSE_ACTION);
        }
        return result;
    }

    private Page<JsonObject> query(String accountId, PagedLocalRequest<AdvancedQueryRequest> queryRequest) {
        context.authenticatedUser().validateHasReadPermission(CURRENT_ACCOUNT_RESOURCE_NAME);
        List<ColumnFilterData> addFilters = null;
        if (accountId != null) {
            addFilters = List.of(ColumnFilterData.eq("id", accountId));
        }
        return advancedQueryService.query(EntityTables.CURRENT, queryRequest, addFilters);
    }

    private String getResolvedAccountId(String idType, String identifier, String subIdentifier) {
        return currentAccountReadService.retrieveIdByIdTypeAndIdentifier(CurrentAccountIdTypeResolver.resolve(idType), identifier,
                subIdentifier);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
