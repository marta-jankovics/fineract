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
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.product.read.CurrentProductReadService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
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
        return currentAccountRepository.getAccountsDataPage(pageable);
    }

    @Override
    public List<CurrentAccountData> retrieveAllByClientId(@NotNull Long clientId, Sort sort) {
        return currentAccountRepository.getAccountsDataByClientId(clientId, sort);
    }

    @Override
    public CurrentAccountData retrieve(@NotNull CurrentAccountResolver accountResolver) {
        CurrentAccountIdType currentType = accountResolver.getIdType();
        String accountIdentifier = accountResolver.getIdentifier();
        if (accountResolver.isSecondaryIdentifier()) {
            currentType = CurrentAccountIdType.ID;
            accountIdentifier = resolveIdBySecondaryIdentifier(accountResolver);
        }

        CurrentAccountData accountData = switch (currentType) {
            case ID -> currentAccountRepository.getAccountDataById(accountIdentifier);
            case EXTERNAL_ID -> currentAccountRepository.getAccountDataByExternalId(new ExternalId(accountIdentifier));
            case ACCOUNT_NUMBER -> currentAccountRepository.getAccountDataByAccountNumber(accountIdentifier);
        };
        if (accountData == null) {
            throw new ResourceNotFoundException("current.account", "Current account with %s: %s cannot be found", currentType,
                    accountIdentifier);
        }

        return accountData;
    }

    @Override
    public String retrieveId(@NotNull CurrentAccountResolver accountResolver) {
        if (accountResolver.isSecondaryIdentifier()) {
            return resolveIdBySecondaryIdentifier(accountResolver);
        }
        return switch (accountResolver.getIdType()) {
            case ID -> accountResolver.getIdentifier();
            case EXTERNAL_ID -> currentAccountRepository.findIdByExternalId(new ExternalId(accountResolver.getIdentifier()))
                    .orElseThrow(() -> new ResourceNotFoundException("current.account",
                            "Current account with external id: %s cannot be found", accountResolver.getIdentifier()));
            case ACCOUNT_NUMBER -> currentAccountRepository.findIdByAccountNumber(accountResolver.getIdentifier())
                    .orElseThrow(() -> new ResourceNotFoundException("current.account",
                            "Current account with account number: %s cannot be found", accountResolver.getIdentifier()));
        };
    }

    @Override
    public IdentifiersResponseData retrieveIdentifiers(@NotNull CurrentAccountResolver accountResolver) {
        String accountId = retrieveId(accountResolver);
        CurrentAccountIdentifiersData currentAccountIdentifiersData = currentAccountRepository.findIdentifiersByAccountId(accountId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("current.account", "Current account with id: %s cannot be found", accountId));
        List<AccountIdentifier> extraSecondaryIdentifiers = accountIdentifierRepository
                .getByAccountTypeAndAccountId(PortfolioAccountType.CURRENT, accountId);
        return currentAccountIdentifiersResponseDataMapper.map(currentAccountIdentifiersData, extraSecondaryIdentifiers);
    }

    private String resolveIdBySecondaryIdentifier(@NotNull CurrentAccountResolver accountResolver) {
        InteropIdentifierType secondaryType = accountResolver.getInteropIdType();
        String accountId = accountIdentifierRepository.getAccountIdByIdTypeAndIdentifier(PortfolioAccountType.CURRENT, secondaryType,
                accountResolver.getIdentifier(), accountResolver.getSubIdentifier());
        if (accountId == null) {
            if (accountResolver.getSubIdentifier() == null) {
                throw new ResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s cannot be found", secondaryType,
                        accountResolver.getIdentifier());
            } else {
                throw new ResourceNotFoundException("current.account",
                        "Current account with secondary identifier: %s and value: %s and sub value: %s cannot be found", secondaryType,
                        accountResolver.getIdentifier(), accountResolver.getSubIdentifier());
            }
        }
        return accountId;
    }
}
