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
package org.apache.fineract.statement.service;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsHelper;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.statement.data.SavingsStatementData;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.domain.AccountStatement;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsCamt053StatementGenerator extends Camt053StatementGenerator {

    protected final SavingsAccountRepository accountRepository;
    protected final SavingsAccountTransactionSummaryWrapper summaryWrapper;
    protected final SavingsHelper savingsHelper;

    @Override
    protected void addStatementData(@NotNull AccountStatement statement, @NotNull Camt053Data content) {
        String accountIdS = statement.getAccountId();
        Long accountId = Long.valueOf(accountIdS);
        SavingsAccount account = accountRepository.findById(accountId).orElseThrow(() -> new SavingsAccountNotFoundException(accountId));
        account.setHelpers(summaryWrapper, savingsHelper);

        String identification = calcIdentification(statement);

        LocalDate fromDate = statement.getStatementDate() == null ? account.getActivationDate() : statement.getStatementDate().plusDays(1);
        LocalDate toDate = statement.getNextStatementDate();
        Predicate<SavingsAccountTransaction> predicate = e -> (e.isDebit() || e.isCredit())
                && !DateUtils.isBefore(e.getSubmittedOnDate(), fromDate) && !DateUtils.isAfter(e.getSubmittedOnDate(), toDate);
        List<SavingsAccountTransaction> transactions = account.getTransactionsFiltered(predicate);

        OffsetDateTime creationDateTime = content.getGroupHeader().getCreationDateTime();

        SavingsStatementData statementData = SavingsStatementData.create(statement, account, fromDate, toDate, identification,
                creationDateTime, transactions);
        content.add(statementData, null);

        statement.setStatementBalance(statementData.getClosureBalance());
    }
}
