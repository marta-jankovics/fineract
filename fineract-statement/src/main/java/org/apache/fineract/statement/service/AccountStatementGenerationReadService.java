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

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;

public interface AccountStatementGenerationReadService {

    boolean isSupport(PortfolioProductType productType);

    Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> retrieveStatementsToGenerate(
            @NotNull PortfolioProductType productType, @NotNull LocalDate transactionDate);

    default String calcBatchId(AccountStatementGenerationData generation) {
        return switch (generation.getBatchType()) {
            case SINGLE -> String.valueOf(generation.getAccountStatementId());
            case ACCOUNT -> String.valueOf(generation.getAccountId());
            case PRODUCT -> String.valueOf(generation.getProductId());
            case CLIENT -> String.valueOf(generation.getProductId()) + '/' + generation.getClientId();
            default -> String.valueOf(generation.getAccountStatementId());
        };
    }
}
