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

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.portfolio.TransactionEntryType;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;

public class CurrentTransactionStatementData extends TransactionStatementData {

    protected CurrentTransactionStatementData(String entryReference, @NotNull BigDecimal amount, @NotNull String currency,
            TransactionEntryType entryType, @NotNull TransactionStatus status, String accountServicerReference, String paymentTypeCode,
            LocalDate bookingDate, LocalDate valueDate, String inputChannel, EntryDetailsData entryDetails) {
        super(entryReference, amount, currency, entryType, status, accountServicerReference, paymentTypeCode, bookingDate, valueDate,
                inputChannel, entryDetails);
    }

    public static CurrentTransactionStatementData create(@NotNull CurrentTransactionData transaction, @NotNull String currency) {
        String paymentTypeCode = transaction.getPaymentTypeName();

        return new CurrentTransactionStatementData(null, transaction.getAmount(), currency, transaction.getTransactionType().getEntryType(),
                TransactionStatus.BOOK, transaction.getId(), paymentTypeCode, transaction.getSubmittedOnDate(),
                transaction.getTransactionDate(), null, null);
    }
}
