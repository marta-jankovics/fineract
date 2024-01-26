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
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersResponseData;
import org.apache.fineract.currentaccount.data.account.ValueSubValueData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = MapstructMapperConfig.class)
public interface CurrentAccountIdentifiersResponseDataMapper {

    default CurrentAccountIdentifiersResponseData map(CurrentAccountIdentifiersData currentAccountIdentifiersData,
            List<AccountIdentifier> extraSecondaryIdentifiers) {
        CurrentAccountIdentifiersResponseData.CurrentAccountIdentifiersResponseDataBuilder builder = CurrentAccountIdentifiersResponseData
                .builder();
        builder.id(currentAccountIdentifiersData.getId()).accountNumber(currentAccountIdentifiersData.getAccountNumber())
                .externalId(currentAccountIdentifiersData.getExternalId());
        for (AccountIdentifier accountIdentifier : extraSecondaryIdentifiers) {
            switch (accountIdentifier.getIdentifierType()) {
                case MSISDN -> builder.msisdn(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case EMAIL -> builder.email(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case PERSONAL_ID ->
                    builder.personalId(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case BUSINESS -> builder.business(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case DEVICE -> builder.device(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case ACCOUNT_ID -> builder.accountId(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case IBAN -> builder.iban(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
                case ALIAS -> builder.alias(new ValueSubValueData(accountIdentifier.getValue(), accountIdentifier.getSubValue()));
            }
        }
        return builder.build();
    }
}
