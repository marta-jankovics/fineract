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
package org.apache.fineract.binx.currentaccount.statement.data;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.fineract.binx.statement.data.BinxTransactionStatementData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.TransactionEntryType;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;

public final class BinxCurrentTransactionStatementData extends BinxTransactionStatementData {

    protected BinxCurrentTransactionStatementData(String entryReference, @NotNull BigDecimal amount, @NotNull String currency,
            TransactionEntryType entryType, @NotNull TransactionStatus status, String accountServicerReference, String paymentTypeCode,
            LocalDate bookingDate, LocalDate valueDate, @NotNull EntryDetailsData entryDetails, String inputChannel,
            String structuredEntryDetails) {
        super(entryReference, amount, currency, entryType, status, accountServicerReference, paymentTypeCode, bookingDate, valueDate,
                entryDetails, inputChannel, structuredEntryDetails);
    }

    public static BinxCurrentTransactionStatementData create(@NotNull CurrentTransactionData transaction,
            @NotNull Map<InteropIdentifierType, AccountIdentifier> identifiers, Map<String, Object> clientDetails, @NotNull String currency,
            int statementType, Map<String, Object> transactionDetails) {
        String paymentTypeCode = transaction.getPaymentTypeName();
        String inputChannel = null;
        String structuredData = null;
        if (transactionDetails != null) {
            if (paymentTypeCode == null) {
                paymentTypeCode = (String) transactionDetails.get("payment_type_code");
            }
            inputChannel = (String) transactionDetails.get("transaction_creation_channel");
            structuredData = Strings.emptyToNull((String) transactionDetails.get("entry_details"));
        }
        BinxCurrentEntryDetailsData entryDetails = BinxCurrentEntryDetailsData.create(transaction, identifiers, clientDetails, currency,
                transactionDetails, paymentTypeCode);

        return new BinxCurrentTransactionStatementData(null, transaction.getAmount(), currency,
                transaction.getTransactionType().getEntryType(), calcTransactionStatus(statementType), transaction.getId(), paymentTypeCode,
                transaction.getSubmittedOnDate(), transaction.getTransactionDate(), entryDetails, inputChannel, structuredData);
    }
}
