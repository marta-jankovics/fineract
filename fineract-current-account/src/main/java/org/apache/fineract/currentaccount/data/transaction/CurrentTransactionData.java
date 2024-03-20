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
package org.apache.fineract.currentaccount.data.transaction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.fineract.currentaccount.domain.transaction.ICurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Data
@AllArgsConstructor
public class CurrentTransactionData implements ICurrentTransaction, Serializable {

    // Current product data
    private final String id;
    private final String accountId;
    private final ExternalId externalId;
    private final CurrentTransactionType transactionType;
    private final LocalDate transactionDate;
    private final LocalDate submittedOnDate;
    private final BigDecimal amount;
    private String transactionName;
    private final OffsetDateTime createdDateTime;

    // Currency data
    private final String currencyCode;
    private final Integer currencyDigitsAfterDecimal;
    private final Integer currencyInMultiplesOf;
    private final String currencyName;
    private final String currencyDisplaySymbol;

    // Payment type data
    private final Long paymentTypeId;
    private final String paymentTypeName;
    private final String paymentTypeDescription;
    private final Boolean paymentTypeIsCashPayment;
    private final String paymentTypeCodeName;

    public CurrentTransactionData(String id, String accountId, ExternalId externalId, CurrentTransactionType transactionType,
            LocalDate transactionDate, LocalDate submittedOnDate, BigDecimal amount, String transactionName, OffsetDateTime createdDateTime,
            Long paymentTypeId, String paymentTypeName) {
        this(id, accountId, externalId, transactionType, transactionDate, submittedOnDate, amount, transactionName, createdDateTime, null,
                null, null, null, null, paymentTypeId, paymentTypeName, null, null, null);
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }
}
