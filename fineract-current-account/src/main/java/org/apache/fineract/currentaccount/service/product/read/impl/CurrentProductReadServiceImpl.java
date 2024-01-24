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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingDropdownReadPlatformService;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductIdType;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

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
    public CurrentProductResponseData retrieveById(UUID productId) {
        CurrentProductData currentProductData = currentProductRepository.findCurrentProductDataById(productId);
        if (currentProductData == null) {
            throw new PlatformResourceNotFoundException("current.product", "Current product with provided id: %s cannot be found", productId);
        }
        return currentProductResponseDataMapper.map(currentProductData);
    }

    @Override
    public CurrentProductTemplateResponseData retrieveTemplate() {
        final List<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();
        final List<EnumOptionData> accountingRuleOptions = this.accountingDropdownReadPlatformService.retrieveAccountingRuleTypeOptions();

        return new CurrentProductTemplateResponseData(currencyOptions, accountingRuleOptions);
    }

    @Override
    public CurrentProductResponseData retrieveByIdTypeAndId(String idType, String id) {
        String reformatIdType = reformatIdType(idType);
        CurrentProductIdType currentProductIdType;
        try {
            currentProductIdType = CurrentProductIdType.valueOf(reformatIdType);
        } catch (IllegalArgumentException e) {
            throw new PlatformApiDataValidationException("error.msg.id.type.not.found", "Provided id type is not supported", "idType", e, idType);
        }
        return switch (currentProductIdType) {
            case ID -> retrieveById(UUID.fromString(id));
            case EXTERNAL_ID -> retrieveByExternalId(new ExternalId(id));
            case SHORT_NAME -> retrieveByShortName(id);
        };

    }

    private String reformatIdType(String idType) {
        return idType != null ? idType.replaceAll("-", "_").toUpperCase() : null;
    }


    private CurrentProductResponseData retrieveByExternalId(ExternalId externalId) {
        CurrentProductData currentProductData = currentProductRepository.findCurrentProductDataByExternalId(externalId);
        if (currentProductData == null) {
            throw new PlatformResourceNotFoundException("current.product", "Current product with provided external id: %s cannot be found", externalId);
        }
        return currentProductResponseDataMapper.map(currentProductData);
    }


    private CurrentProductResponseData retrieveByShortName(String shortName) {
        CurrentProductData currentProductData = currentProductRepository.findCurrentProductDataByShortName(shortName);
        if (currentProductData == null) {
            throw new PlatformResourceNotFoundException("current.product", "Current product with provided short name: %s cannot be found", shortName);
        }
        return currentProductResponseDataMapper.map(currentProductData);
    }
}
