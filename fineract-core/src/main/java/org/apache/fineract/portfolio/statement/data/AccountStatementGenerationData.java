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
package org.apache.fineract.portfolio.statement.data;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.domain.StatementBatchType;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;

@Getter
@AllArgsConstructor
public class AccountStatementGenerationData {

    private final Long accountStatementId;
    private final Long accountId;
    private final LocalDate generationDate;
    private final Long productId;
    private final String clientId;
    private final PortfolioProductType productType;
    private final String statementCode;
    private final StatementType statementType;
    private final StatementPublishType publishType;
    private final StatementBatchType batchType;
}
