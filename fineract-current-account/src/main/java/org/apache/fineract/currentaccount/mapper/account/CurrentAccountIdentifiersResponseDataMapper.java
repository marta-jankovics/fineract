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

import com.google.common.base.CaseFormat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.domain.StringValueHolder;
import org.apache.fineract.portfolio.account.data.IdTypeValueSubValueData;
import org.apache.fineract.portfolio.account.data.IdentifiersResponseData;
import org.apache.fineract.portfolio.account.domain.AccountIdentifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentAccountIdentifiersResponseDataMapper {

    @Mapping(target = "primaryIdentifiers", source = "currentAccountIdentifiersData")
    @Mapping(target = "secondaryIdentifiers", source = "secondaryIdentifiers")
    IdentifiersResponseData map(CurrentAccountIdentifiersData currentAccountIdentifiersData, List<AccountIdentifier> secondaryIdentifiers);

    @SneakyThrows
    default List<IdTypeValueSubValueData> mapPrimaryIdentifiers(CurrentAccountIdentifiersData currentAccountIdentifiersData) {
        List<IdTypeValueSubValueData> primaryIdentifiers = new ArrayList<>();
        Field[] fields = currentAccountIdentifiersData.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(currentAccountIdentifiersData);
            if (field.get(currentAccountIdentifiersData) != null) {
                if (value instanceof StringValueHolder) {
                    value = ((StringValueHolder) value).getValue();
                }
                primaryIdentifiers.add(new IdTypeValueSubValueData(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()),
                        (String) value, null));
            }
        }
        return primaryIdentifiers;
    }

    default List<IdTypeValueSubValueData> mapSecondaryIdentifiers(List<AccountIdentifier> secondaryIdentifiers) {
        return secondaryIdentifiers.stream()
                .map(o -> new IdTypeValueSubValueData(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, o.getIdentifierType().name()),
                        o.getValue(), o.getSubValue()))
                .toList();
    }
}
