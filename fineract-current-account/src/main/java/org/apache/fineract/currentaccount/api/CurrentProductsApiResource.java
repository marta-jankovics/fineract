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
package org.apache.fineract.currentaccount.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.data.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.service.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedRequest;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Path("/v1/currentproducts")
@Component
@Tag(name = "Current Product", description = "An MFIs current product offerings are modeled using this API." + "\n"
        + "When creating current accounts, the details from the current product are used to auto fill details of the current account application process.")
@RequiredArgsConstructor
public class CurrentProductsApiResource {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CurrentProductReadService currentProductReadPlatformService;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Current Product", description = "Creates a Current Product\n\n"
            + "Mandatory Fields: name, shortName, description, currencyCode, digitsAfterDecimal, accountingType, allowOverdraft\n\n"
            + "Optional Fields: inMultiplesOf, overdraftLimit, minRequiredBalance")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PostCurrentProductRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PostCurrentProductResponse.class))) })
    public CommandProcessingResult create(@Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentProduct().withJson(requestJson).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a Current Product", description = "Updates a Current Product")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PutCurrentProductRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PutCurrentProductResponse.class))) })
    public CommandProcessingResult update(@PathParam("productId") @Parameter(description = "productId") final Long productId,
            @Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentProduct(productId).withJson(requestJson).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Current Products", description = "Lists Current Products\n\n" + "Example Requests:\n" + "\n"
            + "currentproducts")
    public Page<CurrentProductResponseData> retrieveAll(@QueryParam("size") @Parameter(description = "size") final Integer size,
            @QueryParam("page") @Parameter(description = "page") final Integer page,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadPlatformService.retrieveAll(PagedRequest.createFrom(page, size, sortOrder, orderBy));
    }

    @GET
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Current Product", description = "Retrieves a Current Product\n\n" + "Example Requests:\n" + "\n"
            + "currentproducts/1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.GetCurrentProductResponse.class))) })
    public CurrentProductResponseData retrieveOne(@PathParam("productId") @Parameter(description = "productId") final Long productId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadPlatformService.retrieveById(productId);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Current Product Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "\n" + "Field Defaults\n" + "Allowed description Lists\n" + "Example Request:\n" + "Account Mapping:\n" + "\n"
            + "currentproducts/template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.GetCurrentProductTemplateResponse.class))) })
    public CurrentProductTemplateResponseData retrieveTemplate() {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadPlatformService.retrieveTemplate();
    }

    @DELETE
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a Current Product", description = "Delete a Current Product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.DeleteCurrentProductResponse.class))) })
    public CommandProcessingResult delete(@PathParam("productId") @Parameter(description = "productId") final Long productId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCurrentProduct(productId).build();
        return this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
