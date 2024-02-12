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

import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.account.IdentifiersResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.portfolio.search.data.AdvancedQueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrentAccountsApi {

    CurrentAccountTemplateResponseData template();

    Page<CurrentAccountResponseData> retrieveAll(Pageable pageable);

    CurrentAccountResponseData retrieveOneByIdentifier(String identifier);

    CurrentAccountResponseData retrieveOneByIdTypeIdentifier(String idType, String identifier);

    CurrentAccountResponseData retrieveOneByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier);

    IdentifiersResponseData retrieveIdentifiersByIdentifier(String identifier);

    IdentifiersResponseData retrieveIdentifiersByIdTypeIdentifier(String idType, String identifier);

    IdentifiersResponseData retrieveIdentifiersByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier);

    CommandProcessingResult create(String requestJson);

    CommandProcessingResult actionByIdentifier(String identifier, String commandParam, String requestJson);

    CommandProcessingResult actionByIdTypeAndIdentifier(String idType, String identifier, String commandParam, String requestJson);

    CommandProcessingResult actionByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier,
            String commandParam, String requestJson);

    CommandProcessingResult updateByIdentifier(String identifier, String requestJson);

    CommandProcessingResult updateByIdTypeIdentifier(String idType, String identifier, String requestJson);

    CommandProcessingResult updateByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier,
            String requestJson);

    String advancedQuery(PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    String advancedQueryByIdentifier(String identifier, PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);

    String advancedQueryByIdTypeIdentifier(String idType, String identifier, PagedLocalRequest<AdvancedQueryRequest> queryRequest,
            UriInfo uriInfo);

    String advancedQueryByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier,
            PagedLocalRequest<AdvancedQueryRequest> queryRequest, UriInfo uriInfo);
}