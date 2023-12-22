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
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.accountdetails.service.AccountEnumerations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentAccountResponseDataMapper {

    default Page<CurrentAccountResponseData> map(Page<CurrentAccountData> data) {
        return data.map(this::map);
    }

    @Mapping(target = "currency", source = "currentProductData", qualifiedByName = "currency")
    @Mapping(target = "status", source = "currentProductData", qualifiedByName = "status")
    @Mapping(target = "accountType", source = "currentProductData", qualifiedByName = "accountType")
    CurrentAccountResponseData map(CurrentAccountData currentProductData);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentAccountData currentProductData) {
        return new CurrencyData(currentProductData.getCurrencyCode(), currentProductData.getCurrencyName(),
                currentProductData.getDigitsAfterDecimal(), currentProductData.getInMultiplesOf(),
                currentProductData.getCurrencyDisplaySymbol(), currentProductData.getCurrencyNameCode());
    }

    @Named("status")
    default EnumOptionData mapStatus(CurrentAccountData currentProductData) {
        return new EnumOptionData((long) currentProductData.getStatus().getValue(), currentProductData.getStatus().getCode(),
                currentProductData.getStatus().name());
    }

    @Named("accountType")
    default EnumOptionData mapAccountType(CurrentAccountData currentProductData) {
        return AccountEnumerations.loanType(currentProductData.getAccountType());
    }

    List<CurrentAccountResponseData> map(List<CurrentAccountData> data);
}
