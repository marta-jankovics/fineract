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
package org.apache.fineract.currentaccount.mapper.account;

import java.util.List;
import java.util.function.Function;
import org.apache.fineract.currentaccount.data.account.CurrentAccountBalanceData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.client.ClientBaseResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductBaseResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.statement.data.dto.ProductStatementResponseData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentAccountResponseDataMapper {

    default Page<CurrentAccountResponseData> map(Page<CurrentAccountData> data,
            Function<String, CurrentAccountBalanceData> balanceDataRetrieverFunc) {
        return data.map(currentAccountData -> mapAll(currentAccountData, balanceDataRetrieverFunc));
    }

    default List<CurrentAccountResponseData> map(List<CurrentAccountData> data,
            Function<String, CurrentAccountBalanceData> balanceDataRetrieverFunc) {
        return data.stream().map(currentAccountData -> mapAll(currentAccountData, balanceDataRetrieverFunc)).toList();
    }

    default CurrentAccountResponseData map(CurrentAccountData currentAccountData,
            Function<String, CurrentAccountBalanceData> balanceDataRetrieverFunc) {
        return mapOne(currentAccountData, balanceDataRetrieverFunc.apply(currentAccountData.getId()));
    }

    default CurrentAccountResponseData mapAll(CurrentAccountData currentAccountData,
            Function<String, CurrentAccountBalanceData> balanceDataRetrieverFunc) {
        return mapAll(currentAccountData, balanceDataRetrieverFunc.apply(currentAccountData.getId()));
    }

    @Mapping(target = "id", source = "accountData.id")
    @Mapping(target = "currency", source = "accountData", qualifiedByName = "currency")
    @Mapping(target = "status", source = "accountData", qualifiedByName = "status")
    @Mapping(target = "product", source = "accountData", qualifiedByName = "product")
    @Mapping(target = "client", source = "accountData", qualifiedByName = "client")
    @Mapping(target = "balanceCalculationType", source = "accountData", qualifiedByName = "balanceCalculationType")
    @Mapping(target = "accountBalance", source = "balanceData.accountBalance")
    @Mapping(target = "holdAmount", ignore = true)
    @Mapping(target = "availableBalance", ignore = true)
    CurrentAccountResponseData mapAll(CurrentAccountData accountData, CurrentAccountBalanceData balanceData);

    @Mapping(target = "id", source = "accountData.id")
    @Mapping(target = "currency", source = "accountData", qualifiedByName = "currency")
    @Mapping(target = "status", source = "accountData", qualifiedByName = "status")
    @Mapping(target = "product", source = "accountData", qualifiedByName = "product")
    @Mapping(target = "client", source = "accountData", qualifiedByName = "client")
    @Mapping(target = "balanceCalculationType", source = "accountData", qualifiedByName = "balanceCalculationType")
    @Mapping(target = "accountBalance", source = "balanceData.accountBalance")
    @Mapping(target = "holdAmount", source = "balanceData.holdAmount")
    @Mapping(target = "availableBalance", expression = "java(accountData.getAvailableBalance(balanceData, false))")
    CurrentAccountResponseData mapOne(CurrentAccountData accountData, CurrentAccountBalanceData balanceData);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentAccountData currentAccountData) {
        return new CurrencyData(currentAccountData.getCurrencyCode(), currentAccountData.getCurrencyName(),
                currentAccountData.getCurrencyDigitsAfterDecimal(), currentAccountData.getCurrencyInMultiplesOf(),
                currentAccountData.getCurrencyDisplaySymbol(), null);
    }

    @Named("status")
    default StringEnumOptionData mapStatus(CurrentAccountData currentAccountData) {
        return currentAccountData.getStatus().toStringEnumOptionData();
    }

    @Named("balanceCalculationType")
    default StringEnumOptionData mapBalanceCalculationType(CurrentAccountData currentAccountData) {
        return currentAccountData.getBalanceCalculationType().toStringEnumOptionData();
    }

    @Named("product")
    default CurrentProductBaseResponseData mapProduct(CurrentAccountData currentAccountData) {
        return new CurrentProductBaseResponseData(currentAccountData.getProductId(), currentAccountData.getProductName(),
                currentAccountData.getProductShortName(), currentAccountData.getProductDescription());
    }

    @Named("client")
    default ClientBaseResponseData mapClient(CurrentAccountData currentAccountData) {
        return new ClientBaseResponseData(currentAccountData.getClientId(), currentAccountData.getClientName());
    }
}
