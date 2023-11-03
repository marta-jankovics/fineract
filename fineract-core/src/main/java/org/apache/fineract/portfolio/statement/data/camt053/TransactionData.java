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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.portfolio.TransactionEntryType;

@Getter
@AllArgsConstructor
public class TransactionData {

    @NotNull
    @JsonProperty("Amount")
    private final BalanceAmountData amount;
    @NotNull
    @JsonProperty("CreditDebitIndicator")
    private final CreditDebitIndicator creditDebitIndicator;
    @NotNull
    @JsonProperty("Status")
    private final CodeOrProprietaryData status;
    @JsonProperty("AccountServicerReference")
    @Size(min = 1, max = 35)
    private final String accountServicerReference;
    @NotNull
    @JsonProperty("BankTransactionCode")
    private final BankTransactionCodeData transactionCode;
    @JsonProperty("BookingDate")
    private final DateAndTimeData bookingDate;
    @JsonProperty("ValueDate")
    private final DateAndTimeData valueDate;
    @JsonInclude(NON_EMPTY)
    @JsonProperty("EntryDetails")
    private final EntryDetailsData[] details;

    public static TransactionData create(@NotNull BigDecimal amount, @NotNull String currency, TransactionEntryType entryType,
            @NotNull TransactionStatus status, String accountServicerReference, LocalDate bookingDate, LocalDate valueDate,
            EntryDetailsData[] entryDetailsData) {
        return new TransactionData(new BalanceAmountData(amount, currency), CreditDebitIndicator.forTransactionEntryType(entryType),
                new CodeOrProprietaryData(status.name(), null), accountServicerReference, new BankTransactionCodeData(null, null),
                DateAndTimeData.create(bookingDate), DateAndTimeData.create(valueDate), entryDetailsData);
    }

    public enum TransactionStatus {
        BOOK, FUTR, INFO, PDNG;
    }
}
