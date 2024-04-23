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
import org.apache.fineract.currentaccount.api.transaction.impl.CurrentTransactionsApiResource;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentAccountTransactionActionCommandStrategy implements CommandStrategy {

    private final CurrentTransactionsApiResource currentTransactionsApiResource;
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
        String body = batchRequest.getBody();
        final List<String> pathParameters = Splitter.on("/transactions/").splitToList(relativeUrl);
        final List<String> transactionParameters = Splitter.on('/').splitToList(pathParameters.get(1));
        String transactionIdType = null;
        String transactionIdentifier;
        if (transactionParameters.size() > 1) {
            transactionIdType = transactionParameters.get(0);
            transactionIdentifier = transactionParameters.get(1);
        } else {
            transactionIdentifier = transactionParameters.get(0);
        }
        CommandProcessingResult responseBody;
        final List<String> accountParameters = Splitter.on('/').splitToList(pathParameters.get(0));
        if (accountParameters.size() > 2) {
            String accountIdType = accountParameters.get(1);
            String accountIdentifier = accountParameters.get(2);
            if (accountParameters.size() > 3) {
                String accountSubIdentifier = accountParameters.get(3);
                responseBody = transactionIdType == null
                        ? currentTransactionsApiResource.actionByAccountIdTypeIdentifierSubIdentifierTransactionIdentifier(accountIdType,
                                accountIdentifier, accountSubIdentifier, transactionIdentifier, command, body)
                        : currentTransactionsApiResource.actionByAccountIdTypeIdentifierSubIdentifierTransactionIdTypeIdentifier(
                                accountIdType, accountIdentifier, accountSubIdentifier, transactionIdType, transactionIdentifier, command,
                                body);
            } else {
                responseBody = transactionIdType == null
                        ? currentTransactionsApiResource.actionByAccountIdTypeIdentifierTransactionIdentifier(accountIdType,
                                accountIdentifier, transactionIdentifier, command, body)
                        : currentTransactionsApiResource.actionByAccountIdTypeIdentifierTransactionIdTypeIdentifier(accountIdType,
                                accountIdentifier, transactionIdType, transactionIdentifier, command, body);
            }
        } else {
            String accountId = accountParameters.get(1);
            responseBody = transactionIdType == null
                    ? currentTransactionsApiResource.actionByAccountIdentifierTransactionIdentifier(accountId, transactionIdentifier,
                            command, body)
                    : currentTransactionsApiResource.actionByAccountIdentifierTransactionIdTypeIdentifier(accountId, transactionIdType,
                            transactionIdentifier, command, body);
        }

        return new BatchResponse().setRequestId(batchRequest.getRequestId()).setStatusCode(HttpStatus.SC_OK)
                .setHeaders(batchRequest.getHeaders()).setBody(jsonSerializer.serialize(responseBody));
    }
}
