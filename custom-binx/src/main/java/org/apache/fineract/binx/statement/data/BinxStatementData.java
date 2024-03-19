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

import static org.apache.fineract.binx.BinxConstants.CONVERSION_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.BinxConstants.DISPOSAL_ACCOUNT_DISCRIMINATOR;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.apache.fineract.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;
import org.apache.fineract.statement.data.camt053.TransactionsSummaryData;

public class BinxStatementData extends StatementData {

    public static final int STATEMENT_TYPE_ALL = 0;
    public static final int STATEMENT_TYPE_BOOKED = 1;
    public static final int STATEMENT_TYPE_PENDING = 2;

    protected BinxStatementData(String identification, OffsetDateTime creationDateTime, DateTimePeriodData fromToDate, AccountData account,
            AccountBalanceData[] balances, TransactionsSummaryData transactionsSummary, TransactionStatementData[] transactions,
            String additionalStatementInformation, String accountType, String customerId, String accountId, String iban, LocalDate fromDate,
            LocalDate toDate) {
        super(identification, creationDateTime, fromToDate, account, balances, transactionsSummary, transactions,
                additionalStatementInformation, accountType, customerId, accountId, iban, fromDate, toDate);
    }

    @Transient
    @JsonIgnore
    public boolean isPendingType() {
        return "PENDING".equals(getAdditionalStatementInformation());
    }

    @Transient
    @JsonIgnore
    public boolean isConversionAccount() {
        return CONVERSION_ACCOUNT_DISCRIMINATOR.equals(getAccountType());
    }

    @Transient
    @JsonIgnore
    public boolean isDisposalAccount() {
        return DISPOSAL_ACCOUNT_DISCRIMINATOR.equals(getAccountType());
    }

    public static String calcAdditionalInfo(int statementType) {
        switch (statementType) {
            case STATEMENT_TYPE_ALL -> {
                return null;
            }
            case STATEMENT_TYPE_BOOKED -> {
                return "BOOKED";
            }
            case STATEMENT_TYPE_PENDING -> {
                return "PENDING";
            }
            default -> {
                return null;
            }
        }
    }
}
