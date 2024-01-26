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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import org.apache.fineract.currentaccount.api.transaction.CurrentTransactionApi;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Path("/v1/current-accounts/{accountId}/transactions")
@Component
@Tag(name = "Current Transactions")
@RequiredArgsConstructor
public class CurrentTransactionsApiResource implements CurrentTransactionApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CurrentTransactionReadService currentTransactionReadService;

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @Override
    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public CurrentTransactionTemplateResponseData retrieveTemplate(@PathParam("accountId") final String accountId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTemplate(accountId);
    }

    @Override
    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Page<CurrentTransactionResponseData> retrieveAll(@PathParam("accountId") final String accountId, @Pagination Pageable pageable) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTransactionByAccountId(accountId, pageable);
    }

    @Override
    @GET
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public CurrentTransactionResponseData retrieveOne(@PathParam("accountId") final String accountId,
            @PathParam("transactionId") final String transactionId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentTransactionReadService.retrieveTransactionById(accountId, transactionId);
    }

    @Override
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Deposit/Withdrawal/Hold Amount transaction API", description = "Deposit/Withdrawal/Hold Amount transaction API\n\n"
            + "Example Requests:\n" + "\n" + "\n" + "currentaccounts/{accountId}/transactions/?command=deposit\n" + "\n"
            + "Accepted command = deposit, withdrawal, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
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

    @Override
    @POST
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n" + "Example Requests:\n" + "\n"
            + "\n" + "currentaccounts/{accountId}/transactions/{transactionId}?command=release\n" + "\n" + "Accepted command = release")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentTransactionsRequest.class)))
    public CommandProcessingResult transaction(@PathParam("accountId") final String accountId,
            @PathParam("transactionId") final String transactionId, @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam,
            @Parameter(hidden = true) final String requestJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(requestJson);

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
}
