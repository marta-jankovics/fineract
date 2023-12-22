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
package org.apache.fineract.currentaccount.service.product.read;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingDropdownReadPlatformService;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.exception.product.CurrentProductNotFoundException;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
@Slf4j
public class CurrentProductReadServiceImpl implements CurrentProductReadService {

    private final CurrentProductRepository currentProductRepository;
    private final CurrentProductResponseDataMapper currentProductResponseDataMapper;
    private final CurrencyReadPlatformService currencyReadPlatformService;
    private final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService;

    @Override
    public List<CurrentProductResponseData> retrieveAll(Sort sort) {
        return currentProductResponseDataMapper.map(currentProductRepository.findAllSorted(sort));
    }

    @Override
    public Page<CurrentProductResponseData> retrieveAll(Pageable pageable) {
        return currentProductResponseDataMapper.map(currentProductRepository.findAllCurrentProductData(pageable));
    }

    @Override
    public CurrentProductResponseData retrieveById(Long productId) {
        CurrentProductData currentProductData = currentProductRepository.findCurrentProductData(productId);
        if (currentProductData == null) {
            throw new CurrentProductNotFoundException(productId);
        }
        return currentProductResponseDataMapper.map(currentProductData);
    }

    @Override
    public CurrentProductTemplateResponseData retrieveTemplate() {
        final List<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();
        final List<EnumOptionData> accountingRuleOptions = this.accountingDropdownReadPlatformService.retrieveAccountingRuleTypeOptions();

        return new CurrentProductTemplateResponseData(currencyOptions, accountingRuleOptions);
    }
}
