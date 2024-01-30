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
package org.apache.fineract.currentaccount.mapper.product;

import org.apache.fineract.accounting.glaccount.data.GLAccountDataForLookup;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.PaymentChannelToFundSourceData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentProductResponseDataMapper {

    default Page<CurrentProductResponseData> map(Page<CurrentProductData> data) {
        return data.map(this::map);
    }

    @Mapping(target = "currency", source = "currentProductData", qualifiedByName = "currency")
    @Mapping(target = "accountingType", source = "currentProductData", qualifiedByName = "accountingType")
    @Mapping(target = "balanceCalculationType", source = "currentProductData", qualifiedByName = "balanceCalculationType")
    @Mapping(target = "controlAccountId", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "referenceAccountId", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "overdraftAccountId", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "transfersInSuspenseAccountId", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "writeOffAccountId", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "incomeFromFee", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "incomeFromPenalty", source = "currentProductData", qualifiedByName = "glAccountMapping")
    @Mapping(target = "paymentChannelToFundSourceMappings", source = "currentProductData", qualifiedByName = "paymentChannelMapping")
    CurrentProductResponseData map(CurrentProductData currentProductData);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentProductData currentProductData) {
        return new CurrencyData(currentProductData.getCurrencyCode(), currentProductData.getCurrencyName(),
                currentProductData.getCurrencyDigitsAfterDecimal(), null, currentProductData.getCurrencyDisplaySymbol(), null);
    }

    @Named("accountingType")
    default StringEnumOptionData mapAccountingType(CurrentProductData currentProductData) {
        return currentProductData.getAccountingType().toStringEnumOptionData();
    }

    @Named("balanceCalculationType")
    default StringEnumOptionData mapBalanceCalculationType(CurrentProductData currentProductData) {
        return currentProductData.getBalanceCalculationType().toStringEnumOptionData();
    }

    @Named("glAccountMapping")
    default GLAccountDataForLookup glAccountMapping(CurrentProductData currentProductData) {
        //TODO: implementation
        return null;
    }

    @Named("paymentChannelMapping")
    default List<PaymentChannelToFundSourceData> paymentChannelMapping(CurrentProductData currentProductData) {
        //TODO: implementation
        return null;
    }

    List<CurrentProductResponseData> map(List<CurrentProductData> data);
}
