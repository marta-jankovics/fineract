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
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.repository.accounting.CurrentAccountAccountingRepository;
import org.apache.fineract.currentaccount.repository.entityaction.EntityActionRepository;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.account.read.impl.CurrentAccountBalanceReadServiceImpl;
import org.apache.fineract.currentaccount.service.account.read.impl.CurrentAccountReadServiceImpl;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountBalanceWriteService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceWriteService;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountWriteService;
import org.apache.fineract.currentaccount.service.account.write.impl.CurrentAccountBalanceWriteServiceImpl;
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
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.currentaccount.service.transaction.write.impl.CurrentTransactionWriteServiceImpl;
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
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
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
    public CurrentAccountBalanceWriteService currentAccountBalanceWriteService(
            CurrentAccountBalanceRepository currentAccountBalanceRepository,
            CurrentAccountBalanceReadService currentAccountBalanceReadService) {
        return new CurrentAccountBalanceWriteServiceImpl(currentAccountBalanceRepository, currentAccountBalanceReadService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountReadService.class)
    public CurrentAccountReadService currentAccountReadService(CurrentAccountRepository currentAccountRepository,
            AccountIdentifierRepository accountIdentifierRepository, CurrentProductReadService currentProductReadPlatformService,
            CurrentProductResponseDataMapper currentProductResponseDataMapper,
            CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper) {
        return new CurrentAccountReadServiceImpl(currentAccountRepository, accountIdentifierRepository, currentProductReadPlatformService,
                currentAccountIdentifiersResponseDataMapper, currentProductResponseDataMapper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountBalanceReadService.class)
    public CurrentAccountBalanceReadService currentAccountBalanceReadService(ConfigurationDomainService configurationService,
            CurrentAccountBalanceRepository currentAccountBalanceRepository, CurrentTransactionRepository currentTransactionRepository) {
        return new CurrentAccountBalanceReadServiceImpl(configurationService, currentAccountBalanceRepository,
                currentTransactionRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountWriteService.class)
    public CurrentAccountWriteService currentAccountWriteService(CurrentAccountDataValidator currentAccountDataValidator,
            CurrentAccountAssembler currentAccountAssembler, CurrentAccountRepository currentAccountRepository,
            ClientRepository clientRepository, CurrentAccountAccountingWriteService currentAccountAccountingWriteService) {
        return new CurrentAccountWriteServiceImpl(currentAccountDataValidator, currentAccountAssembler, currentAccountRepository,
                clientRepository, currentAccountAccountingWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountDataValidator.class)
    public CurrentAccountDataValidator currentAccountDataValidator() {
        return new CurrentAccountDataValidatorImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CurrentAccountAssembler.class)
    public CurrentAccountAssembler currentAccountAssembler(ClientRepository clientRepository,
            CurrentProductRepository currentProductRepository, ExternalIdFactory externalIdFactory,
            AccountIdentifierRepository accountIdentifierRepository, CurrentAccountRepository currentAccountRepository,
            CurrentAccountBalanceWriteService currentAccountBalanceWriteService, EntityActionRepository entityActionRepository,
            ReadWriteNonCoreDataService readWriteNonCoreDataService,
            CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper) {
        return new CurrentAccountAssemblerImpl(clientRepository, currentProductRepository, currentAccountRepository, entityActionRepository,
                accountIdentifierRepository, currentAccountBalanceWriteService, externalIdFactory, readWriteNonCoreDataService,
                currentAccountIdentifiersResponseDataMapper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductReadService.class)
    public CurrentProductReadService currentProductReadService(CurrentProductRepository currentProductRepository,
            CurrencyReadPlatformService currencyReadPlatformService, CurrentProductResponseDataMapper currentProductResponseDataMapper,
            ProductToGLAccountMappingRepository productToGLAccountMappingRepository) {
        return new CurrentProductReadServiceImpl(currentProductRepository, currencyReadPlatformService, currentProductResponseDataMapper,
                productToGLAccountMappingRepository);
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
            CurrentProductToGLAccountMappingHelper currentProductToGLAccountMappingHelper) {
        return new CurrentProductAssemblerImpl(externalIdFactory, currentProductRepository, readWriteNonCoreDataService,
                currentProductToGLAccountMappingHelper);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentProductWriteService.class)
    public CurrentProductWriteService currentProductWriteService(CurrentProductRepository currentProductRepository,
            CurrentProductDataValidator currentProductDataValidator, CurrentProductAssembler currentProductAssembler) {
        return new CurrentProductWriteServiceImpl(currentProductRepository, currentProductDataValidator, currentProductAssembler);
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
            CurrentTransactionAssembler currentTransactionAssembler, CurrentAccountRepository currentAccountRepository,
            CurrentTransactionRepository currentTransactionRepository, CurrentAccountBalanceReadService currentAccountBalanceReadService,
            CurrentAccountBalanceWriteService currentAccountBalanceWriteService, CurrentAccountWriteService currentAccountWriteService) {
        return new CurrentTransactionWriteServiceImpl(currentTransactionDataValidator, currentTransactionAssembler,
                currentAccountRepository, currentTransactionRepository, currentAccountBalanceReadService, currentAccountBalanceWriteService,
                currentAccountWriteService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentTransactionAssembler.class)
    public CurrentTransactionAssembler currentTransactionAssembler(ExternalIdFactory externalIdFactory,
            ReadWriteNonCoreDataService readWriteNonCoreDataService, CurrentProductRepository currentProductRepository,
            CurrentTransactionRepository currentTransactionRepository) {
        return new CurrentTransactionAssemblerImpl(externalIdFactory, readWriteNonCoreDataService, currentProductRepository,
                currentTransactionRepository);
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
    @ConditionalOnMissingBean(CurrentAccountDailyBalanceWriteService.class)
    public CurrentAccountDailyBalanceWriteService currentAccountDailyBalanceWriteService(
            CurrentAccountDailyBalanceRepository dailyBalanceRepository, CurrentTransactionRepository currentTransactionRepository) {
        return new CurrentAccountDailyBalanceWriteServiceImpl(dailyBalanceRepository, currentTransactionRepository);
    }
}
