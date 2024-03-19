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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.UPDATE;
import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.service.AccountStatementWriteServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(CurrentStatementWriteService.class)
public class CurrentStatementWriteService extends AccountStatementWriteServiceImpl {

    private final CurrentAccountRepository accountRepository;

    public CurrentStatementWriteService(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, CurrentAccountRepository currentAccountRepository) {
        super(statementParser, productStatementRepository, statementRepository);
        this.accountRepository = currentAccountRepository;
    }

    @Override
    public boolean isSupport(@NotNull PortfolioProductType productType) {
        return productType == PortfolioProductType.CURRENT;
    }

    @Override
    protected String getDefaultSequencePrefix(@NotNull String accountId) {
        return CURRENT.name();
    }

    @Override
    protected List<String> getStatementAccountIds(@NotNull String productId, @NotNull PortfolioProductType productType) {
        return accountRepository.getIdsByProductIdAndStatus(productId, CurrentAccountStatus.getEnabledStatusList(UPDATE));
    }

    @Override
    protected void postStatementCreate(@NotNull AccountStatement statement) {
        String accountId = statement.getAccountId();
        CurrentAccount currentAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("current.account", accountId));
        if (currentAccount.getStatus().isActive()) {
            statement.activate();
        }
    }
}
