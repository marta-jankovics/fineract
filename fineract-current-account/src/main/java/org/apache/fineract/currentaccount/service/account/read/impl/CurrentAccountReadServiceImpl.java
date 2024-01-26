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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountIdentifiersResponseDataMapper;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountBalanceReadService;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@RequiredArgsConstructor
public class CurrentAccountReadServiceImpl implements CurrentAccountReadService {

    private final CurrentAccountRepository currentAccountRepository;
    private final AccountIdentifierRepository accountIdentifierRepository;
    private final CurrentAccountBalanceReadService currentAccountBalanceReadService;
    private final CurrentProductReadService currentProductReadPlatformService;
    private final CurrentAccountResponseDataMapper currentAccountResponseDataMapper;
    private final CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper;

    @Override
    public Page<CurrentAccountResponseData> retrieveAll(Pageable pageable) {
        return currentAccountResponseDataMapper.map(currentAccountRepository.findAllCurrentAccountData(pageable),
                currentAccountBalanceReadService::getBalance);
    }

    @Override
    public CurrentAccountResponseData retrieveById(final String accountId) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountDataByExternalId(accountId);
        if (currentAccountData == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId);
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
    public List<CurrentAccountResponseData> retrieveAllByClientId(Long clientId, Sort sort) {
        return currentAccountResponseDataMapper.map(currentAccountRepository.findAllByClientId(clientId, sort),
                currentAccountBalanceReadService::getBalance);
    }

    @Override
    public CurrentAccountResponseData retrieveByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier) {
        String reformatIdType = reformatIdType(idType);
        CurrentAccountIdType currentAccountIdType = resolveCurrentAccountIdType(reformatIdType);
        if (currentAccountIdType == null) {
            String accountId = resolveIdByIdTypeAndIdentifier(reformatIdType, idType, identifier, subIdentifier);
            return retrieveById(accountId);
        }
        return switch (currentAccountIdType) {
            case ID -> retrieveById(identifier);
            case EXTERNAL_ID -> retrieveByExternalId(new ExternalId(identifier));
            case ACCOUNT_NUMBER -> retrieveByAccountNumber(identifier);
        };
    }

    @Override
    public String retrieveIdByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier) {
        return getAccountIdByIdTypeAndIdentifier(idType, identifier, subIdentifier);
    }

    @Override
    public CurrentAccountIdentifiersResponseData retrieveIdentifiersById(String accountId) {
        CurrentAccountIdentifiersData currentAccountIdentifiersData = currentAccountRepository.retrieveIdentifiers(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        List<AccountIdentifier> extraSecondaryIdentifiers = accountIdentifierRepository
                .retrieveAccountIdentifiers(PortfolioAccountType.CURRENT, accountId);
        return currentAccountIdentifiersResponseDataMapper.map(currentAccountIdentifiersData, extraSecondaryIdentifiers);
    }

    @Override
    public CurrentAccountIdentifiersResponseData retrieveIdentifiersByIdTypeAndIdentifier(String idType, String identifier,
            String subIdentifier) {
        String accountId = getAccountIdByIdTypeAndIdentifier(idType, identifier, subIdentifier);
        return retrieveIdentifiersById(accountId);
    }

    private String resolveIdByIdTypeAndIdentifier(String reformatIdType, String idType, String identifier, String subIdentifier) {
        InteropIdentifierType interopIdentifierType = null;
        String accountId = null;
        try {
            interopIdentifierType = InteropIdentifierType.valueOf(reformatIdType);
        } catch (IllegalArgumentException e) {
            // No need to throw error we handle it later
        }
        if (interopIdentifierType != null) {
            accountId = accountIdentifierRepository.fetchAccountIdByIdTypeAndId(PortfolioAccountType.CURRENT, interopIdentifierType,
                    identifier, subIdentifier);
        }
        if (accountId == null) {
            if (subIdentifier == null) {
                throw new PlatformResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s cannot be found", idType, identifier);
            } else {
                throw new PlatformResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s and sub value: %s cannot be found", idType, identifier,
                        subIdentifier);
            }
        }
        return accountId;
    }

    private String getAccountIdByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier) {
        String reformatIdType = reformatIdType(idType);
        CurrentAccountIdType currentAccountIdType = resolveCurrentAccountIdType(reformatIdType);

        if (currentAccountIdType == null) {
            return resolveIdByIdTypeAndIdentifier(reformatIdType, idType, identifier, subIdentifier);
        } else {
            return switch (currentAccountIdType) {
                case ID -> identifier;
                case EXTERNAL_ID -> currentAccountRepository.findIdByExternalId(new ExternalId(identifier))
                        .orElseThrow(() -> new PlatformResourceNotFoundException("current.account",
                                "Current account with external id: %s cannot be found", identifier));
                case ACCOUNT_NUMBER -> currentAccountRepository.findIdByAccountNumber(identifier)
                        .orElseThrow(() -> new PlatformResourceNotFoundException("current.account",
                                "Current account with account number: %s cannot be found", identifier));
            };
        }
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

    private CurrentAccountIdType resolveCurrentAccountIdType(String reformatIdType) {
        try {
            return CurrentAccountIdType.valueOf(reformatIdType);
        } catch (IllegalArgumentException e) {
            // No need to throw error, we are going to check in the secondary identifiers
        }
        return null;
    }
}
