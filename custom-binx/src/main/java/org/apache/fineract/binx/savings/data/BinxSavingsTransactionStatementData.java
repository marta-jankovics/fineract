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
package org.apache.fineract.binx.savings.data;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.fineract.binx.statement.data.BinxTransactionStatementData;
import org.apache.fineract.portfolio.TransactionEntryType;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;

public final class BinxSavingsTransactionStatementData extends BinxTransactionStatementData {

    private BinxSavingsTransactionStatementData(String entryReference, @NotNull BigDecimal amount, @NotNull String currency,
            TransactionEntryType entryType, @NotNull TransactionStatus status, String accountServicerReference, String paymentTypeCode,
            LocalDate bookingDate, LocalDate valueDate, @NotNull EntryDetailsData entryDetails, String inputChannel,
            String structuredEntryDetails) {
        super(entryReference, amount, currency, entryType, status, accountServicerReference, paymentTypeCode, bookingDate, valueDate,
                entryDetails, inputChannel, structuredEntryDetails);
    }

    public static BinxSavingsTransactionStatementData create(@NotNull SavingsAccountTransaction transaction,
            Map<String, Object> clientDetails, @NotNull String currency, int statementType,
            @NotNull Map<String, Object> transactionDetails) {
        String paymentTypeCode = (String) transactionDetails.get("payment_type_code");
        if (paymentTypeCode == null) {
            PaymentDetail paymentDetail = transaction.getPaymentDetail();
            paymentTypeCode = paymentDetail == null ? null : paymentDetail.getPaymentType().getName();
        }
        String inputChannel = (String) transactionDetails.get("input_channel");
        String structuredData = Strings.emptyToNull((String) transactionDetails.get("entry_details"));
        BinxSavingsEntryDetailsData entryDetails = BinxSavingsEntryDetailsData.create(transaction, clientDetails, currency,
                transactionDetails, paymentTypeCode);
        return new BinxSavingsTransactionStatementData(null, transaction.getAmount(), currency,
                transaction.getTransactionType().getEntryType(), calcTransactionStatus(statementType), String.valueOf(transaction.getId()),
                paymentTypeCode, transaction.getSubmittedOnDate(), transaction.getTransactionDate(), entryDetails, inputChannel,
                structuredData);
    }
}
