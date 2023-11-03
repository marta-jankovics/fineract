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
package org.apache.fineract.portfolio.savings.statement.data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import lombok.Getter;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.portfolio.statement.data.camt053.AccountData;
import org.apache.fineract.portfolio.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.portfolio.statement.data.camt053.StatementData;
import org.apache.fineract.portfolio.statement.data.camt053.TransactionData;
import org.apache.fineract.portfolio.statement.data.camt053.TransactionsSummaryData;
import org.apache.fineract.portfolio.statement.domain.AccountStatement;

@Getter
public class SavingsStatementData extends StatementData {

    public SavingsStatementData(String identification, BigDecimal amount, DateTimePeriodData fromToDate, AccountData account,
            AccountBalanceData balance, TransactionsSummaryData transactionsSummary, TransactionData[] transactions,
            String additionalStatementInformation) {
        super(identification, amount, fromToDate, account, balance, transactionsSummary, transactions, additionalStatementInformation);
    }

    public static SavingsStatementData create(@NotNull AccountStatement statement, @NotNull SavingsAccount account,
            HashMap<String, Object> accountDetails, LocalDate transactionDate, String identification) {
        if (accountDetails == null || accountDetails.isEmpty()) {
            return null;
        }
        return null;
        // return new SavingsStatementData((String) clientDetails.get("subscription_package"), (String)
        // clientDetails.get("short_name"), (String) clientDetails.get("address"));
    }
}
