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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "external_id" }, name = "sa_external_id_UNIQUE") })
public class CurrentAccountTransaction extends AbstractAuditableWithUTCDateTimeCustom {

    @Basic
    @Column(name = "current_account_id", nullable = false)
    private Long currentAccountId;
    @Basic
    @Column(name = "external_id", length = 100)
    private String externalId;
    @Basic
    @Column(name = "payment_detail_id")
    private Long paymentDetailId;
    @Basic
    @Column(name = "transaction_type_enum", nullable = false)
    private Integer transactionTypeEnum;
    @Basic
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Basic
    @Column(name = "amount", nullable = false, precision = 6)
    private BigDecimal amount;
}
