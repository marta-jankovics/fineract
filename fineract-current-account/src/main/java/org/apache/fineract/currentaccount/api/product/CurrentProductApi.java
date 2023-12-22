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
package org.apache.fineract.currentaccount.api.product;

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
import jakarta.ws.rs.core.MediaType;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.data.domain.Page;

public interface CurrentProductApi {

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Current Products", description = "Lists Current Products\n\n" + "Example Requests:\n" + "\n"
            + "currentproducts")
    Page<CurrentProductResponseData> retrieveAll(@QueryParam("offset") @Parameter(description = "offset") Long offset,
            @QueryParam("limit") @Parameter(description = "limit") Integer limit,
            @QueryParam("page") @Parameter(description = "page") Integer page,
            @QueryParam("size") @Parameter(description = "size") Integer size,
            @QueryParam("orderBy") @Parameter(description = "orderBy") String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") String sortOrder);

    @GET
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve a Current Product", description = "Retrieves a Current Product \n \n" + "Example Requests:\n" + "\n"
            + "currentproducts/1")
    CurrentProductResponseData retrieveOne(@PathParam("productId") @Parameter(description = "productId") Long productId);

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Current Product Template", description = "This is a convenience resource. It can be useful when building maintenance user interface screens for client applications. The template data returned consists of any or all of:\n"
            + "Example Request:\n \n" + "currentproducts/template \n \n")
    CurrentProductTemplateResponseData retrieveTemplate();

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Current Product", description = "Creates a Current Product\n\n"
            + "Mandatory Fields: name, shortName, description, currencyCode, digitsAfterDecimal, accountingType, allowOverdraft, enforceMinRequiredBalance\n\n"
            + "Optional Fields: inMultiplesOf, overdraftLimit, minRequiredBalance")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PostCurrentProductRequest.class)))
    CommandProcessingResult create(@Parameter(hidden = true) String requestJson);

    @PUT
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update a Current Product", description = "Updates a Current Product")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CurrentProductsApiResourceSwagger.PutCurrentProductRequest.class)))
    CommandProcessingResult update(@PathParam("productId") @Parameter(description = "productId") Long productId,
            @Parameter(hidden = true) String requestJson);

    @DELETE
    @Path("{productId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete a Current Product", description = "Delete a Current Product")
    CommandProcessingResult delete(@PathParam("productId") @Parameter(description = "productId") Long productId);
}
