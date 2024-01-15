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

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionTemplateResponseData;
import org.apache.fineract.currentaccount.exception.transaction.CurrentTransactionNotFoundException;
import org.apache.fineract.currentaccount.mapper.transaction.CurrentTransactionResponseDataMapper;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.transaction.read.CurrentTransactionReadService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class CurrentTransactionReadServiceImpl implements CurrentTransactionReadService {

    private final CurrentAccountRepository currentAccountRepository;
    private final PaymentTypeReadPlatformService paymentTypeReadPlatformService;
    private final CurrentTransactionRepository currentTransactionRepository;
    private final CurrentTransactionResponseDataMapper currentTransactionResponseDataMapper;

    @Override
    public CurrentTransactionTemplateResponseData retrieveTemplate(UUID accountId) {
        CurrentAccountData currentAccountData = currentAccountRepository.findCurrentAccountData(accountId);
        final List<PaymentTypeData> paymentTypeOptions = this.paymentTypeReadPlatformService.retrieveAllPaymentTypes();
        CurrencyData currencyData = new CurrencyData(currentAccountData.getCurrencyCode(), currentAccountData.getCurrencyName(),
                currentAccountData.getDigitsAfterDecimal(), currentAccountData.getInMultiplesOf(),
                currentAccountData.getCurrencyDisplaySymbol(), currentAccountData.getCurrencyNameCode());
        return CurrentTransactionTemplateResponseData.builder() //
                .accountNo(currentAccountData.getAccountNo()) //
                .accountId(currentAccountData.getId()) //
                .currency(currencyData) //
                .submittedOnDate(DateUtils.getBusinessLocalDate()) //
                .transactionDate(DateUtils.getBusinessLocalDate()) //
                .paymentTypeOptions(paymentTypeOptions) //
                .build(); //
    }

    @Override
    public CurrentTransactionResponseData retrieveTransactionById(UUID accountId, UUID transactionId) {
        CurrentTransactionData currentTransactionData = currentTransactionRepository.findByIdAndAccountId(accountId, transactionId);
        if (currentTransactionData == null) {
            throw new CurrentTransactionNotFoundException(accountId, transactionId);
        }
        return currentTransactionResponseDataMapper.map(currentTransactionData);
    }

    @Override
    public Page<CurrentTransactionResponseData> retrieveTransactionByAccountId(UUID accountId, Pageable pageable) {
        Page<CurrentTransactionData> currentTransactionData = currentTransactionRepository.findByAccountId(accountId, pageable);
        return currentTransactionResponseDataMapper.map(currentTransactionData);
    }
}
