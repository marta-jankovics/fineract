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

import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
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
public class CreateCurrentAccountCommandStrategy implements CommandStrategy {

    private final CurrentAccountsApiResource currentAccountsApiResource;
    private final DefaultToApiJsonSerializer<SavingsAccountTransactionData> jsonSerializer;

    @Override
    public BatchResponse execute(BatchRequest batchRequest, UriInfo uriInfo) {
        CommandProcessingResult responseBody = currentAccountsApiResource.create(batchRequest.getBody());

        return new BatchResponse().setRequestId(batchRequest.getRequestId()).setStatusCode(HttpStatus.SC_OK)
                .setHeaders(batchRequest.getHeaders()).setBody(jsonSerializer.serialize(responseBody));
    }
}
