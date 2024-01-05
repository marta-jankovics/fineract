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
package org.apache.fineract.currentaccount.api.transaction;

import io.swagger.v3.oas.annotations.Operation;
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
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.stereotype.Component;

@Path("/v1/currentaccounts/{accountId}/transactions")
@Component
@Tag(name = "Current Account Transactions", description = "")
@RequiredArgsConstructor
public class CurrentTransactionsApiResource {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CurrentTransactionReadService currentAccountTransactionReadService;

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public CurrentTransactionTemplateResponseData retrieveTemplate(@PathParam("accountId") final Long accountId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentAccountTransactionReadService.retrieveTemplate(accountId);
    }

    @GET
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public CurrentTransactionResponseData retrieveOne(@PathParam("accountId") final Long accountId,
            @PathParam("transactionId") final Long transactionId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_TRANSACTION_RESOURCE_NAME);
        return this.currentAccountTransactionReadService.retrieveTransactionById(accountId, transactionId);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Deposit/Withdraw/Hold Amount transaction API", description = "Deposit/Withdraw/Hold Amount transaction API\n\n"
            + "Example Requests:\n" + "\n" + "\n" + "currentaccounts/{accountId}/transactions/?command=deposit\n" + "\n"
            + "Accepted command = deposit, withdraw, hold")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentAccountTransactionsRequest.class)))
    public CommandProcessingResult transaction(@PathParam("accountId") final Long accountId,
            @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam, final String apiRequestBodyAsJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;
        if (is(commandParam, CurrentAccountApiConstants.COMMAND_DEPOSIT)) {
            final CommandWrapper commandRequest = builder.currentTransactionDeposit(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_WITHDRAW)) {
            final CommandWrapper commandRequest = builder.currentTransactionWithdraw(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, CurrentAccountApiConstants.COMMAND_HOLD)) {
            final CommandWrapper commandRequest = builder.currentTransactionHold(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException(CurrentAccountApiConstants.COMMAND, commandParam,
                    CurrentAccountApiConstants.COMMAND_DEPOSIT, CurrentAccountApiConstants.COMMAND_WITHDRAW,
                    CurrentAccountApiConstants.COMMAND_HOLD);
        }
        return result;
    }

    @POST
    @Path("{transactionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Release Amount transaction API", description = "Release Amount transaction API\n\n" + "Example Requests:\n" + "\n"
            + "\n" + "currentaccounts/{accountId}/transactions/{transactionId}?command=release\n" + "\n"
            + "Accepted command = releaseAmount")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentTransactionsApiResourceSwagger.PostCurrentAccountTransactionsRequest.class)))
    public CommandProcessingResult transaction(@PathParam("accountId") final Long accountId,
            @PathParam("transactionId") final Long transactionId, @QueryParam(CurrentAccountApiConstants.COMMAND) final String commandParam,
            final String apiRequestBodyAsJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

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
