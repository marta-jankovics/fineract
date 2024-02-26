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
package org.apache.fineract.currentaccount.statement.service;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.statement.service.AccountStatementService;

public interface CurrentStatementService extends AccountStatementService {

    String CONVERSION_ACCOUNT_DISCRIMINATOR = "K";
    String DISPOSAL_ACCOUNT_DISCRIMINATOR = "R";

    Map<String, Object> retrieveClientDetails(@NotNull Long clientId);

    @NotNull
    List<String> getPendingTransactionIds(@NotNull String accountId, List<String> transactionIds);

    Map<String, Map<String, Object>> getTransactionDetails(List<String> transactionIds);

    boolean hasTransaction(@NotNull String accountId, @NotNull String transactionId, @NotNull String internalCorrelationId,
            String categoryPurposeCode, @NotNull List<CurrentTransactionType> types);
}
