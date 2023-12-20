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
package org.apache.fineract.currentaccount.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_product", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"}, name = "sp_unq_name"),
        @UniqueConstraint(columnNames = {"short_name"}, name = "sp_unq_short_name")})
public class CurrentProduct extends AbstractAuditableWithUTCDateTimeCustom {
    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Basic
    @Column(name = "short_name", nullable = false, length = 4)
    private String shortName;
    @Basic
    @Column(name = "description", length = 500)
    private String description;
    @Basic
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Basic
    @Column(name = "currency_digits", nullable = false)
    private Integer currencyDigits;
    @Basic
    @Column(name = "currency_multiples_of")
    private Integer currencyMultiplesOf;
    @Basic
    @Column(name = "accounting_type", nullable = false)
    private Integer accountingType;
    @Basic
    @Column(name = "allow_overdraft", nullable = false)
    private boolean allowOverdraft;
    @Basic
    @Column(name = "overdraft_limit", precision = 6)
    private BigDecimal overdraftLimit;
    @Basic
    @Column(name = "min_required_balance", precision = 6)
    private BigDecimal minRequiredBalance;
}
