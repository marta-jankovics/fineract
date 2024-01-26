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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "m_current_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "external_id" }, name = "m_current_transaction_external_id_key") })
public class CurrentTransaction extends AbstractAuditableWithUTCDateTimeCustom<String> {

    @Id
    @GeneratedValue(generator = "nanoIdSequence")
    @Getter(onMethod = @__(@Override))
    private String id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "external_id", length = 100, unique = true)
    private ExternalId externalId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "payment_type_id")
    private Long paymentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type_enum", nullable = false)
    private CurrentTransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "submitted_on_date", nullable = false)
    private LocalDate submittedOnDate;

    @Column(name = "amount", nullable = false, precision = 6)
    private BigDecimal transactionAmount;

    public static CurrentTransaction newInstance(String accountId, ExternalId externalId, String correlationId, String referenceId,
            Long paymentDetailId, CurrentTransactionType transactionType, LocalDate transactionDate, LocalDate submittedOnDate,
            BigDecimal amount) {
        return new CurrentTransaction(null, accountId, externalId, correlationId, referenceId, paymentDetailId, transactionType,
                transactionDate, submittedOnDate, amount);
    }
}
