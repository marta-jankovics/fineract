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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductIdType;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
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

    @Override
    public CurrentProductTemplateResponseData retrieveTemplate() {
        final List<CurrencyData> currencyOptions = currencyReadPlatformService.retrieveAllowedCurrencies();
        final List<StringEnumOptionData> accountingRuleOptions = Arrays.stream(AccountingRuleType.values())
                .map(art -> new StringEnumOptionData(art.name(), art.getCode(), art.getDescription())).toList();

        return new CurrentProductTemplateResponseData(currencyOptions, accountingRuleOptions);
    }

    @Override
    public List<CurrentProductResponseData> retrieveAll(Sort sort) {
        return currentProductResponseDataMapper.map(currentProductRepository.findAllSorted(sort));
    }

    @Override
    public Page<CurrentProductResponseData> retrieveAll(Pageable pageable) {
        return currentProductResponseDataMapper.map(currentProductRepository.findAllCurrentProductData(pageable));
    }

    @Override
    public CurrentProductResponseData retrieveByIdTypeAndIdentifier(@NotNull CurrentProductIdType idType, String identifier) {
        CurrentProductData productData = switch (idType) {
            case ID -> currentProductRepository.findCurrentProductDataById(identifier);
            case EXTERNAL_ID -> currentProductRepository.findCurrentProductDataByExternalId(new ExternalId(identifier));
            case SHORT_NAME -> currentProductRepository.findCurrentProductDataByShortName(identifier);
        };
        if (productData == null) {
            throw new PlatformResourceNotFoundException("current.product", "Current product with %s: %s cannot be found", idType,
                    identifier);
        }
        return currentProductResponseDataMapper.map(productData);
    }

    @Override
    public String retrieveIdByIdTypeAndIdentifier(@NotNull CurrentProductIdType idType, String identifier) {
        return switch (idType) {
            case ID -> identifier;
            case EXTERNAL_ID -> currentProductRepository.findIdByExternalId(new ExternalId(identifier));
            case SHORT_NAME -> currentProductRepository.findIdByShortName(identifier);
        };
    }
}
