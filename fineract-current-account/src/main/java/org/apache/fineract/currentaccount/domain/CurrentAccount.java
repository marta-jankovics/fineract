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
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account", uniqueConstraints = {@UniqueConstraint(columnNames = {"account_no"}, name = "sa_account_no_UNIQUE"), @UniqueConstraint(columnNames = {"external_id"}, name = "sa_external_id_UNIQUE")})
public class CurrentAccount extends AbstractAuditableWithUTCDateTimeCustom {
    @Basic
    @Column(name = "account_no", nullable = false, length = 20)
    private String accountNo;
    @Basic
    @Column(name = "external_id", length = 100)
    private String externalId;
    @Basic
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    @Basic
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Basic
    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum;
    @Basic
    @Column(name = "sub_status_enum", nullable = false)
    private Integer subStatusEnum;
    @Basic
    @Column(name = "account_type_enum", nullable = false)
    private Integer accountTypeEnum;
    @Basic
    @Column(name = "submittedon_date", nullable = false)
    private LocalDate submittedOnDate;
    @Basic
    @Column(name = "submitted_on_user_id", nullable = false)
    private Long submittedOnUserId;
    @Basic
    @Column(name = "approved_on_date")
    private LocalDate approvedOnDate;
    @Basic
    @Column(name = "approved_on_user_id")
    private Long approvedOnUserid;
    @Basic
    @Column(name = "rejected_on_date")
    private LocalDate rejectedOnDate;
    @Basic
    @Column(name = "rejected_on_user_id")
    private Long rejectedOnUserid;
    @Basic
    @Column(name = "withdrawn_on_date")
    private LocalDate withdrawnOnDate;
    @Basic
    @Column(name = "withdrawn_on_user_id")
    private Long withdrawnOnUserid;
    @Basic
    @Column(name = "activated_on_date")
    private LocalDate activatedOnDate;
    @Basic
    @Column(name = "activated_on_user_id")
    private Long activatedOnUserid;
    @Basic
    @Column(name = "closed_on_date")
    private LocalDate closedOnDate;
    @Basic
    @Column(name = "closed_on_user_id")
    private Long closedOnUserid;
    @Basic
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Basic
    @Column(name = "currency_digits", nullable = false)
    private Integer currencyDigits;
    @Basic
    @Column(name = "currency_multiples_of")
    private Integer currencyMultiplesof;
    @Basic
    @Column(name = "allow_overdraft", nullable = false)
    private boolean allowOverdraft;
    @Basic
    @Column(name = "overdraft_limit", precision = 6)
    private BigDecimal overdraftLimit;
    @Basic
    @Column(name = "min_required_balance", precision = 6)
    private BigDecimal minRequiredBalance;
    @Basic
    @Column(name = "version", nullable = false)
    private int version;
}

