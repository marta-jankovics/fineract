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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.data.domain.Page;

public interface CurrentAccountsApi {

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Current Account Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Requests:\n\n" + "currentaccounts/template\n\n")
    CurrentAccountTemplateResponseData template();

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List current applications/accounts", description = "Lists current applications/accounts\n\n"
            + "Example Requests:\n" + "\n" + "currentaccounts\n" + "\n" + "\n" + "currentaccounts")
    Page<CurrentAccountResponseData> retrieveAll(@Context UriInfo uriInfo,
            @QueryParam("offset") @Parameter(description = "offset") Long offset,
            @QueryParam("limit") @Parameter(description = "limit") Integer limit,
            @QueryParam("page") @Parameter(description = "page") Integer page,
            @QueryParam("size") @Parameter(description = "size") Integer size,
            @QueryParam("orderBy") @Parameter(description = "orderBy") String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") String sortOrder);

    @GET
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account", description = "Retrieves a current application/account\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/1")
    CurrentAccountResponseData retrieveOne(@PathParam("accountId") @Parameter(description = "accountId") Long accountId);

    @GET
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a current application/account by external id", description = "Retrieves a current application/account by external id\n\n"
            + "Example Requests :\n" + "\n" + "currentaccounts/external-id/ExternalId1")
    CurrentAccountResponseData retrieveOne(@PathParam("externalId") @Parameter(description = "externalId") String externalId);

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Submit new current application", description = "Submits new current application\n\n"
            + "Mandatory Fields: clientId, productId, accountNo, submittedOnDate\n\n" + "Optional Fields: externalId, submittedOnDate\n\n"
            + "Inherited from Product (if not provided): enforceMinRequiredBalance, minimumRequiredBalance, allowOverdraft, overdraftLimit\n\n")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PostCurrentAccountSubmitRequest.class)))
    CommandProcessingResult submitApplication(@Parameter(hidden = true) String apiRequestBodyAsJson);

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
    CommandProcessingResult handleCommands(@PathParam("accountId") @Parameter(description = "accountId") Long accountId,
            @QueryParam("command") @Parameter(description = "command") String commandParam,
            @Parameter(hidden = true) String apiRequestBodyAsJson);

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
    CommandProcessingResult handleCommands(@PathParam("externalId") @Parameter(description = "externalId") String externalId,
            @QueryParam("command") @Parameter(description = "command") String commandParam,
            @Parameter(hidden = true) String apiRequestBodyAsJson);

    @PUT
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a current application | Modify current account withhold tax applicability", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountActionRequest.class)))
    CommandProcessingResult update(@PathParam("accountId") @Parameter(description = "accountId") Long accountId,
            @Parameter(hidden = true) String apiRequestBodyAsJson);

    @PUT
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Modify a current application | Modify current account withhold tax applicability", description = "Modify a current application:\n\n"
            + "Current application can only be modified when in 'Submitted and pending approval' state. Once the application is approved, the details cannot be changed using this method.\n\n"
            + "Showing request/response for 'Modify a current application'")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentAccountsApiResourceSwagger.PutCurrentAccountActionRequest.class)))
    CommandProcessingResult update(@PathParam("externalId") @Parameter(description = "externalId") String externalId,
            @Parameter(hidden = true) String apiRequestBodyAsJson);

    @DELETE
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a current application", description = "At present we support hard delete of current application so long as its in 'Submitted and pending approval' state. One the application is moves past this state, it is not possible to do a 'hard' delete of the application or the account. An API endpoint will be added to close/de-activate the current account.")
    CommandProcessingResult delete(@PathParam("accountId") @Parameter(description = "accountId") Long accountId);

    @DELETE
    @Path("/external-id/{externalId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a current application", description = "At present we support hard delete of current application so long as its in 'Submitted and pending approval' state. One the application is moves past this state, it is not possible to do a 'hard' delete of the application or the account. An API endpoint will be added to close/de-activate the current account.")
    CommandProcessingResult delete(@PathParam("externalId") @Parameter(description = "externalId") String externalId);
}
