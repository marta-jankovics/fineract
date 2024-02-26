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
package org.apache.fineract.currentaccount.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.eclipselink.EmptyStringIfNullConverter;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

//TODO: Move to core when it goes to support other entities as well
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_account_identifier", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "identifier_type", "value", "sub_value" }, name = "uk_acc_identifier_id_type_value_sub_value"),
        @UniqueConstraint(columnNames = { "account_type", "account_id", "identifier_type" }, name = "uk_acc_identifier_account_id_type") })
@Converter(name = "EmptyStringIfNullConverter", converterClass = EmptyStringIfNullConverter.class)
public class AccountIdentifier extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private PortfolioAccountType accountType;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false)
    private InteropIdentifierType identifierType;

    @Column(name = "value", nullable = false)
    private String value;

    @Convert("EmptyStringIfNullConverter")
    @Column(name = "sub_value", nullable = false)
    private String subValue;

    @Version
    private Long version;

    public AccountIdentifier(PortfolioAccountType accountType, String accountId, InteropIdentifierType identifierType, String value,
            String subValue) {
        this.accountType = accountType;
        this.accountId = accountId;
        this.identifierType = identifierType;
        this.value = value;
        this.subValue = subValue;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setSubValue(String subValue) {
        this.subValue = subValue;
    }
}
