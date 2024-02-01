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
package org.apache.fineract.statement.service;

import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;

public interface SavingsStatementService extends AccountStatementService {

    @Override
    default boolean isSupport(PortfolioProductType productType) {
        return productType == SAVING;
    }

    Map<String, Object> retrieveClientDetails(@NotNull Long clientId);

    Map<String, Object> retrieveAccountDetails(@NotNull Long clientId, @NotNull Long accountId);

    @NotNull
    List<Long> getPendingTransactionIds(@NotNull Long accountId, List<Long> transactionIds);

    Map<Long, Map<String, Object>> retrieveTransactionDetails(List<Long> transactionIds);

    boolean hasTransaction(@NotNull Long accountId, @NotNull Long transactionId, @NotNull String internalCorrelationId,
            String categoryPurposeCode, @NotNull List<SavingsAccountTransactionType> types);
}