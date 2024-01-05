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

import java.util.List;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionResponseData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentTransactionResponseDataMapper {

    default Page<CurrentTransactionResponseData> map(Page<CurrentTransactionData> data) {
        return data.map(this::map);
    }

    @Mapping(target = "currency", source = "data", qualifiedByName = "currency")
    @Mapping(target = "transactionType", source = "data", qualifiedByName = "transactionType")
    @Mapping(target = "transactionEntryType", source = "data", qualifiedByName = "transactionEntryType")
    @Mapping(target = "paymentDetailData", source = "data", qualifiedByName = "paymentDetailData")
    CurrentTransactionResponseData map(CurrentTransactionData data);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentTransactionData data) {
        return new CurrencyData(data.getCurrencyCode(), data.getCurrencyName(), data.getCurrencyDigitsAfterDecimal(),
                data.getCurrencyInMultiplesOf(), data.getCurrencyDisplaySymbol(), data.getCurrencyNameCode());
    }

    @Named("transactionType")
    default EnumOptionData mapTransactionType(CurrentTransactionData data) {
        return data.getTransactionType().toEnumOptionData();
    }

    @Named("transactionEntryType")
    default EnumOptionData mapTransactionEntryType(CurrentTransactionData data) {
        return data.getTransactionType().getEntryType().toEnumOptionData();
    }

    @Named("paymentDetailData")
    default PaymentDetailData mapPaymentDetailData(CurrentTransactionData data) {
        return new PaymentDetailData(data.getPaymentDetailId(),
                new PaymentTypeData(data.getPaymentTypeId(), data.getPaymentTypeName(), data.getPaymentTypeDescription(),
                        data.getPaymentTypeIsCashPayment(), data.getPaymentTypePosition().intValue(), data.getPaymentTypeCodeName(),
                        data.getPaymentTypeIsSystemDefined()),
                data.getPaymentDetailAccountNumber(), data.getPaymentDetailCheckNumber(), data.getPaymentDetailRoutingCode(),
                data.getPaymentDetailReceiptNumber(), data.getPaymentDetailsBankNumber());
    }

    List<CurrentTransactionResponseData> map(List<CurrentTransactionData> data);
}
