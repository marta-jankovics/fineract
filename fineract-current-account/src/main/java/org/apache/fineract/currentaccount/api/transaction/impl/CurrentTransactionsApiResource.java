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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME;
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
import org.apache.fineract.currentaccount.api.transaction.CurrentTransactionApi;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionIdType;
import org.apache.fineract.currentaccount.service.IdTypeResolver;
import org.apache.fineract.currentaccount.service.account.CurrentAccountIdTypeResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryRequest;
import org.apache.fineract.infrastructure.dataqueries.data.ColumnFilterData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.service.AdvancedQueryService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Path("/v2/current-accounts")
@Component
@Tag(name = "Current Transactions")
@RequiredArgsConstructor
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class CurrentTransactionsApiResource implements CurrentTransactionApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CurrentTransactionReadService currentTransactionReadService;
    private final CurrentAccountReadService currentAccountReadService;
    private final AdvancedQueryService advancedQueryService;

    @GET
    @Path("{accountId}/transactions/template")
    @Override
    public CurrentTransactionTemplateResponseData template(@PathParam("accountId") final String accountId) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTemplate(accountId);
    }

    @GET
    @Path("{accountId}/transactions")
    @Override
    public Page<CurrentTransactionResponseData> retrieveAll(@PathParam("accountId") final String accountId,
            @Pagination @Parameter(hidden = true) Pageable pageable) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTransactionsByAccountId(accountId, pageable);
    }

    @GET
    @Path("{accountId}/transactions/{transactionId}")
    @Override
    public CurrentTransactionResponseData retrieveOne(@PathParam("accountId") final String accountId,
            @PathParam("transactionId") final String transactionId) {
        return retrieveOne(accountId, CurrentTransactionIdType.ID, transactionId);
    }

    @GET
    @Path("{accIdType}/{accIdentifier}/transactions/{idType}/{identifier}")
    @Override
    public CurrentTransactionResponseData retrieveOne(
            @PathParam("accIdType") @Parameter(description = "accIdType", required = true) final String accIdType,
            @PathParam("accIdentifier") @Parameter(description = "accIdentifier", required = true) final String accIdentifier,
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier) {
        return retrieveOne(getResolvedAccountId(accIdType, accIdentifier, null),
                IdTypeResolver.resolve(CurrentTransactionIdType.class, idType), identifier);
    }

    @GET
    @Path("{accIdType}/{accIdentifier}/{accSubIdentifier}/transactions/{idType}/{identifier}")
    @Override
    public CurrentTransactionResponseData retrieveOne(
            @PathParam("accIdType") @Parameter(description = "accIdType", required = true) final String accIdType,
            @PathParam("accIdentifier") @Parameter(description = "accIdentifier", required = true) final String accIdentifier,
            @PathParam("accSubIdentifier") @Parameter(description = "accSubIdentifier", required = true) final String accSubIdentifier,
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier) {
        return retrieveOne(getResolvedAccountId(accIdType, accIdentifier, accSubIdentifier),
                IdTypeResolver.resolve(CurrentTransactionIdType.class, idType), identifier);
    }

    @POST
    @Path("{accountId}/transactions")
    @Operation(summary = "Deposit/Withdrawal/Hold Amount transaction API", description = "Deposit/Withdrawal/Hold Amount transaction API\n\n"
            + "Example Requests:\n" + "\n" + "\n" + "currentaccounts/{accountId}/transactions/?command=deposit\n" + "\n"
            + "Accepted command = deposit, withdrawal, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
    @Override
    public CommandProcessingResult transaction(@PathParam("accountId") final String accountId,
            @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(requestJson);

        CommandProcessingResult result = null;
        if (is(commandParam, CurrentAccountApiConstants.COMMAND_DEPOSIT)) {
            final CommandWrapper commandRequest = builder.currentTransactionDeposit(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_WITHDRAWAL)) {
            final CommandWrapper commandRequest = builder.currentTransactionWithdrawal(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_HOLD)) {
            final CommandWrapper commandRequest = builder.currentTransactionHold(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException(CurrentAccountApiConstants.COMMAND, commandParam,
                    CurrentAccountApiConstants.COMMAND_DEPOSIT, CurrentAccountApiConstants.COMMAND_WITHDRAWAL,
                    CurrentAccountApiConstants.COMMAND_HOLD);
        }
        return result;
    }

    @POST
    @Path("{accountId}/transactions/{transactionId}")
    @Operation(summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n" + "Example Requests:\n" + "\n"
            + "\n" + "currentaccounts/{accountId}/transactions/{transactionId}?command=release\n" + "\n" + "Accepted command = release")
    @Override
    public CommandProcessingResult action(@PathParam("accountId") final String accountId,
            @PathParam("transactionId") final String transactionId,
            @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleCommands(accountId, null, transactionId, commandParam, requestJson);
    }

    @POST
    @Path("{accIdType}/{accIdentifier}/transactions/{idType}/{identifier}")
    @Operation(summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n" + "Example Requests:\n" + "\n"
            + "\n" + "currentaccounts/{accountId}/transactions/{transactionId}?command=release\n" + "\n" + "Accepted command = release")
    @Override
    public CommandProcessingResult action(
            @PathParam("accIdType") @Parameter(description = "accIdType", required = true) final String accIdType,
            @PathParam("accIdentifier") @Parameter(description = "accIdentifier", required = true) final String accIdentifier,
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleCommands(getResolvedAccountId(accIdType, accIdentifier, null), idType, identifier, commandParam, requestJson);
    }

    @POST
    @Path("{accIdType}/{accIdentifier}/{accSubIdentifier}/transactions/{idType}/{identifier}")
    @Operation(summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n" + "Example Requests:\n" + "\n"
            + "\n" + "currentaccounts/{accountId}/transactions/{transactionId}?command=release\n" + "\n" + "Accepted command = release")
    @Override
    public CommandProcessingResult action(
            @PathParam("accIdType") @Parameter(description = "accIdType", required = true) final String accIdType,
            @PathParam("accIdentifier") @Parameter(description = "accIdentifier", required = true) final String accIdentifier,
            @PathParam("accSubIdentifier") @Parameter(description = "accSubIdentifier", required = true) final String accSubIdentifier,
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleCommands(getResolvedAccountId(accIdType, accIdentifier, accSubIdentifier), idType, identifier, commandParam,
                requestJson);
    }

    @POST
    @Path("transactions/query")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Advanced search Current Account Transactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    public Page<JsonObject> advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(null, null, null, queryRequest);
    }

    @POST
    @Path("{accountId}/transactions/query")
    @Operation(summary = "Advanced search Current Account Transactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(null, accountId, null, queryRequest);
    }

    @POST
    @Path("{idType}/{identifier}/transactions/query")
    @Operation(summary = "Advanced search Current Account Transactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(idType, identifier, null, queryRequest);
    }

    @POST
    @Path("{idType}/{identifier}/{subIdentifier}/transactions/query")
    @Operation(summary = "Advanced search Current Account Transactions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public Page<JsonObject> advancedQuery(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier,
            @PathParam(SUB_IDENTIFIER_PARAM) @Parameter(description = SUB_IDENTIFIER_PARAM, required = true) final String subIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(idType, identifier, subIdentifier, queryRequest);
    }

    private CurrentTransactionResponseData retrieveOne(String accountId, @NotNull CurrentTransactionIdType idType, String identifier) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return currentTransactionReadService.retrieveByIdTypeAndIdentifier(accountId, idType, identifier);
    }

    private Page<JsonObject> query(String idType, String identifier, String subIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest) {
        context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        List<ColumnFilterData> addFilters = null;
        if (identifier != null) {
            String accountId = getResolvedAccountId(idType, identifier, subIdentifier);
            addFilters = List.of(ColumnFilterData.eq("account_id", accountId));
        }
        return advancedQueryService.query(EntityTables.CURRENT_TRANSACTION, queryRequest, addFilters);
    }

    private CommandProcessingResult handleCommands(String accountId, String idType, String identifier, String commandParam,
            String requestJson) {
        String transactionId = getResolvedTransactionId(idType, identifier);

        String jsonApiRequest = requestJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, CurrentAccountApiConstants.COMMAND_RELEASE)) {
            final CommandWrapper commandRequest = builder.currentTransactionRelease(accountId, transactionId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        if (result == null) {
            throw new UnrecognizedQueryParamException(CurrentAccountApiConstants.COMMAND, commandParam,
                    CurrentAccountApiConstants.COMMAND_RELEASE);
        }
        return result;
    }

    private String getResolvedAccountId(String idType, String identifier, String subIdentifier) {
        return currentAccountReadService.retrieveIdByIdTypeAndIdentifier(CurrentAccountIdTypeResolver.resolve(idType), identifier,
                subIdentifier);
    }

    private String getResolvedTransactionId(String idType, String identifier) {
        return currentTransactionReadService.retrieveIdByIdTypeAndIdentifier(IdTypeResolver.resolve(CurrentTransactionIdType.class, idType),
                identifier);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
