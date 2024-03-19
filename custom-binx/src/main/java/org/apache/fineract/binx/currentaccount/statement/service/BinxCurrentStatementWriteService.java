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
package org.apache.fineract.binx.currentaccount.statement.service;

import static org.apache.fineract.interoperation.domain.InteropIdentifierType.IBAN;
import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

import jakarta.annotation.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.binx.config.BinxModuleEnabledCondition;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.statement.service.CurrentStatementWriteService;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Priority(-10)
@Conditional(BinxModuleEnabledCondition.class)
public class BinxCurrentStatementWriteService extends CurrentStatementWriteService {

    private final AccountIdentifierRepository accountIdentifierRepository;

    public BinxCurrentStatementWriteService(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, CurrentAccountRepository currentAccountRepository,
            AccountIdentifierRepository accountIdentifierRepository) {
        super(statementParser, productStatementRepository, statementRepository, currentAccountRepository);
        this.accountIdentifierRepository = accountIdentifierRepository;
    }

    @Override
    protected String getDefaultSequencePrefix(@NotNull String accountId) {
        AccountIdentifier identifier = accountIdentifierRepository.getByAccountTypeAndAccountIdAndIdentifierType(CURRENT, accountId, IBAN);
        return identifier == null ? null : identifier.getSubValue();
    }
}
