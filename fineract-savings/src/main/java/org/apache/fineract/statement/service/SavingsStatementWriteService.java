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
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(SavingsStatementWriteService.class)
public class SavingsStatementWriteService extends AccountStatementWriteServiceImpl {

    private final SavingsAccountRepositoryWrapper savingsAccountRepository;

    public SavingsStatementWriteService(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper) {
        super(statementParser, productStatementRepository, statementRepository);
        this.savingsAccountRepository = savingsAccountRepositoryWrapper;
    }

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == SAVING;
    }

    @Override
    protected String getDefaultSequencePrefix(@NotNull String accountId) {
        return SAVING.name();
    }

    @Override
    protected List<String> getStatementAccountIds(@NotNull String productId, @NotNull PortfolioProductType productType) {
        return savingsAccountRepository.findIdsForStatement(Long.valueOf(productId)).stream().map(Object::toString).toList();
    }

    @Override
    protected boolean preStatementCreate(@NotNull String accountId) {
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(Long.valueOf(accountId));
        return !account.isClosed();
    }

    @Override
    protected void postStatementCreate(@NotNull AccountStatement statement) {
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(Long.valueOf(statement.getAccountId()));
        if (account.isActive()) {
            statement.activate();
        }
    }
}
