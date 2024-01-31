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

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.currentaccount.data.account.IdentifiersResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.mapper.account.CurrentAccountIdentifiersResponseDataMapper;
import org.apache.fineract.currentaccount.mapper.product.CurrentProductResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.currentaccount.service.account.CurrentAccountIdTypeResolver;
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
    private final CurrentProductReadService currentProductReadPlatformService;
    private final CurrentAccountIdentifiersResponseDataMapper currentAccountIdentifiersResponseDataMapper;
    private final CurrentProductResponseDataMapper productResponseDataMapper;

    @Override
    public CurrentAccountTemplateResponseData retrieveTemplate() {
        final List<CurrentProductData> productOptions = this.currentProductReadPlatformService
                .retrieveAll(Sort.by(Sort.Direction.ASC, "name"));

        return CurrentAccountTemplateResponseData.builder() //
                .productOptions(productResponseDataMapper.map(productOptions)) //
                .build(); //
    }

    @Override
    public Page<CurrentAccountData> retrieveAll(Pageable pageable) {
        return currentAccountRepository.findAllCurrentAccountData(pageable);
    }

    @Override
    public List<CurrentAccountData> retrieveAllByClientId(@NotNull Long clientId, Sort sort) {
        return currentAccountRepository.findAllByClientId(clientId, sort);
    }

    @Override
    public CurrentAccountData retrieveByIdTypeAndIdentifier(@NotNull CurrentAccountIdTypeResolver idType, String identifier,
            String subIdentifier) {
        CurrentAccountIdType currentType = idType.getCurrentType();
        if (idType.isSecondaryIdentifier()) {
            currentType = CurrentAccountIdType.ID;
            identifier = resolveIdBySecondaryIdentifier(idType, identifier, subIdentifier);
        }

        CurrentAccountData accountData = switch (currentType) {
            case ID -> currentAccountRepository.findCurrentAccountDataById(identifier);
            case EXTERNAL_ID -> currentAccountRepository.findCurrentAccountDataByExternalId(new ExternalId(identifier));
            case ACCOUNT_NUMBER -> currentAccountRepository.findCurrentAccountDataByAccountNumber(identifier);
        };
        if (accountData == null) {
            throw new PlatformResourceNotFoundException("current.account", "Current account with %s: %s cannot be found", currentType,
                    identifier);
        }

        return accountData;
    }

    @Override
    public String retrieveIdByIdTypeAndIdentifier(@NotNull CurrentAccountIdTypeResolver idType, String identifier, String subIdentifier) {
        if (idType.isSecondaryIdentifier()) {
            return resolveIdBySecondaryIdentifier(idType, identifier, subIdentifier);
        }
        CurrentAccountIdType currentType = idType.getCurrentType();
        return switch (currentType) {
            case ID -> identifier;
            case EXTERNAL_ID -> currentAccountRepository.findIdByExternalId(new ExternalId(identifier))
                    .orElseThrow(() -> new PlatformResourceNotFoundException("current.account",
                            "Current account with external id: %s cannot be found", identifier));
            case ACCOUNT_NUMBER -> currentAccountRepository.findIdByAccountNumber(identifier)
                    .orElseThrow(() -> new PlatformResourceNotFoundException("current.account",
                            "Current account with account number: %s cannot be found", identifier));
        };
    }

    @Override
    public IdentifiersResponseData retrieveIdentifiersByIdTypeAndIdentifier(@NotNull CurrentAccountIdTypeResolver idType, String identifier,
            String subIdentifier) {
        String accountId = retrieveIdByIdTypeAndIdentifier(idType, identifier, subIdentifier);
        CurrentAccountIdentifiersData currentAccountIdentifiersData = currentAccountRepository.retrieveIdentifiers(accountId).orElseThrow(
                () -> new PlatformResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        List<AccountIdentifier> extraSecondaryIdentifiers = accountIdentifierRepository
                .retrieveAccountIdentifiers(PortfolioAccountType.CURRENT, accountId);
        return currentAccountIdentifiersResponseDataMapper.map(currentAccountIdentifiersData, extraSecondaryIdentifiers);
    }

    private String resolveIdBySecondaryIdentifier(@NotNull CurrentAccountIdTypeResolver idType, String identifier, String subIdentifier) {
        InteropIdentifierType secondaryType = idType.getInteropType();
        String accountId = accountIdentifierRepository.fetchAccountIdByIdTypeAndId(PortfolioAccountType.CURRENT, secondaryType, identifier,
                subIdentifier);
        if (accountId == null) {
            if (subIdentifier == null) {
                throw new PlatformResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s cannot be found", secondaryType, identifier);
            } else {
                throw new PlatformResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s and sub value: %s cannot be found", secondaryType,
                        identifier, subIdentifier);
            }
        }
        return accountId;
    }
}
