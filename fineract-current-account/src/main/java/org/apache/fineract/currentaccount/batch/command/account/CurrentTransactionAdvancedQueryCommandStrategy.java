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
package org.apache.fineract.currentaccount.batch.command.account;

import static org.apache.fineract.batch.command.CommandStrategyUtils.relativeUrlWithoutVersion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.currentaccount.api.transaction.impl.CurrentTransactionsApiResource;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.portfolio.search.data.AdvancedQueryRequest;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentTransactionAdvancedQueryCommandStrategy implements CommandStrategy {

    private final CurrentTransactionsApiResource currentTransactionsApiResource;

    @Override
    public BatchResponse execute(BatchRequest batchRequest, UriInfo uriInfo) {
        String relativeUrl = relativeUrlWithoutVersion(batchRequest);
        String body = batchRequest.getBody();
        PagedLocalRequest<AdvancedQueryRequest> queryRequest;
        try {
            queryRequest = new ObjectMapper().readValue(body, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new InvalidJsonException(e);
        }
        final List<String> pathParameters = Splitter.on('/').splitToList(relativeUrl);
        int size = pathParameters.size();
        String response;
        if (size > 3) {
            String idType = pathParameters.get(1);
            String identifier = pathParameters.get(2);
            if (size > 4) {
                String subIdentifier = pathParameters.get(3);
                response = currentTransactionsApiResource.advancedQueryByAccountIdTypeIdentifierSubIdentifier(idType, identifier,
                        subIdentifier, queryRequest, uriInfo);
            } else {
                response = currentTransactionsApiResource.advancedQueryByAccountIdTypeIdentifier(idType, identifier, queryRequest, uriInfo);
            }
        } else if (size == 3) {
            String accountId = pathParameters.get(1);
            response = currentTransactionsApiResource.advancedQueryByAccountIdentifier(accountId, queryRequest, uriInfo);
        } else {
            response = currentTransactionsApiResource.advancedQuery(queryRequest, uriInfo);
        }

        return new BatchResponse().setRequestId(batchRequest.getRequestId()).setStatusCode(HttpStatus.SC_OK).setBody(response)
                .setHeaders(batchRequest.getHeaders());
    }
}
