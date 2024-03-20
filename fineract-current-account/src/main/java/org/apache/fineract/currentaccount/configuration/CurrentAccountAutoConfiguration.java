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
package org.apache.fineract.currentaccount.configuration;

import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMappingRepository;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingHelper;
import org.apache.fineract.currentaccount.assembler.account.CurrentAccountAssembler;
import org.apache.fineract.currentaccount.assembler.account.impl.CurrentAccountAssemblerImpl;
import org.apache.fineract.currentaccount.assembler.product.CurrentProductAssembler;
import org.apache.fineract.currentaccount.assembler.product.impl.CurrentProductAssemblerImpl;
import org.apache.fineract.currentaccount.assembler.transaction.CurrentTransactionAssembler;
import org.apache.fineract.currentaccount.assembler.transaction.impl.CurrentTransactionAssemblerImpl;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountIdentifiersResponseDataMapper;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountBalanceRepository;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountDailyBalanceRepository;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accounting.CurrentAccountAccountingRepository;
import org.apache.fineract.currentaccount.repository.entityaction.EntityActionRepository;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.account.read.impl.CurrentAccountBalanceReadServiceImpl;
import org.apache.fineract.currentaccount.service.account.read.impl.CurrentAccountReadServiceImpl;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceWriteService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.service.account.write.impl.CurrentAccountBalanceWriteServiceImpl;
import org.apache.fineract.currentaccount.service.account.write.impl.CurrentAccountDailyBalanceReadServiceImpl;
import org.apache.fineract.currentaccount.service.account.write.impl.CurrentAccountDailyBalanceWriteServiceImpl;
import org.apache.fineract.currentaccount.service.account.write.impl.CurrentAccountWriteServiceImpl;
import org.apache.fineract.currentaccount.service.accounting.write.CurrentAccountAccountingWriteService;
import org.apache.fineract.currentaccount.service.accounting.write.impl.CurrentAccountAccountingWriteServiceImpl;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.currentaccount.service.product.read.impl.CurrentProductReadServiceImpl;
import org.apache.fineract.currentaccount.service.product.write.CurrentProductWriteService;
import org.apache.fineract.currentaccount.service.product.write.impl.CurrentProductToGLAccountMappingHelper;
import org.apache.fineract.currentaccount.service.product.write.impl.CurrentProductWriteServiceImpl;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.currentaccount.service.transaction.read.impl.CurrentTransactionReadServiceImpl;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.service.transaction.write.impl.CurrentTransactionMetadataServiceImpl;
import org.apache.fineract.currentaccount.service.transaction.write.impl.CurrentTransactionWriteServiceImpl;
import org.apache.fineract.currentaccount.statement.service.CurrentCamt053StatementGenerator;
import org.apache.fineract.currentaccount.statement.service.CurrentStatementWriteService;
import org.apache.fineract.currentaccount.validator.account.CurrentAccountDataValidator;
import org.apache.fineract.currentaccount.validator.account.impl.CurrentAccountDataValidatorImpl;
import org.apache.fineract.currentaccount.validator.product.CurrentProductDataValidator;
import org.apache.fineract.currentaccount.validator.product.impl.CurrentProductDataValidatorImpl;
import org.apache.fineract.currentaccount.validator.transaction.CurrentTransactionDataValidator;
import org.apache.fineract.currentaccount.validator.transaction.impl.CurrentTransactionDataValidatorImpl;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.domain.OfficeRepository;
import org.apache.fineract.portfolio.account.domain.AccountIdentifierRepository;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.note.service.NoteWritePlatformService;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.portfolio.transaction.domain.TransactionParamRepository;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.mapper.StatementResponseDataMapper;
import org.apache.fineract.statement.service.ProductStatementWriteService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan("org.apache.fineract.currentaccount")
@ConditionalOnProperty("fineract.module.currentaccount.enabled")
public class CurrentAccountAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentAccountBalanceWriteService.class)
    public CurrentAccountBalanceWriteService currentAccountBalanceWriteService(CurrentAccountRepository currentAccountRepository,
            CurrentAccountBalanceRepository currentAccountBalanceRepository,
            CurrentAccountBalanceReadService currentAccountBalanceReadService) {
        return new CurrentAccountBalanceWriteServiceImpl(currentAccountRepository, currentAccountBalanceRepository,
                currentAccountBalanceReadService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountReadService.class)
    public CurrentAccountReadService currentAccountReadService(CurrentProductRepository currentProductRepository,
            CurrentProductResponseDataMapper currentProductResponseDataMapper, CurrentAccountRepository currentAccountRepository,
            AccountIdentifierRepository accountIdentifierRepository,
            CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper,
            AccountStatementRepository accountStatementRepository, StatementResponseDataMapper statementResponseDataMapper) {
        return new CurrentAccountReadServiceImpl(currentProductRepository, currentProductResponseDataMapper, currentAccountRepository,
                accountIdentifierRepository, currentAccountIdentifiersResponseDataMapper, accountStatementRepository,
                statementResponseDataMapper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountBalanceReadService.class)
    public CurrentAccountBalanceReadService currentAccountBalanceReadService(ConfigurationDomainService configurationService,
            CurrentAccountBalanceRepository currentAccountBalanceRepository, CurrentTransactionRepository currentTransactionRepository,
            CurrentAccountDailyBalanceReadService currentAccountDailyBalanceReadService) {
        return new CurrentAccountBalanceReadServiceImpl(configurationService, currentAccountBalanceRepository, currentTransactionRepository,
                currentAccountDailyBalanceReadService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountWriteService.class)
    public CurrentAccountWriteService currentAccountWriteService(CurrentAccountDataValidator currentAccountDataValidator,
            CurrentAccountAssembler currentAccountAssembler, CurrentAccountRepository currentAccountRepository,
            CurrentAccountAccountingWriteService currentAccountAccountingWriteService) {
        return new CurrentAccountWriteServiceImpl(currentAccountDataValidator, currentAccountAssembler, currentAccountRepository,
                currentAccountAccountingWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountDataValidator.class)
    public CurrentAccountDataValidator currentAccountDataValidator() {
        return new CurrentAccountDataValidatorImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountAssembler.class)
    public CurrentAccountAssembler currentAccountAssembler(ClientRepository clientRepository,
            CurrentProductRepository currentProductRepository, CurrentAccountRepository currentAccountRepository,
            ExternalIdFactory externalIdFactory, CurrentAccountBalanceReadService currentAccountBalanceReadService,
            CurrentAccountBalanceWriteService currentAccountBalanceWriteService, EntityActionRepository entityActionRepository,
            AccountIdentifierRepository accountIdentifierRepository,
            CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper,
            ReadWriteNonCoreDataService readWriteNonCoreDataService, CurrentStatementWriteService currentStatementWriteService,
            NoteWritePlatformService noteWriteService) {
        return new CurrentAccountAssemblerImpl(clientRepository, currentProductRepository, currentAccountRepository, externalIdFactory,
                currentAccountBalanceReadService, currentAccountBalanceWriteService, entityActionRepository, accountIdentifierRepository,
                currentAccountIdentifiersResponseDataMapper, readWriteNonCoreDataService, currentStatementWriteService, noteWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductReadService.class)
    public CurrentProductReadService currentProductReadService(CurrentProductRepository currentProductRepository,
            CurrencyReadPlatformService currencyReadPlatformService, CurrentProductResponseDataMapper currentProductResponseDataMapper,
            ProductToGLAccountMappingRepository productToGLAccountMappingRepository,
            PaymentTypeReadPlatformService paymentTypeReadPlatformService, ConfigurationDomainService configurationDomainService,
            GLAccountRepository glAccountRepository, ProductStatementRepository productStatementRepository,
            StatementResponseDataMapper statementResponseDataMapper) {
        return new CurrentProductReadServiceImpl(currentProductRepository, currencyReadPlatformService, currentProductResponseDataMapper,
                productToGLAccountMappingRepository, paymentTypeReadPlatformService, configurationDomainService, glAccountRepository,
                productStatementRepository, statementResponseDataMapper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductDataValidator.class)
    public CurrentProductDataValidator currentProductDataValidator() {
        return new CurrentProductDataValidatorImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductAssembler.class)
    public CurrentProductAssembler currentProductAssembler(ExternalIdFactory externalIdFactory,
            CurrentProductRepository currentProductRepository, ReadWriteNonCoreDataService readWriteNonCoreDataService,
            CurrentProductToGLAccountMappingHelper currentProductToGLAccountMappingHelper,
            ProductStatementWriteService productStatementWriteService) {
        return new CurrentProductAssemblerImpl(externalIdFactory, currentProductRepository, readWriteNonCoreDataService,
                currentProductToGLAccountMappingHelper, productStatementWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductWriteService.class)
    public CurrentProductWriteService currentProductWriteService(CurrentProductRepository currentProductRepository,
            CurrentAccountRepository currentAccountRepository, CurrentProductDataValidator currentProductDataValidator,
            CurrentProductAssembler currentProductAssembler) {
        return new CurrentProductWriteServiceImpl(currentProductRepository, currentAccountRepository, currentProductDataValidator,
                currentProductAssembler);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionReadService.class)
    public CurrentTransactionReadService currentTransactionReadService(CurrentAccountReadService currentAccountReadService,
            PaymentTypeReadPlatformService paymentTypeReadPlatformService, CurrentTransactionRepository currentTransactionRepository) {
        return new CurrentTransactionReadServiceImpl(currentAccountReadService, paymentTypeReadPlatformService,
                currentTransactionRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionWriteService.class)
    public CurrentTransactionWriteService currentTransactionWriteService(CurrentTransactionDataValidator currentTransactionDataValidator,
            CurrentTransactionAssembler currentTransactionAssembler, CurrentTransactionRepository currentTransactionRepository,
            CurrentAccountRepository currentAccountRepository) {
        return new CurrentTransactionWriteServiceImpl(currentTransactionDataValidator, currentTransactionAssembler,
                currentTransactionRepository, currentAccountRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionAssembler.class)
    public CurrentTransactionAssembler currentTransactionAssembler(ExternalIdFactory externalIdFactory,
            ReadWriteNonCoreDataService readWriteNonCoreDataService, CurrentProductRepository currentProductRepository,
            CurrentTransactionRepository currentTransactionRepository, CurrentAccountBalanceReadService currentAccountBalanceReadService,
            CurrentAccountBalanceWriteService currentAccountBalanceWriteService, NoteWritePlatformService noteWriteService) {
        return new CurrentTransactionAssemblerImpl(externalIdFactory, readWriteNonCoreDataService, currentProductRepository,
                currentTransactionRepository, currentAccountBalanceReadService, currentAccountBalanceWriteService, noteWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionDataValidator.class)
    public CurrentTransactionDataValidator currentTransactionDataValidator() {
        return new CurrentTransactionDataValidatorImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductToGLAccountMappingHelper.class)
    public CurrentProductToGLAccountMappingHelper currentProductToGLAccountMappingHelper(
            ProductToGLAccountMappingRepository accountMappingRepository, ProductToGLAccountMappingHelper productToGLAccountMappingHelper,
            PaymentTypeRepositoryWrapper paymentTypeRepositoryWrapper) {
        return new CurrentProductToGLAccountMappingHelper(accountMappingRepository, productToGLAccountMappingHelper,
                paymentTypeRepositoryWrapper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountAccountingWriteService.class)
    public CurrentAccountAccountingWriteService currentAccountAccountingWriteService(CurrentAccountRepository currentAccountRepository,
            CurrentTransactionRepository currentTransactionRepository,
            CurrentAccountAccountingRepository currentAccountAccountingRepository,
            FinancialActivityAccountRepositoryWrapper financialActivityAccountRepository,
            ProductToGLAccountMappingRepository accountMappingRepository, OfficeRepository officeRepository,
            JournalEntryRepository glJournalEntryRepository) {
        return new CurrentAccountAccountingWriteServiceImpl(currentAccountRepository, currentTransactionRepository,
                currentAccountAccountingRepository, financialActivityAccountRepository, accountMappingRepository, officeRepository,
                glJournalEntryRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountDailyBalanceReadService.class)
    public CurrentAccountDailyBalanceReadService currentAccountDailyBalanceReadService(
            CurrentAccountDailyBalanceRepository dailyBalanceRepository, CurrentTransactionRepository currentTransactionRepository) {
        return new CurrentAccountDailyBalanceReadServiceImpl(dailyBalanceRepository, currentTransactionRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountDailyBalanceWriteService.class)
    public CurrentAccountDailyBalanceWriteService currentAccountDailyBalanceWriteService(CurrentAccountRepository currentAccountRepository,
            CurrentAccountDailyBalanceRepository dailyBalanceRepository,
            CurrentAccountDailyBalanceReadService currentAccountDailyBalanceReadService) {
        return new CurrentAccountDailyBalanceWriteServiceImpl(currentAccountRepository, dailyBalanceRepository,
                currentAccountDailyBalanceReadService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentCamt053StatementGenerator.class)
    public CurrentCamt053StatementGenerator currentCamt053StatementGenerator(CurrentAccountRepository currentAccountRepository,
            AccountIdentifierRepository accountIdentifierRepository, CurrentTransactionRepository transactionRepository,
            CurrentAccountDailyBalanceReadService dailyBalanceReadService, CurrentTransactionMetadataService transactionMetadataService) {
        return new CurrentCamt053StatementGenerator(currentAccountRepository, accountIdentifierRepository, transactionRepository,
                dailyBalanceReadService, transactionMetadataService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionMetadataService.class)
    public CurrentTransactionMetadataService currentTransactionMetadataService(CurrentAccountRepository currentAccountRepository,
                                                                                    CurrentTransactionRepository currentTransactionRepository, TransactionParamRepository transactionParamRepository) {
        return new CurrentTransactionMetadataServiceImpl(currentAccountRepository, currentTransactionRepository,
                transactionParamRepository);
    }
}
