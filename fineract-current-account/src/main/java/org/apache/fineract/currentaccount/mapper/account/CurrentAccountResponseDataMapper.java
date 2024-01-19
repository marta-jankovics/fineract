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

import java.math.BigDecimal;
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
        return data.map(currentAccountData -> map(currentAccountData, null, null));
    }

    default CurrentAccountResponseData map(CurrentAccountData currentAccountData) {
        return map(currentAccountData, null, null);
    }

    @Mapping(target = "currency", source = "currentAccountData", qualifiedByName = "currency")
    @Mapping(target = "status", source = "currentAccountData", qualifiedByName = "status")
    @Mapping(target = "accountType", source = "currentAccountData", qualifiedByName = "accountType")
    @Mapping(target = "availableBalance", source = "availableBalance")
    @Mapping(target = "totalOnHoldBalance", source = "totalOnHoldBalance")
    CurrentAccountResponseData map(CurrentAccountData currentAccountData, BigDecimal availableBalance, BigDecimal totalOnHoldBalance);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentAccountData currentAccountData) {
        return new CurrencyData(currentAccountData.getCurrencyCode(), currentAccountData.getCurrencyName(),
                currentAccountData.getDigitsAfterDecimal(), null,
                currentAccountData.getCurrencyDisplaySymbol(), null);
    }

    @Named("status")
    default EnumOptionData mapStatus(CurrentAccountData currentAccountData) {
        return new EnumOptionData((long) currentAccountData.getStatus().getValue(), currentAccountData.getStatus().getCode(),
                currentAccountData.getStatus().name());
    }

    @Named("accountType")
    default EnumOptionData mapAccountType(CurrentAccountData currentProductData) {
        return AccountEnumerations.loanType(currentProductData.getAccountType());
    }

    List<CurrentAccountResponseData> map(List<CurrentAccountData> data);
}
