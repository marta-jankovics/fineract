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
package org.apache.fineract.currentaccount.statement.service;

import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataService;
import org.apache.fineract.currentaccount.statement.data.CurrentStatementData;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.account.domain.AccountIdentifier;
import org.apache.fineract.portfolio.account.domain.AccountIdentifierRepository;
import org.apache.fineract.portfolio.transaction.data.TransactionParamData;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.service.Camt053StatementGenerator;

@RequiredArgsConstructor
@Slf4j
public class CurrentCamt053StatementGenerator extends Camt053StatementGenerator {

    protected final CurrentAccountRepository accountRepository;
    protected final AccountIdentifierRepository accountIdentifierRepository;
    protected final CurrentTransactionRepository transactionRepository;
    protected final CurrentAccountDailyBalanceReadService dailyBalanceReadService;
    protected final CurrentTransactionMetadataService transactionMetadataService;

    @Override
    protected void addStatementData(@NotNull AccountStatement statement, @NotNull Camt053Data content) {
        String accountId = statement.getAccountId();
        CurrentAccountData accountData = accountRepository.getAccountDataById(accountId);
        if (accountData == null) {
            throw new ResourceNotFoundException("current.account", accountId);
        }
        Map<InteropIdentifierType, AccountIdentifier> identifiersByType = accountIdentifierRepository
                .getByAccountTypeAndAccountId(CURRENT, accountId).stream()
                .collect(Collectors.toMap(AccountIdentifier::getIdentifierType, e -> e));

        LocalDate fromDate = statement.getStatementDate() == null ? accountData.getActivatedOnDate()
                : statement.getStatementDate().plusDays(1);
        LocalDate toDate = statement.getNextStatementDate();

        ICurrentAccountDailyBalance dailyBalance = dailyBalanceReadService.getDailyBalance(accountId, toDate);
        String identification = calcIdentification(statement);
        List<CurrentTransactionType> types = CurrentTransactionType.getFiltered(e -> e.isMonetaryDebit() || e.isMonetaryCredit());
        List<CurrentTransactionData> transactions = transactionRepository.getTransactionsDataForStatement(accountId, fromDate, toDate, types);
        calculateTransactionNames(accountData, transactions);

        OffsetDateTime creationDateTime = content.getGroupHeader().getCreationDateTime();
        CurrentStatementData statementData = CurrentStatementData.create(statement, accountData, dailyBalance, identifiersByType, fromDate,
                toDate, identification, creationDateTime, transactions);
        content.add(statementData, null);
        statement.setStatementBalance(statementData.getClosureBalance());
    }

    protected void calculateTransactionNames(@NotNull CurrentAccountData accountData, @NotNull List<CurrentTransactionData> transactions) {
        List<CurrentTransactionData> transactionsToCalculate = transactions.stream().filter(e -> e.getTransactionName() == null).toList();
        if (transactionsToCalculate.isEmpty()) {
            return;
        }
        Map<String, TransactionParamData> transactionParams = transactionMetadataService.calculateTransactionParams(accountData, transactionsToCalculate);
        for (CurrentTransactionData transactionToCalculate : transactionsToCalculate) {
            TransactionParamData transactionParam = transactionParams.get(transactionToCalculate.getId());
            if (transactionParam != null) { // can not be null
                transactionToCalculate.setTransactionName(transactionParam.getTransactionName());
            }
        }
    }
}
