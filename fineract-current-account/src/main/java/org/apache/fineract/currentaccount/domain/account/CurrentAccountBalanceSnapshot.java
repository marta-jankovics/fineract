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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.eclipselink.converter.UUIDConverter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "m_current_account_balance_snapshot")
@Converter(name = "uuidConverter", converterClass = UUIDConverter.class)
public class CurrentAccountBalanceSnapshot extends AbstractAuditableWithUTCDateTimeCustom<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter(onMethod = @__(@Override))
    @Convert(value = "uuidConverter")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "available_balance", nullable = false, precision = 6)
    private BigDecimal availableBalance;

    @Column(name = "total_on_hold_balance", nullable = false, precision = 6)
    private BigDecimal totalOnHoldBalance;

    @Column(name = "calculated_till_utc", nullable = false)
    private OffsetDateTime calculatedTill;

    @Column(name = "calculated_till_transaction_id", nullable = false)
    private UUID calculatedTillTransactionId;

    @Version
    private int version;
}
