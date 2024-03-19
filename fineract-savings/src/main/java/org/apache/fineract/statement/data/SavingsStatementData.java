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
package org.apache.fineract.statement.data;

import static org.apache.fineract.interoperation.domain.InteropIdentifierType.ALIAS;
import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_BEGIN_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_END_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_FULL_OF_PERIOD;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountSummary;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;
import org.apache.fineract.statement.data.camt053.TransactionsSummaryData;
import org.apache.fineract.statement.domain.AccountStatement;

public final class SavingsStatementData extends StatementData {

    private SavingsStatementData(String identification, OffsetDateTime creationDateTime, DateTimePeriodData fromToDate, AccountData account,
            AccountBalanceData[] balances, TransactionsSummaryData transactionsSummary, TransactionStatementData[] transactions,
            String accountType, String customerId, String accountId, LocalDate fromDate, LocalDate toDate) {
        super(identification, creationDateTime, fromToDate, account, balances, transactionsSummary, transactions, null, accountType,
                customerId, accountId, null, fromDate, toDate);
    }

    public static SavingsStatementData create(@NotNull AccountStatement statement, @NotNull SavingsAccount account,
            @NotNull LocalDate fromDate, @NotNull LocalDate toDate, @NotNull String identification,
            @NotNull OffsetDateTime creationDateTime, @NotNull List<SavingsAccountTransaction> transactions) {
        DateTimePeriodData fromToDate = DateTimePeriodData.create(fromDate, toDate);
        String otherId = account.getAccountNumber();
        String currency = account.getCurrency().getCode();
        // only one of the identifiers can be stored here
        AccountData accountData = AccountData.create(null, otherId, ALIAS.name(), currency);
        AccountBalanceData opening = AccountBalanceData.create(BALANCE_CODE_BEGIN_OF_PERIOD,
                MathUtil.nullToZero(statement.getStatementBalance()), currency, fromDate);
        BigDecimal balance = account.getSummaryOnDate(toDate).getAccountBalance();
        AccountBalanceData closure = AccountBalanceData.create(BALANCE_CODE_END_OF_PERIOD, balance, currency, toDate);
        BigDecimal holdAmount = account.calculateHoldAmountOnDate(toDate); // onHoldFunds is not calculated
        AccountBalanceData total = AccountBalanceData.create(BALANCE_CODE_FULL_OF_PERIOD, MathUtil.subtract(balance, holdAmount), currency,
                toDate);
        SavingsAccountSummary sum = account.getSummaryForTransactions(transactions);
        TransactionsSummaryData trSum = TransactionsSummaryData.create(sum.getTotalCredit(), sum.getTotalDebit());
        int size = transactions.size();
        ArrayList<SavingsTransactionStatementData> entries = new ArrayList<>(size);
        for (SavingsAccountTransaction transaction : transactions) {
            entries.add(SavingsTransactionStatementData.create(transaction, currency));
        }

        return new SavingsStatementData(identification, creationDateTime, fromToDate, accountData,
                new AccountBalanceData[] { opening, closure, total }, trSum, entries.toArray(new SavingsTransactionStatementData[size]),
                SAVING.name().toLowerCase(), null, otherId, fromDate, toDate);
    }
}
