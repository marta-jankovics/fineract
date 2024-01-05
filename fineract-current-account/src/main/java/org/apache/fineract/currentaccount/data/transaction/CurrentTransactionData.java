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
import lombok.Data;
import org.apache.fineract.currentaccount.enums.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Data
public class CurrentTransactionData implements Serializable {

    // Current product data
    private final Long id;
    private final Long accountId;
    private final ExternalId externalId;
    private final CurrentTransactionType transactionType;
    private final LocalDate transactionDate;
    private final LocalDate submittedOnDate;
    private final BigDecimal transactionAmount;

    // Currency data
    private final String currencyCode;
    private final String currencyName;
    private final String currencyNameCode;
    private final String currencyDisplaySymbol;
    private final Integer currencyDigitsAfterDecimal;
    private final Integer currencyInMultiplesOf;

    // Payment detail data
    private final Long paymentDetailId;
    private final String paymentDetailAccountNumber;
    private final String paymentDetailCheckNumber;
    private final String paymentDetailRoutingCode;
    private final String paymentDetailReceiptNumber;
    private final String paymentDetailsBankNumber;

    // Payment type data
    private final Long paymentTypeId;
    private final String paymentTypeName;
    private final String paymentTypeDescription;
    private final Boolean paymentTypeIsCashPayment;
    private final Long paymentTypePosition;
    private final String paymentTypeCodeName;
    private final Boolean paymentTypeIsSystemDefined;
}
