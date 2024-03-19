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
package org.apache.fineract.binx.savings.service;

import static org.apache.fineract.binx.BinxConstants.CONVERSION_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.BinxConstants.DISPOSAL_ACCOUNT_DISCRIMINATOR;

import jakarta.annotation.Priority;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.binx.config.BinxModuleEnabledCondition;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.service.SavingsStatementWriteService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Priority(-10)
@Conditional(BinxModuleEnabledCondition.class)
public class BinxSavingsStatementWriteService extends SavingsStatementWriteService {

    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final BinxSavingsDetailsReadService savingsDetailsReadService;

    public BinxSavingsStatementWriteService(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper,
            SavingsAccountRepositoryWrapper savingsAccountRepository, BinxSavingsDetailsReadService savingsDetailsReadService) {
        super(statementParser, productStatementRepository, statementRepository, savingsAccountRepositoryWrapper);
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsDetailsReadService = savingsDetailsReadService;
    }

    @Override
    protected String getDefaultSequencePrefix(@NotNull String accountId) {
        Long lAccountId = Long.valueOf(accountId);
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(lAccountId);
        Map<String, Object> accountDetails = savingsDetailsReadService.getAccountDetails(account.clientId(), lAccountId);
        if (accountDetails != null) {
            if (accountId.equals(accountDetails.get("conversion_account_id"))) {
                return CONVERSION_ACCOUNT_DISCRIMINATOR;
            }
            if (accountId.equals(accountDetails.get("disposal_account_id"))) {
                return DISPOSAL_ACCOUNT_DISCRIMINATOR;
            }
        }
        return null;
    }
}
