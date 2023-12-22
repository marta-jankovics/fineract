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
package org.apache.fineract.currentaccount.api.account;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.activateAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.approveAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.closeAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.rejectAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.undoApprovalAction;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.withdrawnByApplicantAction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.PagedRequest;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Path("/v1/currentaccounts")
@Component
@Tag(name = "Current Account", description = "Current accounts are instances of a particular current product created for an individual. An application process around the creation of accounts is also supported.")
@RequiredArgsConstructor
public class CurrentAccountsApiResource implements CurrentAccountsApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;
    private final CurrentAccountReadService currentAccountReadService;

    @Override
    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Current Account Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "currentaccounts/template\n\n")
    public CurrentAccountTemplateResponseData template() {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveTemplate();
    }

    @Override
    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List current applications/accounts", description = "Lists current applications/accounts\n\n"
            + "Example Requests:\n" + "\n" + "currentaccounts\n" + "\n" + "\n" + "currentaccounts")
    public Page<CurrentAccountResponseData> retrieveAll(@Context final UriInfo uriInfo,
            @QueryParam("offset") @Parameter(description = "offset") final Long offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("page") @Parameter(description = "page") final Integer page,
            @QueryParam("size") @Parameter(description = "size") final Integer size,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveAll(PagedRequest.createFrom(offset, limit, page, size, sortOrder, orderBy));
    }

    @Override
    @GET
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account", description = "Retrieves a current application/account\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/1")
    public CurrentAccountResponseData retrieveOne(@PathParam("accountId") @Parameter(description = "accountId") final Long accountId) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveById(accountId);
    }

    @Override
    @GET
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account by external id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/external-id/ExternalId1")
    public CurrentAccountResponseData retrieveOne(@PathParam("externalId") @Parameter(description = "externalId") final String externalId) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_ACCOUNT_RESOURCE_NAME);
        return currentAccountReadService.retrieveByExternalId(ExternalIdFactory.produce(externalId));
    }

    @Override
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Submit new current application", description = "Submits new current application\n\n"
            + "Mandatory Fields: clientId, productId, accountNo, submittedOnDate\n\n" + "Optional Fields: externalId, submittedOnDate\n\n"
            + "Inherited from Product (if not provided): enforceMinRequiredBalance, minimumRequiredBalance, allowOverdraft, overdraftLimit\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountSubmitRequest.class)))
    public CommandProcessingResult submitApplication(@Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentAccount().withJson(apiRequestBodyAsJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @Override
    @POST
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Approve current application | Undo approval current application | Reject current application | Withdraw current application | Activate a current account | Close a current account", description = "Approve current application:\n\n"
            + "Approves current application so long as its in 'Submitted and pending approval' state.\n\n"
            + "Undo approval current application:\n\n"
            + "Will move 'approved' current application back to 'Submitted and pending approval' state.\n\n"
            + "Reject current application:\n\n"
            + "Rejects current application so long as its in 'Submitted and pending approval' state.\n\n"
            + "Withdraw current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted and pending approval' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an approved current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountActionRequest.class)))
    public CommandProcessingResult handleCommands(@PathParam("accountId") @Parameter(description = "accountId") final Long accountId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return handleCommands(accountId, null, apiRequestBodyAsJson, commandParam);
    }

    @Override
    @POST
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Approve current application | Undo approval current application | Reject current application | Withdraw current application | Activate a current account | Close a current account", description = "Approve current application:\n\n"
            + "Approves current application so long as its in 'Submitted and pending approval' state.\n\n"
            + "Undo approval current application:\n\n"
            + "Will move 'approved' current application back to 'Submitted and pending approval' state.\n\n" + "Assign Current Officer:\n\n"
            + "Rejects current application so long as its in 'Submitted and pending approval' state.\n\n"
            + "Withdraw current application:\n\n"
            + "Used when an applicant withdraws from the current application. It must be in 'Submitted and pending approval' state.\n\n"
            + "Activate a current account:\n\n"
            + "Results in an approved current application being converted into an 'active' current account.\n\n"
            + "Close a current account:\n\n"
            + "Results in an Activated current application being converted into an 'closed' current account.\n" + "\n"
            + "closedOnDate is closure date of current account\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountSubmitRequest.class)))
    public CommandProcessingResult handleCommands(@PathParam("externalId") @Parameter(description = "externalId") final String externalId,
            @QueryParam("command") @Parameter(description = "command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return handleCommands(null, externalId, apiRequestBodyAsJson, commandParam);
    }

    @Override
    @PUT
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a current application | Modify current account withhold tax applicability", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountActionRequest.class)))
    public CommandProcessingResult update(@PathParam("accountId") @Parameter(description = "accountId") final Long accountId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return updateSavingAccount(accountId, null, apiRequestBodyAsJson);
    }

    @Override
    @PUT
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a current application | Modify current account withhold tax applicability", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountActionRequest.class)))
    public CommandProcessingResult update(@PathParam("externalId") @Parameter(description = "externalId") final String externalId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        return updateSavingAccount(null, externalId, apiRequestBodyAsJson);
    }

    @Override
    @DELETE
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a current application", description = "At present we support hard delete of current application so long as its in 'Submitted and pending approval' state. One the application is moves past this state, it is not possible to do a 'hard' delete of the application or the account. An API endpoint will be added to close/de-activate the current account.")
    public CommandProcessingResult delete(@PathParam("accountId") @Parameter(description = "accountId") final Long accountId) {
        return deleteSavingAccount(accountId, null);
    }

    @Override
    @DELETE
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a current application", description = "At present we support hard delete of current application so long as its in 'Submitted and pending approval' state. One the application is moves past this state, it is not possible to do a 'hard' delete of the application or the account. An API endpoint will be added to close/de-activate the current account.")
    public CommandProcessingResult delete(@PathParam("externalId") @Parameter(description = "externalId") final String externalId) {
        return deleteSavingAccount(null, externalId);
    }

    private CommandProcessingResult updateSavingAccount(Long accountId, String externalId, String apiRequestBodyAsJson) {
        ExternalId accountExternalId = ExternalIdFactory.produce(externalId);
        accountId = getResolvedAccountId(accountId, accountExternalId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentAccount(accountId).withJson(apiRequestBodyAsJson)
                .build();

        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult handleCommands(Long accountId, String externalId, String apiRequestBodyAsJson, String commandParam) {
        ExternalId accountExternalId = ExternalIdFactory.produce(externalId);
        accountId = getResolvedAccountId(accountId, accountExternalId);

        String jsonApiRequest = apiRequestBodyAsJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, rejectAction)) {
            final CommandWrapper commandRequest = builder.rejectCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.withdrawnByApplicantAction)) {
            final CommandWrapper commandRequest = builder.withdrawCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.approveAction)) {
            final CommandWrapper commandRequest = builder.approveCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.undoApprovalAction)) {
            final CommandWrapper commandRequest = builder.undoCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.activateAction)) {
            final CommandWrapper commandRequest = builder.currentAccountActivation(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.closeAction)) {
            final CommandWrapper commandRequest = builder.closeCurrentAccountApplication(accountId).build();
            result = commandSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam, rejectAction, withdrawnByApplicantAction, approveAction,
                    undoApprovalAction, activateAction, closeAction);
        }

        return result;
    }

    private CommandProcessingResult deleteSavingAccount(Long accountId, String externalId) {
        ExternalId accountExternalId = ExternalIdFactory.produce(externalId);
        accountId = getResolvedAccountId(accountId, accountExternalId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCurrentAccount(accountId).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private Long getResolvedAccountId(Long accountId, ExternalId accountExternalId) {
        Long resolvedAccountId = accountId;
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
