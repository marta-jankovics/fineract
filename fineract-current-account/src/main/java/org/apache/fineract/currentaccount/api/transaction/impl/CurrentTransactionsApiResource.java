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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.COMMAND;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.COMMAND_PARAM_FORCE;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME;

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
import org.apache.fineract.currentaccount.mapper.transaction.CurrentTransactionResponseDataMapper;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.transaction.CurrentTransactionResolver;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
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

@Path("/v1/current-accounts")
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
    private final CurrentTransactionResponseDataMapper currentTransactionResponseDataMapper;
    private final AdvancedQueryService advancedQueryService;
    private final DefaultToApiJsonSerializer<JsonObject> toApiJsonSerializer;

    @GET
    @Path("{accountIdentifier}/transactions/template")
    @Operation(operationId = "templateCurrentTransaction", summary = "Retrieve Current Transaction Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions/template\n\n")
    @Override
    public CurrentTransactionTemplateResponseData template(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTemplate(CurrentAccountResolver.resolveDefault(accountIdentifier));
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/transactions/template")
    @Operation(operationId = "templateCurrentTransaction", summary = "Retrieve Current Transaction Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions/template\n\n")
    @Override
    public CurrentTransactionTemplateResponseData template(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTemplate(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null));
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions/template")
    @Operation(operationId = "templateCurrentTransaction", summary = "Retrieve Current Transaction Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions/template\n\n")
    @Override
    public CurrentTransactionTemplateResponseData template(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService
                .retrieveTemplate(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier));
    }

    @GET
    @Path("{accountIdentifier}/transactions")
    @Operation(operationId = "retrieveAllCurrentTransactions", summary = "List current transactions/accounts", description = "Lists current transactions/accounts\n\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions\n\n")
    @Override
    public Page<CurrentTransactionResponseData> retrieveAll(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @Pagination @SortDefault.SortDefaults({ @SortDefault(sort = "transactionDate"), @SortDefault(sort = "createdDate"),
                    @SortDefault(sort = "id") }) @Parameter(hidden = true) Pageable pageable) {
        return retrieveAll(CurrentAccountResolver.resolveDefault(accountIdentifier), pageable);
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/transactions")
    @Operation(operationId = "retrieveAllCurrentTransactions", summary = "List current transactions/accounts", description = "Lists current transactions/accounts\n\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions\n\n")
    @Override
    public Page<CurrentTransactionResponseData> retrieveAll(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @Pagination @SortDefault.SortDefaults({ @SortDefault(sort = "transactionDate"), @SortDefault(sort = "createdDate"),
                    @SortDefault(sort = "id") }) @Parameter(hidden = true) Pageable pageable) {
        return retrieveAll(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null), pageable);
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions")
    @Operation(operationId = "retrieveAllCurrentTransactions", summary = "List current transactions/accounts", description = "Lists current transactions/accounts\n\n"
            + "Example Requests:\n\n" + "current-accounts/1/transactions\n\n")
    @Override
    public Page<CurrentTransactionResponseData> retrieveAll(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier,
            @Pagination @SortDefault.SortDefaults({ @SortDefault(sort = "transactionDate"), @SortDefault(sort = "createdDate"),
                    @SortDefault(sort = "id") }) @Parameter(hidden = true) Pageable pageable) {
        return retrieveAll(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier), pageable);
    }

    @GET
    @Path("{accountIdentifier}/transactions/{transactionIdentifier}")
    @Operation(operationId = "retrieveOneCurrentTransaction", summary = "Retrieve a current transaction/account", description = "Retrieves a current transaction/account\n\n"
            + "Example Requests :\n\n" + "current-accounts/1/transactions/1")
    @Override
    public CurrentTransactionResponseData retrieveOne(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier) {
        return retrieveOne(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier));
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/transactions/{transactionIdType}/{transactionIdentifier}")
    @Operation(operationId = "retrieveOneCurrentTransactionByIdentifier", summary = "Retrieve a current transaction/account", description = "Retrieves a current transaction/account\n\n"
            + "Example Requests :\n\n" + "current-accounts/external-id/ExternalId1/transactions/external-id/ExternalId2")
    @Override
    public CurrentTransactionResponseData retrieveOne(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("transactionIdType") @Parameter(description = "transactionIdType", required = true) final String transactionIdType,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier) {
        return retrieveOne(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier));
    }

    @GET
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions/{transactionIdType}/{transactionIdentifier}")
    @Operation(operationId = "retrieveOneCurrentTransactionByIdentifier", summary = "Retrieve a current transaction/account", description = "Retrieves a current transaction/account\n\n"
            + "Example Requests :\n\n" + "current-accounts/external-id/ExternalId1/S/transactions/external-id/ExternalId2")
    @Override
    public CurrentTransactionResponseData retrieveOne(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier,
            @PathParam("transactionIdType") @Parameter(description = "transactionIdType", required = true) final String transactionIdType,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier) {
        return retrieveOne(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier));
    }

    @POST
    @Path("{accountIdentifier}/transactions")
    @Operation(operationId = "applyCurrentTransaction", summary = "Deposit/Withdrawal/Hold Amount transaction API", description = "Deposit/Withdrawal/Hold Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n" + "current-accounts/1/transactions/?command=deposit\n\n"
            + "Accepted command = deposit, withdrawal, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
    @Override
    public CommandProcessingResult transaction(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @QueryParam(COMMAND) final String command, @QueryParam(COMMAND_PARAM_FORCE) final Boolean force,
            @Parameter(hidden = true) final String requestJson) {
        return handleTransaction(CurrentAccountResolver.resolveDefault(accountIdentifier), command, force, requestJson);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/transactions")
    @Operation(operationId = "applyCurrentTransactionByIdentifier", summary = "Deposit/Withdrawal/Hold Amount transaction API", description = "Deposit/Withdrawal/Hold Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n" + "current-accounts/external-id/ExternalId1/transactions/?command=deposit\n\n"
            + "Accepted command = deposit, withdrawal, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
    @Override
    public CommandProcessingResult transaction(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @QueryParam(COMMAND) final String command, @QueryParam(COMMAND_PARAM_FORCE) final Boolean force,
            @Parameter(hidden = true) final String requestJson) {
        return handleTransaction(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null), command, force, requestJson);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions")
    @Operation(operationId = "applyCurrentTransactionBySubIdentifier", summary = "Deposit/Withdrawal/Hold Amount transaction API", description = "Deposit/Withdrawal/Hold Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n" + "current-accounts/external-id/ExternalId1/S/transactions/?command=deposit\n\n"
            + "Accepted command = deposit, withdrawal, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
    @Override
    public CommandProcessingResult transaction(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier,
            @QueryParam(COMMAND) final String command, @QueryParam(COMMAND_PARAM_FORCE) final Boolean force,
            @Parameter(hidden = true) final String requestJson) {
        return handleTransaction(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier), command, force,
                requestJson);
    }

    @POST
    @Path("{accountIdentifier}/transactions/{transactionIdentifier}")
    @Operation(operationId = "adjustCurrentTransaction", summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n"
            + "current-accounts/{accountIdentifier}/transactions/{transactionIdentifier}?command=release\n\n"
            + "Accepted command = release")
    @Override
    public CommandProcessingResult action(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier,
            @QueryParam(COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleAction(CurrentAccountResolver.resolveDefault(accountIdentifier),
                CurrentTransactionResolver.resolveDefault(transactionIdentifier), commandParam, requestJson);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/transactions/{transactionIdType}/{transactionIdentifier}")
    @Operation(operationId = "adjustCurrentTransactionByIdentifier", summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n"
            + "current-accounts/external-id/ExternalId1/transactions/external-id/ExternalId2?command=release\n\n"
            + "Accepted command = release")
    @Override
    public CommandProcessingResult action(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("transactionIdType") @Parameter(description = "transactionIdType", required = true) final String transactionIdType,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier,
            @QueryParam(COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleAction(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), commandParam, requestJson);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions/{transactionIdType}/{transactionIdentifier}")
    @Operation(operationId = "adjustCurrentTransactionBySubIdentifier", summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n"
            + "Example Requests:\n\n" + "\n"
            + "current-accounts/external-id/ExternalId1/S/transactions/external-id/ExternalId2?command=release\n\n"
            + "Accepted command = release")
    @Override
    public CommandProcessingResult action(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier,
            @PathParam("transactionIdType") @Parameter(description = "transactionIdType", required = true) final String transactionIdType,
            @PathParam("transactionIdentifier") @Parameter(description = "transactionIdentifier", required = true) final String transactionIdentifier,
            @QueryParam(COMMAND) final String commandParam, @Parameter(hidden = true) final String requestJson) {
        return handleAction(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier),
                CurrentTransactionResolver.resolve(transactionIdType, transactionIdentifier), commandParam, requestJson);
    }

    @POST
    @Path("transactions/query")
    @Operation(operationId = "advancedQueryCurrentTransaction", summary = "Advanced search Current Account Transactions", description = "Example Requests:\n\n"
            + "current-accounts/transactions/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public String advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(null, queryRequest);
    }

    @POST
    @Path("{accountIdentifier}/transactions/query")
    @Operation(operationId = "advancedQueryCurrentTransactionById", summary = "Advanced search Current Account Transactions /account", description = "Example Requests:\n\n"
            + "current-accounts/1/transactions/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public String advancedQuery(
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier") final String accountIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(CurrentAccountResolver.resolveDefault(accountIdentifier), queryRequest);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/transactions/query")
    @Operation(operationId = "advancedQueryCurrentTransactionByIdentifier", summary = "Advanced search Current Account Transactions /account", description = "Example Requests:\n\n"
            + "current-accounts/external-id/ExternalId1/transactions/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public String advancedQuery(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, null), queryRequest);
    }

    @POST
    @Path("{accountIdType}/{accountIdentifier}/{accountSubIdentifier}/transactions/query")
    @Operation(operationId = "advancedQueryCurrentTransactionBySubIdentifier", summary = "Advanced search Current Account Transactions /account", description = "Example Requests:\n\n"
            + "current-accounts/external-id/ExternalId1/S/transactions/query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = List.class))) })
    @Override
    public String advancedQuery(
            @PathParam("accountIdType") @Parameter(description = "accountIdType", required = true) final String accountIdType,
            @PathParam("accountIdentifier") @Parameter(description = "accountIdentifier", required = true) final String accountIdentifier,
            @PathParam("accountSubIdentifier") @Parameter(description = "accountSubIdentifier", required = true) final String accountSubIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, @Context final UriInfo uriInfo) {
        return query(CurrentAccountResolver.resolve(accountIdType, accountIdentifier, accountSubIdentifier), queryRequest);
    }

    private CurrentTransactionResponseData retrieveOne(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return currentTransactionResponseDataMapper.map(currentTransactionReadService.retrieve(accountResolver, transactionResolver));
    }

    private String query(CurrentAccountResolver accountResolver, PagedLocalRequest<AdvancedQueryRequest> queryRequest) {
        context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        List<ColumnFilterData> addFilters = null;
        if (accountResolver != null) {
            addFilters = List.of(ColumnFilterData.eq("account_id", getResolvedAccountId(accountResolver)));
        }
        Page<JsonObject> result = advancedQueryService.query(EntityTables.CURRENT_TRANSACTION, queryRequest, addFilters);
        return toApiJsonSerializer.serializePretty(true, result);
    }

    @NotNull
    private CommandProcessingResult handleTransaction(@NotNull CurrentAccountResolver accountResolver, String commandParam,
            Boolean forceParam, String requestJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(requestJson);

        CommandProcessingResult result = null;
        String accountIdentifier = getResolvedAccountId(accountResolver);
        if (is(commandParam, CurrentAccountApiConstants.COMMAND_DEPOSIT)) {
            final CommandWrapper commandRequest = builder.currentAccountDeposit(accountIdentifier).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_WITHDRAWAL)) {
            boolean force = Boolean.TRUE.equals(forceParam);
            final CommandWrapper commandRequest = builder.currentAccountWithdrawal(accountIdentifier, force).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_HOLD)) {
            final CommandWrapper commandRequest = builder.currentAccountHold(accountIdentifier).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException(COMMAND, commandParam, CurrentAccountApiConstants.COMMAND_DEPOSIT,
                    CurrentAccountApiConstants.COMMAND_WITHDRAWAL, CurrentAccountApiConstants.COMMAND_HOLD);
        }
        return result;
    }

    private CommandProcessingResult handleAction(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver, String commandParam, String requestJson) {
        String jsonApiRequest = requestJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        String accountIdentifier = getResolvedAccountId(accountResolver);
        String transactionIdentifier = getResolvedTransactionId(accountResolver, transactionResolver);
        if (is(commandParam, CurrentAccountApiConstants.COMMAND_RELEASE)) {
            final CommandWrapper commandRequest = builder.currentTransactionRelease(accountIdentifier, transactionIdentifier).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        if (result == null) {
            throw new UnrecognizedQueryParamException(COMMAND, commandParam, CurrentAccountApiConstants.COMMAND_RELEASE);
        }
        return result;
    }

    private String getResolvedAccountId(@NotNull CurrentAccountResolver accountResolver) {
        return currentAccountReadService.retrieveId(accountResolver);
    }

    private String getResolvedTransactionId(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver) {
        return currentTransactionReadService.retrieveId(accountResolver, transactionResolver);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    private Page<CurrentTransactionResponseData> retrieveAll(@NotNull CurrentAccountResolver accountResolver, Pageable pageable) {
        this.context.authenticatedUser().validateHasReadPermission(CURRENT_TRANSACTION_RESOURCE_NAME);
        return currentTransactionResponseDataMapper.map(this.currentTransactionReadService.retrieveAll(accountResolver, pageable));
    }
}
