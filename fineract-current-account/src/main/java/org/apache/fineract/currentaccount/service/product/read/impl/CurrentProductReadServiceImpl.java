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
package org.apache.fineract.currentaccount.service.product.read.impl;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMappingRepository;
import org.apache.fineract.currentaccount.data.accounting.GLAccountDetailsData;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductCashBasedAccount;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.product.CurrentProductResolver;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.statement.data.dto.ProductStatementTemplateData;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.domain.StatementBatchType;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.mapper.StatementResponseDataMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
@Slf4j
public class CurrentProductReadServiceImpl implements CurrentProductReadService {

    private static final List<AccountingRuleType> ALLOWED_ACCOUNTING_RULE_TYPES = List.of(AccountingRuleType.NONE,
            AccountingRuleType.CASH_BASED);

    private final CurrentProductRepository currentProductRepository;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final CurrentProductResponseDataMapper currentProductResponseDataMapper;
    private final ProductToGLAccountMappingRepository productToGLAccountMappingRepository;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final GLAccountRepository glAccountRepository;
    private final ProductStatementRepository productStatementRepository;
    private final StatementResponseDataMapper statementResponseDataMapper;

    @Override
    public CurrentProductTemplateResponseData retrieveTemplate() {
        final List<CurrencyData> currencyOptions = currencyReadPlatformService.retrieveAllowedCurrencies();
        final List<StringEnumOptionData> accountingRuleOptions = Arrays.stream(AccountingRuleType.values())
                .filter(ALLOWED_ACCOUNTING_RULE_TYPES::contains)
                .map(art -> new StringEnumOptionData(art.name(), art.getCode(), art.getDescription())).toList();
        List<GLAccount> glAccountsData = glAccountRepository.getAllEnabledDetailAccounts();
        Map<String, List<GLAccountDetailsData>> accountingMappingOptions = glAccountsData.stream()
                .map(account -> new GLAccountDetailsData(account.getId(), account.getName(), account.getGlCode(),
                        GLAccountType.fromInt(account.getType()).name()))
                .collect(Collectors.groupingBy(GLAccountDetailsData::getType));

        final List<PaymentTypeData> paymentTypeOptions = paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        final String accountMappingForPayment = configurationDomainService.getAccountMappingForPaymentType();
        final List<StringEnumOptionData> statementTypeOptions = Arrays.stream(StatementType.VALUES)
                .map(StatementType::toStringEnumOptionData).toList();
        final List<StringEnumOptionData> publishTypeOptions = Arrays.stream(StatementPublishType.VALUES)
                .map(StatementPublishType::toStringEnumOptionData).toList();
        final List<StringEnumOptionData> batchTypeOptions = Arrays.stream(StatementBatchType.VALUES)
                .map(StatementBatchType::toStringEnumOptionData).toList();
        ProductStatementTemplateData statementTemplate = new ProductStatementTemplateData(statementTypeOptions, publishTypeOptions,
                batchTypeOptions);

        return new CurrentProductTemplateResponseData(currencyOptions, accountingRuleOptions,
                Map.of(AccountingRuleType.CASH_BASED.name(), Stream.of(CurrentProductCashBasedAccount.values())
                        .map(CurrentProductCashBasedAccount::toGLStringEnumOptionData).toList()),
                accountingMappingOptions, paymentTypeOptions, accountMappingForPayment, statementTemplate);
    }

    @Override
    public Page<CurrentProductResponseData> retrieveAll(Pageable pageable) {
        return currentProductResponseDataMapper.map(currentProductRepository.getProductsDataPage(pageable));
    }

    @Override
    public CurrentProductResponseData retrieve(@NotNull CurrentProductResolver productResolver) {
        CurrentProductData productData = switch (productResolver.getIdType()) {
            case ID -> currentProductRepository.getProductDataById(productResolver.getIdentifier());
            case EXTERNAL_ID -> currentProductRepository.getProductDataByExternalId(new ExternalId(productResolver.getIdentifier()));
            case SHORT_NAME -> currentProductRepository.getProductDataByShortName(productResolver.getIdentifier());
        };
        if (productData == null) {
            throw new ResourceNotFoundException("current.product", "Current product with %s: %s cannot be found",
                    productResolver.getIdType(), productResolver.getIdentifier());
        }
        return currentProductResponseDataMapper.map(productData,
                (CurrentProductData product) -> productToGLAccountMappingRepository.findByProductIdentifierAndProductType(product.getId(),
                        PortfolioProductType.CURRENT.getValue()),
                (CurrentProductData product) -> statementResponseDataMapper.mapProductStatements(
                        productStatementRepository.getByProductIdAndProductType(product.getId(), PortfolioProductType.CURRENT)));
    }

    @Override
    public String retrieveId(@NotNull CurrentProductResolver productResolver) {
        return switch (productResolver.getIdType()) {
            case ID -> productResolver.getIdentifier();
            case EXTERNAL_ID -> currentProductRepository.getIdByExternalId(new ExternalId(productResolver.getIdentifier()));
            case SHORT_NAME -> currentProductRepository.getIdByShortName(productResolver.getIdentifier());
        };
    }
}
