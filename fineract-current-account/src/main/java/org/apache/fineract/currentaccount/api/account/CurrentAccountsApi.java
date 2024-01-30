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

import com.google.gson.JsonObject;
import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.account.IdentifiersResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrentAccountsApi {

    CurrentAccountTemplateResponseData template();

    Page<CurrentAccountResponseData> retrieveAll(Pageable pageable);

    CurrentAccountResponseData retrieveOne(String accountId);

    CurrentAccountResponseData retrieveOne(String idType, String identifier);

    CurrentAccountResponseData retrieveOne(String idType, String identifier, String subId);

    IdentifiersResponseData retrieveIdentifiers(String accountId);

    IdentifiersResponseData retrieveIdentifiers(String idType, String identifier);

    IdentifiersResponseData retrieveIdentifiers(String idType, String identifier, String subIdentifier);

    CommandProcessingResult create(String requestJson);

    CommandProcessingResult action(String accountId, String commandParam, String requestJson);

    CommandProcessingResult action(String idType, String identifier, String commandParam, String requestJson);

    CommandProcessingResult action(String idType, String identifier, String subId, String commandParam, String requestJson);

    CommandProcessingResult update(String accountId, String requestJson);

    CommandProcessingResult update(String idType, String identifier, String requestJson);

    CommandProcessingResult update(String idType, String identifier, String subIdentifier, String requestJson);

    Page<JsonObject> advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    Page<JsonObject> advancedQuery(String accountId, PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    Page<JsonObject> advancedQuery(String idType, String identifier, PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    Page<JsonObject> advancedQuery(String idType, String identifier, String subId, PagedLocalRequest<AdvancedQueryRequest> queryRequest,
            UriInfo uriInfo);
}
