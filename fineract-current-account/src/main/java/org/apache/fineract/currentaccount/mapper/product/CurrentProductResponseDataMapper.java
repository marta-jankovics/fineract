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
import org.apache.fineract.currentaccount.data.product.CurrentProductData;
import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentProductResponseDataMapper {

    default Page<CurrentProductResponseData> map(Page<CurrentProductData> data) {
        return data.map(this::map);
    }

    @Mapping(target = "currency", source = "currentProductData", qualifiedByName = "currency")
    @Mapping(target = "accountingType", source = "currentProductData", qualifiedByName = "accountingType")
    CurrentProductResponseData map(CurrentProductData currentProductData);

    @Named("currency")
    default CurrencyData mapToCurrencyData(CurrentProductData currentProductData) {
        return new CurrencyData(currentProductData.getCurrencyCode(), currentProductData.getCurrencyName(),
                currentProductData.getDigitsAfterDecimal(), null,
                currentProductData.getCurrencyDisplaySymbol(), null);
    }

    @Named("accountingType")
    default EnumOptionData mapAccountingType(CurrentProductData currentProductData) {
        return new EnumOptionData((long) currentProductData.getAccountingType().getValue(),
                currentProductData.getAccountingType().getCode(), currentProductData.getAccountingType().toString());
    }

    List<CurrentProductResponseData> map(List<CurrentProductData> data);
}
