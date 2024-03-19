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
package org.apache.fineract.binx.config;

import org.apache.fineract.binx.currentaccount.service.BinxCurrentDetailsReadService;
import org.apache.fineract.binx.currentaccount.service.BinxCurrentDetailsReadServiceImpl;
import org.apache.fineract.binx.currentaccount.service.BinxTransactionMetadataWriteService;
import org.apache.fineract.binx.currentaccount.statement.service.BinxCurrentCamt053StatementGenerator;
import org.apache.fineract.binx.savings.service.BinxSavingsCamt053StatementGenerator;
import org.apache.fineract.binx.savings.service.BinxSavingsDetailsReadService;
import org.apache.fineract.binx.savings.service.BinxSavingsDetailsReadServiceImpl;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataWriteService;
import org.apache.fineract.currentaccount.statement.service.CurrentCamt053StatementGenerator;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsHelper;
import org.apache.fineract.statement.service.SavingsCamt053StatementGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ComponentScan("org.apache.fineract.binx")
@Conditional(BinxModuleEnabledCondition.class)
@Order(-10)
public class BinxAutoConfiguration {

    @Bean
    @Primary
    public BinxSavingsDetailsReadService savingsDetailsReadService(ReadWriteNonCoreDataService nonCoreDataService,
            GenericDataService genericDataService, DatabaseSpecificSQLGenerator sqlGenerator, JdbcTemplate jdbcTemplate) {
        return new BinxSavingsDetailsReadServiceImpl(nonCoreDataService, genericDataService, sqlGenerator, jdbcTemplate);
    }

    @Bean
    @Primary
    public BinxCurrentDetailsReadService currentDetailsReadService(ReadWriteNonCoreDataService nonCoreDataService,
            GenericDataService genericDataService, DatabaseSpecificSQLGenerator sqlGenerator, JdbcTemplate jdbcTemplate) {
        return new BinxCurrentDetailsReadServiceImpl(nonCoreDataService, genericDataService, sqlGenerator, jdbcTemplate);
    }

    @Bean
    @Primary
    public CurrentCamt053StatementGenerator currentCamt053StatementGenerator(CurrentAccountRepository currentAccountRepository,
            AccountIdentifierRepository accountIdentifierRepository, CurrentTransactionRepository transactionRepository,
            CurrentAccountDailyBalanceReadService dailyBalanceReadService, BinxCurrentDetailsReadService detailsReadService) {
        return new BinxCurrentCamt053StatementGenerator(currentAccountRepository, accountIdentifierRepository, transactionRepository,
                dailyBalanceReadService, detailsReadService);
    }

    @Bean
    @Primary
    public SavingsCamt053StatementGenerator savingsCamt053StatementGenerator(SavingsAccountRepository savingsAccountRepository,
            SavingsAccountTransactionSummaryWrapper summaryWrapper, SavingsHelper savingsHelper,
            BinxSavingsDetailsReadService detailsReadService) {
        return new BinxSavingsCamt053StatementGenerator(savingsAccountRepository, summaryWrapper, savingsHelper, detailsReadService);
    }

    @Bean
    @Primary
    public CurrentTransactionMetadataWriteService currentTransactionMetadataWriteService(CurrentAccountRepository currentAccountRepository,
            CurrentTransactionRepository currentTransactionRepository, BinxCurrentDetailsReadService currentDetailsReadService,
            PaymentTypeRepository paymentTypeRepository) {
        return new BinxTransactionMetadataWriteService(currentAccountRepository, currentTransactionRepository, currentDetailsReadService,
                paymentTypeRepository);
    }
}
