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
package org.apache.fineract.currentaccount.service.transaction.read.impl;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.currentaccount.service.account.read.CurrentAccountReadService;
import org.apache.fineract.currentaccount.service.transaction.CurrentTransactionResolver;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionReadServiceImpl implements CurrentTransactionReadService {

    private final CurrentAccountReadService currentAccountReadService;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final CurrentTransactionRepository currentTransactionRepository;

    @Override
    public CurrentTransactionTemplateResponseData retrieveTemplate(@NotNull CurrentAccountResolver accountResolver) {
        CurrentAccountData currentAccountData = currentAccountReadService.retrieve(accountResolver);
        final List<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        CurrencyData currencyData = new CurrencyData(currentAccountData.getCurrencyCode(), currentAccountData.getCurrencyName(),
                currentAccountData.getCurrencyDigitsAfterDecimal(), currentAccountData.getCurrencyInMultiplesOf(),
                currentAccountData.getCurrencyDisplaySymbol(), null);
        return CurrentTransactionTemplateResponseData.builder() //
                .accountId(currentAccountData.getId()) //
                .currency(currencyData) //
                .submittedOnDate(DateUtils.getBusinessLocalDate()) //
                .transactionDate(DateUtils.getBusinessLocalDate()) //
                .paymentTypeOptions(paymentTypeOptions) //
                .build(); //
    }

    @Override
    public CurrentTransactionData retrieve(@NotNull CurrentAccountResolver accountResolver,
            @NotNull CurrentTransactionResolver transactionResolver) {
        CurrentTransactionData transactionData = switch (transactionResolver.getIdType()) {
            case ID -> currentTransactionRepository.getTransaction(accountResolver.getIdentifier(), transactionResolver.getIdentifier());
            case EXTERNAL_ID -> currentTransactionRepository.getTransactionData(accountResolver.getIdentifier(),
                    new ExternalId(transactionResolver.getIdentifier()));
        };
        if (transactionData == null) {
            throw new PlatformResourceNotFoundException("current.product", "Current transaction with %s: %s on account %s cannot be found",
                    transactionResolver.getIdType(), transactionResolver.getIdType(), accountResolver.getIdType(),
                    accountResolver.getIdentifier(), accountResolver.getSubIdentifier());
        }
        return transactionData;
    }

    @Override
    public String retrieveId(@NotNull CurrentAccountResolver accountResolver, @NotNull CurrentTransactionResolver transactionResolver) {
        return switch (transactionResolver.getIdType()) {
            case ID -> transactionResolver.getIdentifier();
            case EXTERNAL_ID -> currentTransactionRepository.getIdByExternalId(new ExternalId(transactionResolver.getIdentifier()));
        };
    }

    @Override
    public Page<CurrentTransactionData> retrieveAll(@NotNull CurrentAccountResolver accountResolver, Pageable pageable) {
        String currentAccountId = currentAccountReadService.retrieveId(accountResolver);
        return currentTransactionRepository.getTransactionDataPage(currentAccountId, pageable);
    }
}
