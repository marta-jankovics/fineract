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
package org.apache.fineract.currentaccount.statement.data;

import static org.apache.fineract.currentaccount.statement.data.CurrentStatementData.STATEMENT_TYPE_PENDING;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.portfolio.TransactionEntryType;
import org.apache.fineract.statement.data.camt053.BalanceAmountData;
import org.apache.fineract.statement.data.camt053.BankTransactionCodeData;
import org.apache.fineract.statement.data.camt053.CodeOrProprietaryData;
import org.apache.fineract.statement.data.camt053.CreditDebitIndicator;
import org.apache.fineract.statement.data.camt053.DateAndTimeData;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;
import org.apache.fineract.statement.data.camt053.TransactionData;
import org.apache.logging.log4j.util.Strings;

public final class CurrentTransactionData extends TransactionData {

    @Transient
    @JsonIgnore
    private transient String structuredEntryDetails;

    private CurrentTransactionData(@NotNull BigDecimal amount, @NotNull String currency, TransactionEntryType entryType,
            @NotNull TransactionStatus status, String accountServicerReference, LocalDate bookingDate, LocalDate valueDate,
            EntryDetailsData entryDetails, String structuredEntryDetails) {
        super(new BalanceAmountData(amount, currency), CreditDebitIndicator.forTransactionEntryType(entryType),
                new CodeOrProprietaryData(status.name(), null), accountServicerReference, new BankTransactionCodeData(null, null),
                DateAndTimeData.create(bookingDate), DateAndTimeData.create(valueDate),
                entryDetails == null ? null : new EntryDetailsData[] { entryDetails });
        this.structuredEntryDetails = structuredEntryDetails;
    }

    public static CurrentTransactionData create(@NotNull CurrentTransaction transaction, Map<String, Object> clientDetails,
            @NotNull String currency, int statementType, @NotNull Map<String, Object> details) { // TODO CURRENT! load
                                                                                                 // data
        CurrentEntryDetailsData entryDetails = null;
        String structuredData = (String) details.get("entry_details");
        if (Strings.isEmpty(structuredData)) {
            structuredData = null;
            entryDetails = CurrentEntryDetailsData.create(transaction, clientDetails, currency, details);
        }
        return new CurrentTransactionData(transaction.getAmount(), currency, transaction.getTransactionType().getEntryType(),
                calcTransactionStatus(statementType), String.valueOf(transaction.getId()), transaction.getSubmittedOnDate(),
                transaction.getTransactionDate(), entryDetails, structuredData);
    }

    @NotNull
    public static TransactionStatus calcTransactionStatus(int statementType) {
        return statementType == STATEMENT_TYPE_PENDING ? TransactionStatus.PDNG : TransactionStatus.BOOK;
    }

    @Transient
    @JsonIgnore
    public String getStructuredEntryDetails() {
        return structuredEntryDetails;
    }
}
