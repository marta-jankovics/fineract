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
package org.apache.fineract.currentaccount.api.product.impl;

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
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.currentaccount.api.CurrentAccountApiConstants;
import org.apache.fineract.currentaccount.api.product.CurrentProductApi;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedRequest;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Path("/v1/currentproducts")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Component
@Tag(name = "Current Product", description = "An MFIs current product offerings are modeled using this API." + "\n"
        + "When creating current accounts, the details from the current product are used to auto fill details of the current account application process.")
@RequiredArgsConstructor
public class CurrentProductsApiResource implements CurrentProductApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;
    private final CurrentProductReadService currentProductReadService;

    @GET
    @Operation(summary = "List Current Products", description = "Lists Current Products\n\n" + "Example Requests:\n" + "\n"
            + "currentproducts")
    public Page<CurrentProductResponseData> retrieveAll(@Pagination @SortDefault(sort = "createdDate") Pageable pageable) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadService.retrieveAll(PagedRequest.createFrom(pageable));
    }

    @GET
    @Path("{productId}")
    @Operation(summary = "Retrieve a Current Product", description = "Retrieves a Current Product \n \n" + "Example Requests:\n" + "\n"
            + "currentproducts/1")
    public CurrentProductResponseData retrieveOne(@PathParam("productId") @Parameter(description = "productId") final UUID productId) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadService.retrieveById(productId);
    }

    @GET
    @Path("{idType}/{id}")
    @Operation(summary = "Retrieve a Current Product by alternative id", description = "Retrieves a Current Product by alternative id \n \n" + "Example Requests:\n" + "\n"
            + "currentproducts/external-id/randomExtId1")
    public CurrentProductResponseData retrieveOne(@PathParam("idType") @Parameter(description = "idType", required = true) final String idType,
                                                  @PathParam("id") @Parameter(description = "id", required = true) final String id) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadService.retrieveByIdTypeAndId(idType, id);
    }

    @GET
    @Path("template")
    @Operation(summary = "Retrieve Current Product Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Request:\n \n" + "currentproducts/template \n \n")
    public CurrentProductTemplateResponseData retrieveTemplate() {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME);
        return this.currentProductReadService.retrieveTemplate();
    }

    @POST
    @Operation(summary = "Create a Current Product", description = "Creates a Current Product\n\n"
            + "Mandatory Fields: name, shortName, currencyCode, currencyDigitsAfterDecimal, accountingType, allowOverdraft, allowForceTransaction, balanceCalculationType, locale\n\n"
            + "Optional Fields: externalId, currencyInMultiplesOf, overdraftLimit, minimumRequiredBalance, description")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PostCurrentProductRequest.class)))
    public CommandProcessingResult create(@Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentProduct().withJson(requestJson).build();
        return this.commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path("{productId}")
    @Operation(summary = "Update a Current Product", description = "Updates a Current Product")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PutCurrentProductRequest.class)))
    public CommandProcessingResult update(@PathParam("productId") @Parameter(description = "productId") final UUID productId,
                                          @Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentProduct(productId).withJson(requestJson).build();
        return this.commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path("{idType}/{id}")
    @Operation(summary = "Update a Current Product by alternative id", description = "Updates a Current Product by alternative id")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PutCurrentProductRequest.class)))
    public CommandProcessingResult update(@PathParam("idType") @Parameter(description = "idType") final String idType,
                                          @PathParam("id") @Parameter(description = "id") final String id,
                                          @Parameter(hidden = true) final String requestJson) {
        final UUID productId = resolveByIdType(idType, id);
        return update(productId, requestJson);
    }

    @DELETE
    @Path("{productId}")
    @Operation(summary = "Delete a Current Product", description = "Delete a Current Product")
    public CommandProcessingResult delete(@PathParam("productId") @Parameter(description = "productId") final UUID productId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCurrentProduct(productId).build();
        return this.commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @DELETE
    @Path("{idType}/{id}")
    @Operation(summary = "Delete a Current Product by alternative id", description = "Delete a Current Product by alternative id")
    public CommandProcessingResult delete(@PathParam("idType") @Parameter(description = "idType") final String idType,
            @PathParam("id") @Parameter(description = "id") final String id) {
        final UUID productId = resolveByIdType(idType, id);
        return delete(productId);
    }

    private UUID resolveByIdType(String idType, String id) {
        return this.currentProductReadService.retrieveIdByIdType(idType, id);
    }
}
