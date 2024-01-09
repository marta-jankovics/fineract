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
import java.util.UUID;
import lombok.Data;
import org.apache.fineract.currentaccount.enums.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

@Data
public class CurrentTransactionData implements Serializable {

    // Current product data
    private final UUID id;
    private final UUID accountId;
    private final ExternalId externalId;
    private final CurrentTransactionType transactionType;
    private final LocalDate transactionDate;
    private final LocalDate submittedOnDate;
    private final BigDecimal transactionAmount;
    private final OffsetDateTime createdDateTime;

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

    public CurrentTransactionData(UUID id, UUID accountId, ExternalId externalId, CurrentTransactionType transactionType,
            LocalDate transactionDate, LocalDate submittedOnDate, BigDecimal transactionAmount, OffsetDateTime createdDateTime) {
        this.id = id;
        this.accountId = accountId;
        this.externalId = externalId;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.submittedOnDate = submittedOnDate;
        this.transactionAmount = transactionAmount;
        this.createdDateTime = createdDateTime;
        currencyCode = null;
        currencyName = null;
        currencyNameCode = null;
        currencyDisplaySymbol = null;
        currencyDigitsAfterDecimal = null;
        currencyInMultiplesOf = null;
        paymentDetailId = null;
        paymentDetailAccountNumber = null;
        paymentDetailCheckNumber = null;
        paymentDetailRoutingCode = null;
        paymentDetailReceiptNumber = null;
        paymentDetailsBankNumber = null;
        paymentTypeId = null;
        paymentTypeName = null;
        paymentTypeDescription = null;
        paymentTypeIsCashPayment = null;
        paymentTypePosition = null;
        paymentTypeCodeName = null;
        paymentTypeIsSystemDefined = null;
    }

    public CurrentTransactionData(UUID id, UUID accountId, ExternalId externalId, CurrentTransactionType transactionType,
            LocalDate transactionDate, LocalDate submittedOnDate, BigDecimal transactionAmount, OffsetDateTime createdDateTime,
            String currencyCode, String currencyName, String currencyNameCode, String currencyDisplaySymbol,
            Integer currencyDigitsAfterDecimal, Integer currencyInMultiplesOf, Long paymentDetailId, String paymentDetailAccountNumber,
            String paymentDetailCheckNumber, String paymentDetailRoutingCode, String paymentDetailReceiptNumber,
            String paymentDetailsBankNumber, Long paymentTypeId, String paymentTypeName, String paymentTypeDescription,
            Boolean paymentTypeIsCashPayment, Long paymentTypePosition, String paymentTypeCodeName, Boolean paymentTypeIsSystemDefined) {
        this.id = id;
        this.accountId = accountId;
        this.externalId = externalId;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.submittedOnDate = submittedOnDate;
        this.transactionAmount = transactionAmount;
        this.createdDateTime = createdDateTime;
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.currencyNameCode = currencyNameCode;
        this.currencyDisplaySymbol = currencyDisplaySymbol;
        this.currencyDigitsAfterDecimal = currencyDigitsAfterDecimal;
        this.currencyInMultiplesOf = currencyInMultiplesOf;
        this.paymentDetailId = paymentDetailId;
        this.paymentDetailAccountNumber = paymentDetailAccountNumber;
        this.paymentDetailCheckNumber = paymentDetailCheckNumber;
        this.paymentDetailRoutingCode = paymentDetailRoutingCode;
        this.paymentDetailReceiptNumber = paymentDetailReceiptNumber;
        this.paymentDetailsBankNumber = paymentDetailsBankNumber;
        this.paymentTypeId = paymentTypeId;
        this.paymentTypeName = paymentTypeName;
        this.paymentTypeDescription = paymentTypeDescription;
        this.paymentTypeIsCashPayment = paymentTypeIsCashPayment;
        this.paymentTypePosition = paymentTypePosition;
        this.paymentTypeCodeName = paymentTypeCodeName;
        this.paymentTypeIsSystemDefined = paymentTypeIsSystemDefined;
    }
}
