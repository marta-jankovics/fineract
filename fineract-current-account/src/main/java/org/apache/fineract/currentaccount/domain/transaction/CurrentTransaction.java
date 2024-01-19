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
package org.apache.fineract.currentaccount.domain.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.eclipselink.converter.UUIDConverter;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.Converter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_account_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "external_id" }, name = "m_current_account_transaction_external_id_key") })
@Converter(name = "uuidConverter", converterClass = UUIDConverter.class)
public class CurrentTransaction extends AbstractAuditableWithUTCDateTimeCustom<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter(onMethod = @__(@Override))
    @Convert(value = "uuidConverter")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "external_id", length = 100, unique = true)
    private ExternalId externalId;

    @Column(name = "payment_detail_id")
    private Long paymentDetailId;

    @Enumerated
    @Column(name = "transaction_type_enum", nullable = false)
    private CurrentTransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "submitted_on_date", nullable = false)
    private LocalDate submittedOnDate;

    @Column(name = "amount", nullable = false, precision = 6)
    private BigDecimal transactionAmount;

    public static CurrentTransaction newInstance(UUID accountId, ExternalId externalId, Long paymentDetailId,
            CurrentTransactionType transactionType, LocalDate transactionDate, LocalDate submittedOnDate, BigDecimal amount) {
        return new CurrentTransaction(null, accountId, externalId, paymentDetailId, transactionType, transactionDate, submittedOnDate,
                amount);
    }
}
