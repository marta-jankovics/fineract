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
package org.apache.fineract.binx.statement.data;

import static org.apache.fineract.binx.currentaccount.statement.data.BinxCurrentStatementData.STATEMENT_TYPE_PENDING;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.fineract.portfolio.TransactionEntryType;
import org.apache.fineract.statement.data.camt053.EntryDetailsData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;

public class BinxTransactionStatementData extends TransactionStatementData {

    @Transient
    @JsonIgnore
    private transient String structuredEntryDetails;

    protected BinxTransactionStatementData(String entryReference, @NotNull BigDecimal amount, @NotNull String currency,
            TransactionEntryType entryType, @NotNull TransactionStatus status, String accountServicerReference, String paymentTypeCode,
            LocalDate bookingDate, LocalDate valueDate, @NotNull EntryDetailsData entryDetails, String inputChannel,
            String structuredEntryDetails) {
        super(entryReference, amount, currency, entryType, status, accountServicerReference, paymentTypeCode, bookingDate, valueDate,
                inputChannel, entryDetails);
        this.structuredEntryDetails = structuredEntryDetails;
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
