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

import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_BEGIN_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_END_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_FULL_OF_PERIOD;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.TransactionData;
import org.apache.fineract.statement.data.camt053.TransactionsSummaryData;
import org.apache.fineract.statement.domain.AccountStatement;

public final class CurrentStatementData extends StatementData {

    public static final int STATEMENT_TYPE_ALL = 0;
    public static final int STATEMENT_TYPE_BOOKED = 1;
    public static final int STATEMENT_TYPE_PENDING = 2;

    @Transient
    @JsonIgnore
    private transient boolean isConversionAccount;

    private CurrentStatementData(String identification, OffsetDateTime creationDateTime, DateTimePeriodData fromToDate, AccountData account,
            AccountBalanceData[] balances, TransactionsSummaryData transactionsSummary, TransactionData[] transactions,
            String additionalStatementInformation, boolean isConversionAccount) {
        super(identification, creationDateTime, fromToDate, account, balances, transactionsSummary, transactions,
                additionalStatementInformation);
        this.isConversionAccount = isConversionAccount;
    }

    public static CurrentStatementData create(@NotNull AccountStatement statement, @NotNull CurrentAccount account,
            Map<String, Object> clientDetails, @NotNull Map<String, Object> accountDetails, @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate, @NotNull String identification, @NotNull OffsetDateTime creationDateTime, int statementType,
            boolean isConversionAccount, @NotNull List<CurrentTransaction> transactions, // TODO CURRENT! loan data
            @NotNull Map<String, Map<String, Object>> transactionDetails) {
        DateTimePeriodData fromToDate = DateTimePeriodData.create(fromDate, toDate);
        String iban = null; // (String) accountDetails.get("iban"); only one of the identifiers can be stored here
        String otherId = (String) accountDetails.get("internal_account_id");
        // TODO CURRENT!
        String currency = null;
        // String currency = account.getCurrency().getCode();
        AccountData accountData = AccountData.create(iban, otherId, clientDetails, currency);
        AccountBalanceData opening = AccountBalanceData.create(BALANCE_CODE_BEGIN_OF_PERIOD,
                MathUtil.nullToZero(statement.getStatementBalance()), currency, fromDate);
        BigDecimal balance = null;
        // BigDecimal balance = account.getSummaryOnDate(toDate).getAccountBalance();
        AccountBalanceData closure = AccountBalanceData.create(BALANCE_CODE_END_OF_PERIOD, balance, currency, toDate);
        BigDecimal holdAmount = null;
        // BigDecimal holdAmount = account.calculateHoldAmountOnDate(toDate); // onHoldFunds is not calculated
        AccountBalanceData total = AccountBalanceData.create(BALANCE_CODE_FULL_OF_PERIOD, MathUtil.subtract(balance, holdAmount), currency,
                toDate);
        // SavingsAccountSummary sum = account.getSummaryForTransactions(transactions);
        TransactionsSummaryData trSum = null;
        // TransactionsSummaryData trSum = TransactionsSummaryData.create(sum.getTotalCredit(), sum.getTotalDebit());
        int size = transactions.size();
        ArrayList<CurrentTransactionData> entries = new ArrayList<>(size);
        for (CurrentTransaction transaction : transactions) {
            entries.add(CurrentTransactionData.create(transaction, clientDetails, currency, statementType,
                    transactionDetails.get(transaction.getId())));
        }

        return new CurrentStatementData(identification, creationDateTime, fromToDate, accountData,
                new AccountBalanceData[] { opening, closure, total }, trSum, entries.toArray(new CurrentTransactionData[size]),
                calcAdditionalInfo(statementType), isConversionAccount);
    }

    @Transient
    @JsonIgnore
    public boolean isConversionAccount() {
        return isConversionAccount;
    }

    @Transient
    @JsonIgnore
    public BigDecimal getClosureBalance() {
        return Arrays.stream(getBalances()).filter(e -> BALANCE_CODE_END_OF_PERIOD.equals(e.getType().getCodeOrProprietary().getCode()))
                .findFirst().map(accountBalanceData -> accountBalanceData.getAmount().getAmount()).orElse(BigDecimal.ZERO);
    }

    private static String calcAdditionalInfo(int statementType) {
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