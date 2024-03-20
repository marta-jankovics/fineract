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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;

/**
 * Immutable data object representing a current transaction.
 */
@Getter
@AllArgsConstructor
public final class CurrentTransactionResponseData implements Serializable {

    private final String id;
    private final String accountId;
    private final ExternalId externalId;
    private final PaymentTypeData paymentTypeData;
    private final StringEnumOptionData transactionType;
    private final StringEnumOptionData transactionEntryType;
    private final LocalDate transactionDate;
    private final LocalDate submittedOnDate;
    private final BigDecimal transactionAmount;
    private final CurrencyData currency;
    private final String transactionName;
    private final BigDecimal accountBalance;
    private final BigDecimal holdAmount;
    private final BigDecimal availableBalance;
}