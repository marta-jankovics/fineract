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
package org.apache.fineract.portfolio.savings.statement.service;

import static java.lang.String.format;
import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;
import static org.apache.fineract.portfolio.savings.statement.data.SavingsStatementData.STATEMENT_TYPE_ALL;
import static org.apache.fineract.portfolio.savings.statement.data.SavingsStatementData.STATEMENT_TYPE_BOOKED;
import static org.apache.fineract.portfolio.savings.statement.data.SavingsStatementData.STATEMENT_TYPE_PENDING;
import static org.apache.fineract.portfolio.statement.domain.StatementType.CAMT053;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsHelper;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.portfolio.savings.statement.data.SavingsCamt053Data;
import org.apache.fineract.portfolio.savings.statement.data.SavingsMetaData;
import org.apache.fineract.portfolio.savings.statement.data.SavingsStatementData;
import org.apache.fineract.portfolio.statement.data.AccountStatementGenerationData;
import org.apache.fineract.portfolio.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.portfolio.statement.domain.AccountStatement;
import org.apache.fineract.portfolio.statement.domain.AccountStatementRepository;
import org.apache.fineract.portfolio.statement.domain.AccountStatementResult;
import org.apache.fineract.portfolio.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SavingsStatementGenerationWriteServiceImpl extends AccountStatementGenerationWriteServiceImpl {

    private final SavingsAccountRepository accountRepository;
    private final SavingsStatementService accountStatementService;
    private final SavingsAccountTransactionSummaryWrapper summaryWrapper;
    private final SavingsHelper savingsHelper;

    @Autowired
    public SavingsStatementGenerationWriteServiceImpl(AccountStatementRepository statementRepository,
            AccountStatementResultRepository statementResultRepository, SavingsAccountRepository accountRepository,
            SavingsStatementService accountStatementService, SavingsAccountTransactionSummaryWrapper summaryWrapper,
            SavingsHelper savingsHelper) {
        super(statementRepository, statementResultRepository);
        this.accountRepository = accountRepository;
        this.accountStatementService = accountStatementService;
        this.summaryWrapper = summaryWrapper;
        this.savingsHelper = savingsHelper;
    }

    @Override
    public boolean isSupport(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType) {
        return productType == SAVING && statementType == CAMT053;
    }

    @Override
    protected AccountStatementResult generateResult(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull List<AccountStatementGenerationData> generationBatch,
            @NotNull HashMap<Long, AccountStatement> statements) {
        OffsetDateTime creationDateTime = DateUtils.getAuditOffsetDateTime();
        String messageId = UUID.randomUUID().toString();
        GroupHeaderData headerData = new GroupHeaderData(messageId, creationDateTime);
        SavingsCamt053Data camt053 = new SavingsCamt053Data(headerData);
        SavingsMetaData metadataData = new SavingsMetaData();
        for (AccountStatement statement : statements.values()) {
            log.debug("Generating statement result for id {}", statement.getId());
            generateResultData(statement, camt053, metadataData, creationDateTime);
        }
        String content;
        String metadata;
        try {
            content = camt053.mapToString(JSON_MAPPER);
            metadata = metadataData.mapToString(JSON_MAPPER);
        } catch (JsonProcessingException e) {
            log.error("Statement result json mapping has failed for {} - {} - {}. Reason: {}", productType, statementType, publishType,
                    e.getMessage());
            throw ErrorHandler.getMappable(e);
        }
        LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        String path = calcResultPath(camt053, transactionDate);
        String name = productType.name().toLowerCase() + "_" + statementType.name().toLowerCase() + "_" + transactionDate + "_"
                + messageId.replaceAll("[^a-zA-Z0-9!\\-_.'()$]", "_") + ".json";
        return AccountStatementResult.create(messageId, productType, statementType, publishType, content, metadata, path, name);
    }

    private void generateResultData(AccountStatement statement, SavingsCamt053Data result, SavingsMetaData metadata,
            OffsetDateTime creationDateTime) {
        statement.generate(); // validation

        Long accountId = statement.getAccountId();
        SavingsAccount account = accountRepository.findById(accountId).orElseThrow(() -> new SavingsAccountNotFoundException(accountId));
        account.setHelpers(summaryWrapper, savingsHelper);

        Long clientId = account.clientId();
        Map<String, Object> accountDetails = Optional.ofNullable(accountStatementService.retrieveAccountDetails(clientId, accountId))
                .orElseThrow(() -> new SavingsAccountNotFoundException(accountId));
        boolean isConversionAccount = String.valueOf(accountId).equals(accountDetails.get("conversion_account_id"));
        Long disposalAccountId = isConversionAccount ? Long.valueOf((String) accountDetails.get("disposal_account_id")) : accountId;

        Map<String, Object> clientDetails = accountStatementService.retrieveClientDetails(clientId);

        String pfx = statement.getSequencePrefix();
        LocalDate generationDate = statement.getNextStatementDate();
        int year = generationDate.getYear();
        String seq = StringUtils.leftPad(statement.getSequenceNo().toString(), 2, '0');
        String identification = pfx == null ? format("%s/%s", year, seq) : format("%s-%s/%s", pfx, year, seq);

        LocalDate fromDate = statement.getStatementDate() == null ? account.getActivationDate() : statement.getStatementDate().plusDays(1);
        Predicate<SavingsAccountTransaction> predicate = e -> (e.isDebit() || e.isCredit())
                && !DateUtils.isBefore(e.getSubmittedOnDate(), fromDate) && !DateUtils.isAfter(e.getSubmittedOnDate(), generationDate);
        List<SavingsAccountTransaction> transactions = account.getTransactionsFiltered(predicate);
        Map<Long, Map<String, Object>> transactionDetailsById = accountStatementService
                .retrieveTransactionDetails(transactions.stream().map(SavingsAccountTransaction::getId).collect(Collectors.toList()));
        for (SavingsAccountTransaction transaction : transactions) {
            Long transactionId = transaction.getId();
            Map<String, Object> details = transactionDetailsById.get(transactionId);
            boolean isOutgoing = transaction.isDebit();
            if (details == null) {
                transactionDetailsById.put(transactionId, Map.of("isOutgoing", isOutgoing));
            } else {
                if (isConversionAccount || transaction.isCredit()) {
                    String internalCorrelationId = (String) details.get("internal_correlation_id");
                    String categoryPurposeCode = (String) details.get("category_purpose_code");
                    List<SavingsAccountTransactionType> debitTypes = SavingsAccountTransactionType
                            .getFiltered(SavingsAccountTransactionType::isDebit);
                    isOutgoing = accountStatementService.hasTransaction(disposalAccountId, transactionId, internalCorrelationId,
                            categoryPurposeCode, debitTypes);
                }
                details.put("isOutgoing", isOutgoing);
            }
        }

        BigDecimal closureBalance;
        if (isConversionAccount) {
            List<Long> pendingTransactionIds = accountStatementService.getPendingTransactionIds(accountId, transactions.stream()
                    .filter(e -> e.getTransactionType().isDeposit()).map(SavingsAccountTransaction::getId).collect(Collectors.toList()));
            List<SavingsAccountTransaction> bookedTransactions = transactions.stream()
                    .filter(e -> !pendingTransactionIds.contains(e.getId())).toList();
            SavingsStatementData booked = SavingsStatementData.create(statement, account, clientDetails, accountDetails, fromDate,
                    generationDate, identification, creationDateTime, STATEMENT_TYPE_BOOKED, isConversionAccount, bookedTransactions,
                    transactionDetailsById);
            result.add(booked, null);

            closureBalance = booked.getClosureBalance();
            List<SavingsAccountTransaction> pendingTransactions = transactions.stream()
                    .filter(e -> pendingTransactionIds.contains(e.getId())).toList();
            if (!pendingTransactions.isEmpty()) {
                SavingsStatementData pending = SavingsStatementData.create(statement, account, clientDetails, accountDetails, fromDate,
                        generationDate, identification, creationDateTime, STATEMENT_TYPE_PENDING, isConversionAccount, pendingTransactions,
                        transactionDetailsById);
                result.add(pending, null);
            }
        } else {
            SavingsStatementData all = SavingsStatementData.create(statement, account, clientDetails, accountDetails, fromDate,
                    generationDate, identification, creationDateTime, STATEMENT_TYPE_ALL, isConversionAccount, transactions,
                    transactionDetailsById);
            result.add(all, null);
            closureBalance = all.getClosureBalance();
        }
        metadata.add(clientDetails, accountDetails, isConversionAccount, account.getCurrency().getCode(), fromDate, generationDate);
        statement.setStatementBalance(closureBalance);
    }

    @NotNull
    private static String calcResultPath(@NotNull SavingsCamt053Data camt053, @NotNull LocalDate transactionDate) {
        Boolean conversionAccount = camt053.isConversionAccount();
        int year = transactionDate.get(ChronoField.YEAR);
        int month = transactionDate.get(ChronoField.MONTH_OF_YEAR);
        int day = transactionDate.get(ChronoField.DAY_OF_MONTH);
        String path = year + File.separator + year + '-' + month + File.separator + year + '-' + month + '-' + day;
        if (conversionAccount != null) {
            path += File.separator + (conversionAccount ? "conversion_account" : "disposal_account");
        }
        return path;
    }
}
