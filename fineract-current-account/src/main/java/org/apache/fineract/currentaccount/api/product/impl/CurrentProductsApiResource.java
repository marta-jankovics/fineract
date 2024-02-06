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

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIER_API_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.IDENTIFIER_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_AND_IDENTIFIER_API_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_API_PARAM;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.ID_TYPE_PARAM;

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
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.service.product.CurrentProductResolver;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.api.jersey.Pagination;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedRequest;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Component;

@Path("/v1/current-products")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Component
@Tag(name = "Current Products", description = "An MFIs current product offerings are modeled using this API." + "\n"
        + "When creating current accounts, the details from the current product are used to auto fill details of the current account application process.")
@RequiredArgsConstructor
public class CurrentProductsApiResource implements CurrentProductApi {

    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService;
    private final CurrentProductReadService currentProductReadService;
    private final CurrentProductResponseDataMapper currentProductResponseDataMapper;

    @GET
    @Path("template")
    @Operation(operationId = "templateCurrentProduct", summary = "Retrieve Current Product Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Request:\n \n" + "current-products/template \n \n")
    @Override
    public CurrentProductTemplateResponseData template() {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_ENTITY_NAME);
        return this.currentProductReadService.retrieveTemplate();
    }

    @GET
    @Operation(operationId = "retrieveAllCurrentProducts", summary = "List Current Products", description = "Lists Current Products\n\n"
            + "Example Requests:\n" + "\n" + "current-products")
    @Override
    public Page<CurrentProductResponseData> retrieveAll(
            @Pagination @SortDefault(sort = "createdDate") @Parameter(hidden = true) Pageable pageable) {
        this.context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_ENTITY_NAME);
        return currentProductResponseDataMapper.map(this.currentProductReadService.retrieveAll(PagedRequest.createFrom(pageable)));
    }

    @GET
    @Path(IDENTIFIER_API_PARAM)
    @Operation(operationId = "retrieveOneCurrentProduct", summary = "Retrieve a Current Product", description = "Retrieves a Current Product \n \n"
            + "Example Requests:\n" + "\n" + "current-products/1")
    @Override
    public CurrentProductResponseData retrieveOne(
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM) final String identifier) {
        return retrieveOne(CurrentProductResolver.resolveDefault(identifier));
    }

    @GET
    @Path(ID_TYPE_AND_IDENTIFIER_API_PARAM)
    @Operation(operationId = "retrieveOneCurrentProduct", summary = "Retrieve a Current Product by alternative id", description = "Retrieves a Current Product by alternative id \n \n"
            + "Example Requests:\n" + "\n" + "current-products/external-id/randomExtId1")
    @Override
    public CurrentProductResponseData retrieveOne(
            @PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM, required = true) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM, required = true) final String identifier) {
        return retrieveOne(CurrentProductResolver.resolve(idType, identifier));
    }

    @POST
    @Operation(operationId = "createCurrentProduct", summary = "Create a Current Product", description = "Creates a Current Product\n\n"
            + "Mandatory Fields: name, shortName, currencyCode, currencyDigitsAfterDecimal, accountingType, allowOverdraft, allowForceTransaction, balanceCalculationType, locale\n\n"
            + "Optional Fields: externalId, currencyInMultiplesOf, overdraftLimit, minimumRequiredBalance, description")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductCommandResponse.class))) })
    @Override
    public CommandProcessingResult create(@Parameter(hidden = true) final String requestJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCurrentProduct().withJson(requestJson).build();
        return this.commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    @PUT
    @Path(IDENTIFIER_API_PARAM)
    @Operation(operationId = "updateCurrentProduct", summary = "Update a Current Product", description = "Updates a Current Product")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult update(@PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM) final String identifier,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentProduct(CurrentProductResolver.resolveDefault(identifier), requestJson);
    }

    @PUT
    @Path(ID_TYPE_AND_IDENTIFIER_API_PARAM)
    @Operation(operationId = "updateCurrentProductByIdentifier", summary = "Update a Current Product by alternative id", description = "Updates a Current Product by alternative id")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductUpdateCommandResponse.class))) })
    @Override
    public CommandProcessingResult update(@PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM) final String identifier,
            @Parameter(hidden = true) final String requestJson) {
        return updateCurrentProduct(CurrentProductResolver.resolve(idType, identifier), requestJson);
    }

    @DELETE
    @Path(ID_TYPE_API_PARAM)
    @Operation(operationId = "deleteCurrentProduct", summary = "Delete a Current Product", description = "Delete a Current Product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductDeleteCommandResponse.class))) })
    @Override
    public CommandProcessingResult delete(@PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM) final String identifier) {
        return deleteCurrentProduct(CurrentProductResolver.resolveDefault(identifier));
    }

    @DELETE
    @Path(ID_TYPE_AND_IDENTIFIER_API_PARAM)
    @Operation(operationId = "deleteCurrentProductByIdentifier", summary = "Delete a Current Product by alternative id", description = "Delete a Current Product by alternative id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.CurrentProductDeleteCommandResponse.class))) })
    @Override
    public CommandProcessingResult delete(@PathParam(ID_TYPE_PARAM) @Parameter(description = ID_TYPE_PARAM) final String idType,
            @PathParam(IDENTIFIER_PARAM) @Parameter(description = IDENTIFIER_PARAM) final String identifier) {
        return deleteCurrentProduct(CurrentProductResolver.resolve(idType, identifier));
    }

    private CommandProcessingResult deleteCurrentProduct(@NotNull CurrentProductResolver productResolver) {
        String identifier = getResolvedProductId(productResolver);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCurrentProduct(identifier).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private String getResolvedProductId(@NotNull CurrentProductResolver productResolver) {
        return currentProductReadService.retrieveId(productResolver);
    }

    private CurrentProductResponseData retrieveOne(@NotNull CurrentProductResolver productResolver) {
        context.authenticatedUser().validateHasReadPermission(CurrentAccountApiConstants.CURRENT_PRODUCT_ENTITY_NAME);
        return currentProductResponseDataMapper.map(currentProductReadService.retrieve(productResolver));
    }

    private CommandProcessingResult updateCurrentProduct(@NotNull CurrentProductResolver productResolver, String requestJson) {
        String identifier = getResolvedProductId(productResolver);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCurrentProduct(identifier).withJson(requestJson).build();
        return commandSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
