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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.activateAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.cancelAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.closeAction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.api.account.CurrentAccountsApi;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Component;

@Path("/v1/current-accounts")
@Component
@Tag(name = "Current Accounts", description = "Current accounts are instances of a particular current product created for an individual. An application process around the creation of accounts is also supported.")
@RequiredArgsConstructor
public class CurrentAccountsApiResource implements CurrentAccountsApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;
    private final CurrentAccountReadService currentAccountReadService;

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Current Account Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "currentaccounts/template\n\n")
    @Override
    public CurrentAccountTemplateResponseData template() {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveTemplate();
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List current applications/accounts", description = "Lists current applications/accounts\n\n"
            + "Example Requests:\n" + "\n" + "currentaccounts\n" + "\n" + "\n" + "currentaccounts")
    @Override
    public Page<CurrentAccountResponseData> retrieveAll(@Pagination @SortDefault("createdDate") Pageable pageable) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveAll(pageable);
    }

    @GET
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account", description = "Retrieves a current application/account\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/1")
    @Override
    public CurrentAccountResponseData retrieveOne(@PathParam("accountId") @Parameter(description = "accountId") final String accountId) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveById(accountId);
    }

    @GET
    @Path("{idType}/{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account by alternative id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/external-id/ExternalId1")
    @Override
    public CurrentAccountResponseData retrieveOne(
            @PathParam("idType") @Parameter(description = "idType", required = true) final String idType,
            @PathParam("id") @Parameter(description = "id", required = true) final String id) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveByIdTypeAndId(idType, id, null);
    }

    @GET
    @Path("{idType}/{id}/{subId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account by alternative id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/external-id/ExternalId1")
    @Override
    public CurrentAccountResponseData retrieveOne(
            @PathParam("idType") @Parameter(description = "idType", required = true) final String idType,
            @PathParam("id") @Parameter(description = "id", required = true) final String id,
            @PathParam("subId") @Parameter(description = "subId", required = true) final String subId) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveByIdTypeAndId(idType, id, subId);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Submit new current application", description = "Submits new current application\n\n"
            + "Mandatory Fields: clientId, productId, accountNumber, submittedOnDate\n\n"
            + "Optional Fields: externalId, submittedOnDate\n\n"
            + "Inherited from Product (if not provided): enforceminimumRequiredBalance, minimumRequiredBalance, allowOverdraft, overdraftLimit\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountSubmitRequest.class)))
    @Override
    public CommandProcessingResult submitApplication(@Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentAccount().withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @POST
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Cancel current application | Activate a current account | Close a current account", description = "Cancel current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an submitted current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountActionRequest.class)))
    @Override
    public CommandProcessingResult handleCommands(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String requestJson) {
        return handleCommands(accountId, null, requestJson, commandParam);
    }

    @PUT
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a current application", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted' state. Once the application is activate, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountActionRequest.class)))
    @Override
    public CommandProcessingResult update(@PathParam("accountId") @Parameter(description = "accountId") final String accountId,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentAccount(accountId, null, requestJson);
    }

    private CommandProcessingResult updateCurrentAccount(String accountId, String externalId, String requestJson) {
        ExternalId accountExternalId = ExternalIdFactory.produce(externalId);
        accountId = getResolvedAccountId(accountId, accountExternalId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentAccount(accountId).withJson(requestJson).build();

        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult handleCommands(String accountId, String externalId, String requestJson, String commandParam) {
        ExternalId accountExternalId = ExternalIdFactory.produce(externalId);
        accountId = getResolvedAccountId(accountId, accountExternalId);

        String jsonApiRequest = requestJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, CurrentAccountApiConstants.cancelAction)) {
            final CommandWrapper commandRequest = builder.cancelCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.activateAction)) {
            final CommandWrapper commandRequest = builder.currentAccountActivation(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.closeAction)) {
            final CommandWrapper commandRequest = builder.closeCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam, cancelAction, activateAction, closeAction);
        }

        return result;
    }

    private String getResolvedAccountId(String accountId, ExternalId accountExternalId) {
        String resolvedAccountId = accountId;
        if (resolvedAccountId == null) {
            accountExternalId.throwExceptionIfEmpty();
            resolvedAccountId = currentAccountReadService.retrieveAccountIdByExternalId(accountExternalId);
        }
        return resolvedAccountId;
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
