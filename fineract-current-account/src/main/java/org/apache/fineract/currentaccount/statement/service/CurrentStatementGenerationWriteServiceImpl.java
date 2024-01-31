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

import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;
import static org.apache.fineract.statement.domain.StatementType.CAMT053;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.statement.data.CurrentCamt053Data;
import org.apache.fineract.currentaccount.statement.data.CurrentMetaData;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.domain.SavingsHelper;
import org.apache.fineract.statement.data.AccountStatementGenerationData;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.AccountStatementResult;
import org.apache.fineract.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CurrentStatementGenerationWriteServiceImpl extends AccountStatementGenerationWriteServiceImpl {

    private final CurrentAccountRepository accountRepository;
    private final CurrentStatementService accountStatementService;
    // private final CurrentTransactionSummaryWrapper summaryWrapper;

    @Autowired
    public CurrentStatementGenerationWriteServiceImpl(AccountStatementRepository statementRepository,
            AccountStatementResultRepository statementResultRepository, CurrentAccountRepository accountRepository,
            CurrentStatementService accountStatementService, // CurrentTransactionSummaryWrapper summaryWrapper,
            SavingsHelper savingsHelper) {
        super(statementRepository, statementResultRepository);
        this.accountRepository = accountRepository;
        this.accountStatementService = accountStatementService;
        // TODO CURRENT!
        // this.summaryWrapper = summaryWrapper;
    }

    @Override
    public boolean isSupport(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType) {
        return productType == SAVING && statementType == CAMT053;
    }

    @Override
    protected AccountStatementResult generateResult(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, @NotNull List<AccountStatementGenerationData> generationBatch,
            @NotNull Map<Long, AccountStatement> statements) {
        OffsetDateTime creationDateTime = DateUtils.getAuditOffsetDateTime();
        String messageId = UUID.randomUUID().toString();
        GroupHeaderData headerData = new GroupHeaderData(messageId, creationDateTime);
        CurrentCamt053Data camt053 = new CurrentCamt053Data(headerData);
        CurrentMetaData metadataData = new CurrentMetaData();
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

    private void generateResultData(AccountStatement statement, CurrentCamt053Data result, CurrentMetaData metadata,
            OffsetDateTime creationDateTime) {
        statement.generate(); // validation

        // TODO CURRENT!
        // Long accountId = statement.getAccountId();
        // String currentId = accountId.toString();
        // CurrentAccount account = accountRepository.findById(currentId)
        // .orElseThrow(() -> new ResourceNotFoundException(CURRENT_ACCOUNT_ENTITY_NAME, currentId));
        // account.setHelpers(summaryWrapper, savingsHelper);

        // Long clientId = account.getClientId();
        // Map<String, Object> accountDetails =
        // Optional.ofNullable(accountStatementService.retrieveAccountDetails(clientId, currentId))
        // .orElseThrow(() -> new ResourceNotFoundException(CURRENT_ACCOUNT_ENTITY_NAME, currentId));
        // boolean isConversionAccount = String.valueOf(accountId).equals(accountDetails.get("conversion_account_id"));
        // Long disposalAccountId = isConversionAccount ? Long.valueOf((String)
        // accountDetails.get("disposal_account_id")) : accountId;
        //
        // Map<String, Object> clientDetails = accountStatementService.retrieveClientDetails(clientId);
        //
        // String pfx = statement.getSequencePrefix();
        // LocalDate generationDate = statement.getNextStatementDate();
        // int year = generationDate.getYear();
        // String seq = StringUtils.leftPad(statement.getSequenceNo().toString(), 2, '0');
        // String identification = pfx == null ? format("%s/%s", year, seq) : format("%s-%s/%s", pfx, year, seq);
        //
        // LocalDate fromDate = statement.getStatementDate() == null ? account.getActivatedOnDate() :
        // statement.getStatementDate().plusDays(1);
        // Predicate<CurrentTransaction> predicate = e -> (e.isDebit() || e.isCredit())
        // && !DateUtils.isBefore(e.getSubmittedOnDate(), fromDate) && !DateUtils.isAfter(e.getSubmittedOnDate(),
        // generationDate);
        // List<CurrentTransaction> transactions = account.getTransactionsFiltered(predicate);
        // Map<String, Map<String, Object>> transactionDetailsById = accountStatementService
        // .retrieveTransactionDetails(transactions.stream().map(CurrentTransaction::getId).collect(Collectors.toList()));
        // for (CurrentTransaction transaction : transactions) {
        // String transactionId = transaction.getId();
        // Map<String, Object> details = transactionDetailsById.get(transactionId);
        // boolean isOutgoing = transaction.isDebit();
        // if (details == null) {
        // transactionDetailsById.put(transactionId, Map.of("isOutgoing", isOutgoing));
        // } else {
        // if (isConversionAccount || transaction.isCredit()) {
        // String internalCorrelationId = (String) details.get("internal_correlation_id");
        // String categoryPurposeCode = (String) details.get("category_purpose_code");
        // List<CurrentTransactionType> debitTypes = CurrentTransactionType
        // .getFiltered(CurrentTransactionType::isDebit);
        // isOutgoing = accountStatementService.hasTransaction(disposalAccountId, transactionId, internalCorrelationId,
        // categoryPurposeCode, debitTypes);
        // }
        // details.put("isOutgoing", isOutgoing);
        // }
        // }
        //
        // BigDecimal closureBalance;
        // if (isConversionAccount) {
        // List<Long> pendingTransactionIds = accountStatementService.getPendingTransactionIds(accountId,
        // transactions.stream()
        // .filter(e ->
        // e.getTransactionType().isDeposit()).map(CurrentTransaction::getId).collect(Collectors.toList()));
        // List<CurrentTransaction> bookedTransactions = transactions.stream()
        // .filter(e -> !pendingTransactionIds.contains(e.getId())).toList();
        // CurrentStatementData booked = CurrentStatementData.create(statement, account, clientDetails, accountDetails,
        // fromDate,
        // generationDate, identification, creationDateTime, STATEMENT_TYPE_BOOKED, isConversionAccount,
        // bookedTransactions,
        // transactionDetailsById);
        // result.add(booked, null);
        //
        // closureBalance = booked.getClosureBalance();
        // List<CurrentTransaction> pendingTransactions = transactions.stream()
        // .filter(e -> pendingTransactionIds.contains(e.getId())).toList();
        // if (!pendingTransactions.isEmpty()) {
        // CurrentStatementData pending = CurrentStatementData.create(statement, account, clientDetails, accountDetails,
        // fromDate,
        // generationDate, identification, creationDateTime, STATEMENT_TYPE_PENDING, isConversionAccount,
        // pendingTransactions,
        // transactionDetailsById);
        // result.add(pending, null);
        // }
        // } else {
        // CurrentStatementData all = CurrentStatementData.create(statement, account, clientDetails, accountDetails,
        // fromDate,
        // generationDate, identification, creationDateTime, STATEMENT_TYPE_ALL, isConversionAccount, transactions,
        // transactionDetailsById);
        // result.add(all, null);
        // closureBalance = all.getClosureBalance();
        // }
        // metadata.add(clientDetails, accountDetails, isConversionAccount, account.getCurrency().getCode(), fromDate,
        // generationDate);
        // statement.setStatementBalance(closureBalance);
    }

    @NotNull
    private String calcResultPath(@NotNull CurrentCamt053Data camt053, @NotNull LocalDate transactionDate) {
        Boolean conversionAccount = camt053.isConversionAccount();
        return calcResultPath(conversionAccount ? "conversion_account" : "disposal_account", transactionDate);
    }
}
