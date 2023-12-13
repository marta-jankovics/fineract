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
package org.apache.fineract.portfolio.statement.data.camt053;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.apache.fineract.portfolio.statement.StatementUtils;

@Getter
public class TransactionData {

    @NotNull
    @JsonProperty(value = "Amount", required = true)
    private final BalanceAmountData amount;
    @NotNull
    @JsonProperty(value = "CreditDebitIndicator", required = true)
    private final CreditDebitIndicator creditDebitIndicator;
    @NotNull
    @JsonProperty(value = "Status", required = true)
    private final CodeOrProprietaryData status;
    @JsonProperty("AccountServicerReference")
    @Size(min = 1, max = 35)
    private final String accountServicerReference;
    @NotNull
    @JsonProperty(value = "BankTransactionCode", required = true)
    private final BankTransactionCodeData transactionCode;
    @JsonProperty("BookingDate")
    private final DateAndTimeData bookingDate;
    @JsonProperty("ValueDate")
    private final DateAndTimeData valueDate;
    @JsonInclude(NON_EMPTY)
    @JsonProperty("EntryDetails")
    private final EntryDetailsData[] details;

    public TransactionData(@NotNull BalanceAmountData amount, @NotNull CreditDebitIndicator creditDebitIndicator,
            @NotNull CodeOrProprietaryData status, String accountServicerReference, @NotNull BankTransactionCodeData transactionCode,
            DateAndTimeData bookingDate, DateAndTimeData valueDate, EntryDetailsData[] details) {
        this.amount = amount;
        this.creditDebitIndicator = creditDebitIndicator;
        this.status = status;
        this.accountServicerReference = StatementUtils.ensureSize(accountServicerReference, "AccountServicerReference", 1, 35);
        this.transactionCode = transactionCode;
        this.bookingDate = bookingDate;
        this.valueDate = valueDate;
        this.details = details;
    }

    public enum TransactionStatus {
        BOOK, FUTR, INFO, PDNG;
    }
}
