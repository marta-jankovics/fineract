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

import static org.apache.fineract.interoperation.domain.InteropIdentifierType.ALIAS;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.IBAN;
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
import java.util.Optional;
import org.apache.fineract.binx.statement.data.BinxAccountData;
import org.apache.fineract.binx.statement.data.BinxStatementData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.service.account.CurrentAccountResolver;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.domain.AccountIdentifier;
import org.apache.fineract.statement.data.camt053.AccountBalanceData;
import org.apache.fineract.statement.data.camt053.AccountData;
import org.apache.fineract.statement.data.camt053.DateTimePeriodData;
import org.apache.fineract.statement.data.camt053.TransactionStatementData;
import org.apache.fineract.statement.data.camt053.TransactionsSummaryData;
import org.apache.fineract.statement.domain.AccountStatement;

public final class BinxCurrentStatementData extends BinxStatementData {

    private BinxCurrentStatementData(String identification, OffsetDateTime creationDateTime, DateTimePeriodData fromToDate,
            AccountData account, AccountBalanceData[] balances, TransactionsSummaryData transactionsSummary,
            TransactionStatementData[] transactions, String additionalStatementInformation, String accountType, String customerId,
            String accountId, String iban, LocalDate fromDate, LocalDate toDate) {
        super(identification, creationDateTime, fromToDate, account, balances, transactionsSummary, transactions,
                additionalStatementInformation, accountType, customerId, accountId, iban, fromDate, toDate);
    }

    public static BinxCurrentStatementData create(@NotNull AccountStatement statement, @NotNull CurrentAccountData account,
            @NotNull ICurrentAccountDailyBalance dailyBalance, @NotNull Map<InteropIdentifierType, AccountIdentifier> identifiers,
            Map<String, Object> clientDetails, @NotNull LocalDate fromDate, @NotNull LocalDate toDate, @NotNull String identification,
            @NotNull OffsetDateTime creationDateTime, int statementType, String accountDiscriminator,
            @NotNull List<CurrentTransactionData> transactions, @NotNull Map<String, Map<String, Object>> transactionDetails) {
        DateTimePeriodData fromToDate = DateTimePeriodData.create(fromDate, toDate);
        String iban = Optional.ofNullable(identifiers.get(IBAN)).map(AccountIdentifier::getValue).orElse(null);
        Optional<CurrentAccountResolver> otherResolver = Optional.ofNullable(identifiers.get(ALIAS))
                .map(e -> CurrentAccountResolver.resolveInternal(ALIAS, e.getValue(), null));
        CurrentAccountResolver otherIdentifier = otherResolver.orElse(CurrentAccountResolver.resolveDefault(account.getId()));

        String currency = account.getCurrencyCode();
        // only one of the identifiers can be stored here
        AccountData accountData = BinxAccountData.create(null, otherIdentifier.getIdentifier(), otherIdentifier.getTypeName(),
                clientDetails, currency);
        AccountBalanceData opening = AccountBalanceData.create(BALANCE_CODE_BEGIN_OF_PERIOD,
                MathUtil.nullToZero(statement.getStatementBalance()), currency, fromDate);
        BigDecimal balance = dailyBalance.getAccountBalance();
        AccountBalanceData closure = AccountBalanceData.create(BALANCE_CODE_END_OF_PERIOD, balance, currency, toDate);
        BigDecimal holdAmount = dailyBalance.getHoldAmount(); // onHoldFunds is not calculated
        AccountBalanceData total = AccountBalanceData.create(BALANCE_CODE_FULL_OF_PERIOD, MathUtil.subtract(balance, holdAmount), currency,
                toDate);
        BigDecimal totalCredit = transactions.stream().filter(t -> t.getTransactionType().isCredit()).map(CurrentTransactionData::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebit = transactions.stream().filter(t -> t.getTransactionType().isDebit()).map(CurrentTransactionData::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TransactionsSummaryData trSum = TransactionsSummaryData.create(totalCredit, totalDebit);
        int size = transactions.size();
        ArrayList<BinxCurrentTransactionStatementData> entries = new ArrayList<>(size);
        for (CurrentTransactionData transaction : transactions) {
            entries.add(BinxCurrentTransactionStatementData.create(transaction, identifiers, clientDetails, currency, statementType,
                    transactionDetails.get(transaction.getId())));
        }

        String customerId = Strings.nullToEmpty(clientDetails == null ? null : (String) clientDetails.get("customer_id"));
        return new BinxCurrentStatementData(identification, creationDateTime, fromToDate, accountData,
                new AccountBalanceData[] { opening, closure, total }, trSum, entries.toArray(new BinxCurrentTransactionStatementData[size]),
                calcAdditionalInfo(statementType), accountDiscriminator, customerId,
                otherResolver.map(CurrentAccountResolver::getIdentifier).orElse(null), iban, fromDate, toDate);
    }
}