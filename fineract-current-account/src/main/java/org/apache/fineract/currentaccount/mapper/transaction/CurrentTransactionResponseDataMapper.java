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
package org.apache.fineract.currentaccount.mapper.transaction;

import java.util.function.Function;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionBalanceResponseData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.currentaccount.domain.transaction.ICurrentTransaction;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentTransactionResponseDataMapper {

    default Page<CurrentTransactionResponseData> map(Page<CurrentTransactionData> data) {
        return data.map(this::mapAll);
    }

    default CurrentTransactionResponseData map(CurrentTransactionData transactionData,
            Function<CurrentAccountResolver, CurrentAccountData> accountRetrieverFunc) {
        return mapOne(transactionData, accountRetrieverFunc.apply(CurrentAccountResolver.resolveDefault(transactionData.getAccountId())));
    }

    default CurrentTransactionBalanceResponseData map(CurrentTransactionData transactionData,
            Function<CurrentAccountResolver, CurrentAccountData> accountRetrieverFunc,
            Function<ICurrentTransaction, CurrentAccountBalanceData> balanceRetrieverFunc) {
        return mapBalance(accountRetrieverFunc.apply(CurrentAccountResolver.resolveDefault(transactionData.getAccountId())),
                balanceRetrieverFunc.apply(transactionData));
    }

    @Mapping(target = "currency", source = "data", qualifiedByName = "currency")
    @Mapping(target = "transactionType", source = "data", qualifiedByName = "transactionType")
    @Mapping(target = "transactionEntryType", source = "data", qualifiedByName = "transactionEntryType")
    @Mapping(target = "paymentTypeData", source = "data", qualifiedByName = "mapPaymentTypeData")
    @Mapping(target = "transactionAmount", source = "data.amount")
    CurrentTransactionResponseData mapAll(CurrentTransactionData data);

    @Mapping(target = "id", source = "transactionData.id")
    @Mapping(target = "accountId", source = "transactionData.accountId")
    @Mapping(target = "externalId", source = "transactionData.externalId")
    @Mapping(target = "currency", source = "transactionData", qualifiedByName = "currency")
    @Mapping(target = "transactionType", source = "transactionData", qualifiedByName = "transactionType")
    @Mapping(target = "transactionEntryType", source = "transactionData", qualifiedByName = "transactionEntryType")
    @Mapping(target = "paymentTypeData", source = "transactionData", qualifiedByName = "mapPaymentTypeData")
    @Mapping(target = "transactionAmount", source = "transactionData.amount")
    @Mapping(target = "transactionName", source = "transactionData.transactionName")
    CurrentTransactionResponseData mapOne(CurrentTransactionData transactionData, CurrentAccountData accountData);

    @Mapping(target = "transactionId", source = "balanceData.transactionId")
    @Mapping(target = "accountId", source = "accountData.id")
    @Mapping(target = "currency", source = "accountData", qualifiedByName = "currency")
    @Mapping(target = "accountBalance", source = "balanceData.accountBalance")
    @Mapping(target = "holdAmount", source = "balanceData.holdAmount")
    @Mapping(target = "availableBalance", expression = "java(accountData.getAvailableBalance(balanceData, false))")
    CurrentTransactionBalanceResponseData mapBalance(CurrentAccountData accountData, CurrentAccountBalanceData balanceData);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentTransactionData data) {
        return new CurrencyData(data.getCurrencyCode(), data.getCurrencyName(), data.getCurrencyDigitsAfterDecimal(), null,
                data.getCurrencyDisplaySymbol(), null);
    }

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentAccountData accountData) {
        return new CurrencyData(accountData.getCurrencyCode(), accountData.getCurrencyName(), accountData.getCurrencyDigitsAfterDecimal(),
                accountData.getCurrencyInMultiplesOf(), accountData.getCurrencyDisplaySymbol(), null);
    }

    @Named("transactionType")
    default StringEnumOptionData mapTransactionType(CurrentTransactionData data) {
        return data.getTransactionType().toStringEnumOptionData();
    }

    @Named("transactionEntryType")
    default StringEnumOptionData mapTransactionEntryType(CurrentTransactionData data) {
        return data.getTransactionType().getEntryType().toStringEnumOptionData();
    }

    @Named("mapPaymentTypeData")
    default PaymentTypeData mapPaymentTypeData(CurrentTransactionData data) {
        Long paymentTypeId = data.getPaymentTypeId();
        return paymentTypeId == null ? null : PaymentTypeData.instance(paymentTypeId, data.getPaymentTypeName());
    }
}
