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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.STATEMENT_GENERATION;
import static org.apache.fineract.portfolio.PortfolioProductType.CURRENT;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.statement.domain.CurrentStatementRepository;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementStatus;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentStatementGenerationReadServiceImpl implements AccountStatementGenerationReadService {

    private final CurrentStatementRepository statementRepository;

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == CURRENT;
    }

    @Override
    public Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> retrieveStatementsToGenerate(
            @NotNull PortfolioProductType productType, @NotNull LocalDate transactionDate) {
        List<StatementStatus> statuses = StatementStatus.getFiltered(StatementStatus::canGenerate);
        List<CurrentAccountStatus> accountStatuses = CurrentAccountStatus.getEnabledStatusList(STATEMENT_GENERATION);
        List<AccountStatementGenerationData> statementGenerations = statementRepository.getStatementsToGenerate(transactionDate, statuses,
                accountStatuses);
        return statementGenerations.stream().collect(Collectors.groupingBy(AccountStatementGenerationData::getStatementType,
                Collectors.groupingBy(AccountStatementGenerationData::getPublishType, Collectors.groupingBy(this::calcBatchId))));
    }
}
