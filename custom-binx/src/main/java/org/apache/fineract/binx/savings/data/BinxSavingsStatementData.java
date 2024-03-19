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

import static org.apache.fineract.interoperation.domain.InteropIdentifierType.ALIAS;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_BEGIN_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_END_OF_PERIOD;
import static org.apache.fineract.statement.data.camt053.AccountBalanceData.BALANCE_CODE_FULL_OF_PERIOD;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.fineract.binx.statement.data.BinxAccountData;
import org.apache.fineract.binx.statement.data.BinxStatementData;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountSummary;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;
import org.apache.fineract.statement.data.camt053.TransactionsSummaryData;
import org.apache.fineract.statement.domain.AccountStatement;

public final class BinxSavingsStatementData extends BinxStatementData {

    private BinxSavingsStatementData(String identification, OffsetDateTime creationDateTime, DateTimePeriodData fromToDate,
            AccountData account, AccountBalanceData[] balances, TransactionsSummaryData transactionsSummary,
            TransactionStatementData[] transactions, String additionalStatementInformation, String accountType, String customerId,
            String accountId, String iban, LocalDate fromDate, LocalDate toDate) {
        super(identification, creationDateTime, fromToDate, account, balances, transactionsSummary, transactions,
                additionalStatementInformation, accountType, customerId, accountId, iban, fromDate, toDate);
    }

    public static BinxSavingsStatementData create(@NotNull AccountStatement statement, @NotNull SavingsAccount account,
            Map<String, Object> clientDetails, @NotNull Map<String, Object> accountDetails, @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate, @NotNull String identification, @NotNull OffsetDateTime creationDateTime, int statementType,
            String accountDiscriminator, @NotNull List<SavingsAccountTransaction> transactions,
            @NotNull Map<Long, Map<String, Object>> transactionDetails) {
        DateTimePeriodData fromToDate = DateTimePeriodData.create(fromDate, toDate);
        String iban = (String) accountDetails.get("iban");
        String otherId = (String) accountDetails.get("internal_account_id");
        String currency = account.getCurrency().getCode();
        // only one of the identifiers can be stored here
        BinxAccountData accountData = BinxAccountData.create(null, otherId, ALIAS.name(), clientDetails, currency);
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
        ArrayList<BinxSavingsTransactionStatementData> entries = new ArrayList<>(size);
        for (SavingsAccountTransaction transaction : transactions) {
            entries.add(BinxSavingsTransactionStatementData.create(transaction, clientDetails, currency, statementType,
                    transactionDetails.get(transaction.getId())));
        }

        String customerId = Strings.nullToEmpty(clientDetails == null ? null : (String) clientDetails.get("customer_id"));
        return new BinxSavingsStatementData(identification, creationDateTime, fromToDate, accountData,
                new AccountBalanceData[] { opening, closure, total }, trSum, entries.toArray(new BinxSavingsTransactionStatementData[size]),
                calcAdditionalInfo(statementType), accountDiscriminator, customerId, otherId, iban, fromDate, toDate);
    }
}
