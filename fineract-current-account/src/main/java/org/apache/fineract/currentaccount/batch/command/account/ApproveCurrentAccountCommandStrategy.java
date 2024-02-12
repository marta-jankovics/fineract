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
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.COMMAND;

import com.google.common.base.Splitter;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.command.CommandStrategyUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.currentaccount.api.account.impl.CurrentAccountsApiResource;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApproveCurrentAccountCommandStrategy implements CommandStrategy {

    private final CurrentAccountsApiResource currentAccountsApiResource;
    private final DefaultToApiJsonSerializer<SavingsAccountTransactionData> jsonSerializer;

    @Override
    public BatchResponse execute(BatchRequest batchRequest, UriInfo uriInfo) {
        String relativeUrl = relativeUrlWithoutVersion(batchRequest);
        String command = null;
        int idx = relativeUrl.indexOf('?');
        if (idx > 0) {
            final Map<String, String> queryParameters = CommandStrategyUtils.getQueryParameters(relativeUrl);
            command = queryParameters.get(COMMAND);
            relativeUrl = relativeUrl.substring(0, idx);
        }
        final List<String> pathParameters = Splitter.on('/').splitToList(relativeUrl);
        int size = pathParameters.size();
        String body = batchRequest.getBody();
        CommandProcessingResult responseBody;
        if (size > 2) {
            String idType = pathParameters.get(1);
            String identifier = pathParameters.get(2);
            if (size > 3) {
                String subIdentifier = pathParameters.get(3);
                responseBody = currentAccountsApiResource.actionByIdTypeIdentifierSubIdentifier(idType, identifier, subIdentifier, command,
                        body);
            } else {
                responseBody = currentAccountsApiResource.actionByIdTypeAndIdentifier(idType, identifier, command, body);
            }
        } else {
            String accountId = pathParameters.get(1);
            responseBody = currentAccountsApiResource.actionByIdentifier(accountId, command, body);
        }

        return new BatchResponse().setRequestId(batchRequest.getRequestId()).setStatusCode(HttpStatus.SC_OK)
                .setHeaders(batchRequest.getHeaders()).setBody(jsonSerializer.serialize(responseBody));
    }
}
