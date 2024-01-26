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
package org.apache.fineract.currentaccount.service.account.read.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
public class CurrentAccountReadServiceImpl implements CurrentAccountReadService {

    private final CurrentAccountRepository currentAccountRepository;
    private final AccountIdentifierRepository accountIdentifierRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final CurrentProductReadService currentProductReadPlatformService;
    private final CurrentAccountResponseDataMapper currentAccountResponseDataMapper;

    @Override
    public Page<CurrentAccountResponseData> retrieveAll(Pageable pageable) {
        return currentAccountResponseDataMapper.map(currentAccountRepository.findAllCurrentAccountData(pageable),
                currentAccountBalanceReadService::getBalance);
    }

    @Override
    public CurrentAccountResponseData retrieveById(final String accountId) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountDataByExternalId(accountId);
        if (currentAccountData == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found",
                    accountId);
        }
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(accountId);
        return currentAccountResponseDataMapper.map(currentAccountData, currentAccountBalanceData);
    }

    @Override
    public CurrentAccountTemplateResponseData retrieveTemplate() {
        final List<CurrentProductResponseData> productOptions = this.currentProductReadPlatformService
                .retrieveAll(Sort.by(Sort.Direction.ASC, "name"));

        return CurrentAccountTemplateResponseData.builder() //
                .productOptions(productOptions) //
                .build(); //
    }

    @Override
    public String retrieveAccountIdByExternalId(ExternalId accountExternalId) {
        String id = currentAccountRepository.findIdByExternalId(accountExternalId);
        if (id == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with external id: %s cannot be found",
                    accountExternalId);
        }
        return id;
    }

    @Override
    public List<CurrentAccountResponseData> retrieveAllByClientId(Long clientId, Sort sort) {
        return currentAccountResponseDataMapper.map(currentAccountRepository.findAllByClientId(clientId, sort),
                currentAccountBalanceReadService::getBalance);
    }

    @Override
    public CurrentAccountResponseData retrieveByIdTypeAndId(String idType, String id, String subId) {
        String reformatIdType = reformatIdType(idType);
        CurrentAccountIdType currentAccountIdType = null;
        try {
            currentAccountIdType = CurrentAccountIdType.valueOf(reformatIdType);
        } catch (IllegalArgumentException e) {
            // No need to throw error, we are going to check in the secondary identifiers
        }

        if (currentAccountIdType == null) {
            String accountId = accountIdentifierRepository.fetchAccountIdByIdTypeAndId(PortfolioAccountType.CURRENT, idType, id, subId);
            if (accountId == null) {
                throw new PlatformResourceNotFoundException("current.account", "Current account with secondary identifier: %s and value: %s cannot be found",
                        idType, id);
            }
            return retrieveById(accountId);
        }
        return switch (currentAccountIdType) {
            case ID -> retrieveById(id);
            case EXTERNAL_ID -> retrieveByExternalId(new ExternalId(id));
            case ACCOUNT_NUMBER -> retrieveByAccountNumber(id);
        };
    }

    private CurrentAccountResponseData retrieveByAccountNumber(String accountNumber) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountDataByAccountNumber(accountNumber);
        if (currentAccountData == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with account number: %s cannot be found",
                    accountNumber);
        }
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(currentAccountData.getId());
        return currentAccountResponseDataMapper.map(currentAccountData, currentAccountBalanceData);
    }

    private CurrentAccountResponseData retrieveByExternalId(ExternalId externalId) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountDataByExternalId(externalId);
        if (currentAccountData == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with external id: %s cannot be found",
                    externalId);
        }
        CurrentAccountBalanceData currentAccountBalanceData = currentAccountBalanceReadService.getBalance(currentAccountData.getId());
        return currentAccountResponseDataMapper.map(currentAccountData, currentAccountBalanceData);
    }

    private String reformatIdType(String idType) {
        return idType != null ? idType.replaceAll("-", "_").toUpperCase() : null;
    }
}
