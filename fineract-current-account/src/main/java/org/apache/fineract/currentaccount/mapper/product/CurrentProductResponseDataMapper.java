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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.producttoaccountmapping.domain.ProductToGLAccountMapping;
import org.apache.fineract.currentaccount.data.accounting.GLAccountDetailsData;
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.GlAccountMapping;
import org.apache.fineract.currentaccount.data.product.PaymentChannelToFundSourceData;
import org.apache.fineract.currentaccount.enumeration.product.CurrentProductCashBasedAccount;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentProductResponseDataMapper {

    default Page<CurrentProductResponseData> map(Page<CurrentProductData> data,
            Function<CurrentProductData, List<ProductToGLAccountMapping>> glAccountMappingRetrieveFunc) {
        return data.map(currentProductData -> map(currentProductData, glAccountMappingRetrieveFunc));
    }

    default CurrentProductResponseData map(CurrentProductData currentProductData,
            Function<CurrentProductData, List<ProductToGLAccountMapping>> glAccountMappingRetrieveFunc) {
        return mapResolved(currentProductData, glAccountMappingRetrieveFunc.apply(currentProductData));
    }

    default List<CurrentProductResponseData> map(List<CurrentProductData> data,
            Function<CurrentProductData, List<ProductToGLAccountMapping>> glAccountMappingRetrieveFunc) {
        return data.stream().map(currentProductData -> map(currentProductData, glAccountMappingRetrieveFunc)).toList();
    }

    default Page<CurrentProductResponseData> map(Page<CurrentProductData> data) {
        return data.map(this::map);
    }

    default List<CurrentProductResponseData> map(List<CurrentProductData> data) {
        return data.stream().map(currentProductData -> mapResolved(currentProductData, null)).toList();
    }

    default CurrentProductResponseData map(CurrentProductData currentProductData) {
        return mapResolved(currentProductData, null);
    }

    @Mapping(target = "currency", source = "currentProductData", qualifiedByName = "currency")
    @Mapping(target = "accountingType", source = "currentProductData", qualifiedByName = "accountingType")
    @Mapping(target = "balanceCalculationType", source = "currentProductData", qualifiedByName = "balanceCalculationType")
    @Mapping(target = "glAccountMappings", source = "glAccountMappings", qualifiedByName = "glAccountMapping")
    @Mapping(target = "paymentChannelToFundSourceMappings", source = "glAccountMappings", qualifiedByName = "paymentChannelMapping")
    CurrentProductResponseData mapResolved(CurrentProductData currentProductData, List<ProductToGLAccountMapping> glAccountMappings);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentProductData currentProductData) {
        return new CurrencyData(currentProductData.getCurrencyCode(), currentProductData.getCurrencyName(),
                currentProductData.getCurrencyDigitsAfterDecimal(), currentProductData.getCurrencyInMultiplesOf(),
                currentProductData.getCurrencyDisplaySymbol(), null);
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
    default List<GlAccountMapping> glAccountMapping(List<ProductToGLAccountMapping> glAccountMappings) {
        if (glAccountMappings == null) {
            return null;
        }
        return glAccountMappings.stream().filter(glAccountMapping -> glAccountMapping.getPaymentType() == null).map(glAccountMapping -> {
            GLAccountDetailsData glAccountDetailsData = new GLAccountDetailsData(glAccountMapping.getGlAccount().getId(),
                    glAccountMapping.getGlAccount().getName(), glAccountMapping.getGlAccount().getGlCode(),
                    GLAccountType.fromInt(glAccountMapping.getGlAccount().getType()).name());

            StringEnumOptionData cashAccount = CurrentProductCashBasedAccount.fromInt(glAccountMapping.getFinancialAccountType())
                    .toGLStringEnumOptionData();
            return new GlAccountMapping(cashAccount, glAccountDetailsData);
        }).collect(Collectors.toList());
    }

    @Named("paymentChannelMapping")
    default List<PaymentChannelToFundSourceData> paymentChannelMapping(List<ProductToGLAccountMapping> glAccountMappings) {
        if (glAccountMappings == null) {
            return null;
        }
        return glAccountMappings.stream().filter(glAccountMapping -> glAccountMapping.getPaymentType() != null).map(glAccountMapping -> {
            GLAccountDetailsData glAccountDetailsData = new GLAccountDetailsData(glAccountMapping.getGlAccount().getId(),
                    glAccountMapping.getGlAccount().getName(), glAccountMapping.getGlAccount().getGlCode(),
                    GLAccountType.fromInt(glAccountMapping.getGlAccount().getType()).name());

            PaymentTypeData paymentTypeData = PaymentTypeData.instance(glAccountMapping.getPaymentType().getId(),
                    glAccountMapping.getPaymentType().getName());
            return new PaymentChannelToFundSourceData(paymentTypeData, glAccountDetailsData);
        }).collect(Collectors.toList());
    }
}
