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
package org.apache.fineract.binx.savings.service;

import static org.apache.fineract.binx.BinxConstants.CONVERSION_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.BinxConstants.DISPOSAL_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.statement.data.BinxMetadata.CONVERSION_ACCOUNT;
import static org.apache.fineract.binx.statement.data.BinxMetadata.DISPOSAL_ACCOUNT;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_ALL;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_BOOKED;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_PENDING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.binx.savings.data.BinxSavingsStatementData;
import org.apache.fineract.binx.savings.data.BinxSavingsTransactionStatementData;
import org.apache.fineract.binx.statement.data.BinxCamt053Data;
import org.apache.fineract.binx.statement.data.BinxMetadata;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsHelper;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.StatementMetadata;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.service.SavingsCamt053StatementGenerator;

@Slf4j
public class BinxSavingsCamt053StatementGenerator extends SavingsCamt053StatementGenerator {

    private final BinxSavingsDetailsReadService detailsReadService;

    public BinxSavingsCamt053StatementGenerator(SavingsAccountRepository accountRepository,
            SavingsAccountTransactionSummaryWrapper summaryWrapper, SavingsHelper savingsHelper,
            BinxSavingsDetailsReadService detailsReadService) {
        super(accountRepository, summaryWrapper, savingsHelper);
        this.detailsReadService = detailsReadService;
    }

    @Override
    @NotNull
    protected BinxCamt053Data createCamt053Data(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType) {
        GroupHeaderData header = createHeader(productType, publishType);
        return new BinxCamt053Data(header);
    }

    @Override
    protected void addStatementData(@NotNull AccountStatement statement, @NotNull Camt053Data content) {
        String accountIdS = statement.getAccountId();
        Long accountId = Long.valueOf(accountIdS);
        SavingsAccount account = accountRepository.findById(accountId).orElseThrow(() -> new SavingsAccountNotFoundException(accountId));
        account.setHelpers(summaryWrapper, savingsHelper);

        Long clientId = account.clientId();
        Map<String, Object> accountDetails = Optional.ofNullable(detailsReadService.getAccountDetails(clientId, accountId))
                .orElseThrow(() -> new SavingsAccountNotFoundException(accountId));
        Long conversionAccountId = Long.valueOf((String) accountDetails.get("conversion_account_id"));
        Long disposalAccountId = Long.valueOf((String) accountDetails.get("disposal_account_id"));

        boolean isConversionAccount = accountId.equals(conversionAccountId);
        boolean isDisposalAccount = accountId.equals(disposalAccountId);
        String accountDiscriminator = isConversionAccount ? CONVERSION_ACCOUNT_DISCRIMINATOR
                : (isDisposalAccount ? DISPOSAL_ACCOUNT_DISCRIMINATOR : null);
        Map<String, Object> clientDetails = detailsReadService.getClientDetails(clientId);

        String identification = calcIdentification(statement);

        LocalDate fromDate = statement.getStatementDate() == null ? account.getActivationDate() : statement.getStatementDate().plusDays(1);
        LocalDate toDate = statement.getNextStatementDate();
        Predicate<SavingsAccountTransaction> predicate = e -> (e.isDebit() || e.isCredit())
                && !DateUtils.isBefore(e.getSubmittedOnDate(), fromDate) && !DateUtils.isAfter(e.getSubmittedOnDate(), toDate);
        List<SavingsAccountTransaction> transactions = account.getTransactionsFiltered(predicate);
        Map<Long, Map<String, Object>> transactionDetailsById = detailsReadService
                .getTransactionDetails(transactions.stream().map(SavingsAccountTransaction::getId).collect(Collectors.toList()));
        List<SavingsAccountTransactionType> debitTypes = SavingsAccountTransactionType.getFiltered(SavingsAccountTransactionType::isDebit);
        for (SavingsAccountTransaction transaction : transactions) {
            Long transactionId = transaction.getId();
            Map<String, Object> transactionDetails = transactionDetailsById.get(transactionId);
            boolean isOutgoing = transaction.isDebit();
            if (transactionDetails == null) {
                transactionDetailsById.put(transactionId, Map.of("isOutgoing", isOutgoing));
            } else {
                if ((isConversionAccount || transaction.isCredit()) && disposalAccountId != null) {
                    String internalCorrelationId = (String) transactionDetails.get("internal_correlation_id");
                    String categoryPurposeCode = (String) transactionDetails.get("category_purpose_code");
                    isOutgoing = detailsReadService.hasTransaction(disposalAccountId, transactionId, internalCorrelationId,
                            categoryPurposeCode, debitTypes);
                }
                transactionDetails.put("isOutgoing", isOutgoing);
            }
        }

        OffsetDateTime creationDateTime = content.getGroupHeader().getCreationDateTime();
        BigDecimal closureBalance;
        if (isConversionAccount) {
            List<Long> pendingTransactionIds = detailsReadService.getPendingTransactionIds(accountId, transactions.stream()
                    .filter(e -> e.getTransactionType().isDeposit()).map(SavingsAccountTransaction::getId).collect(Collectors.toList()));
            List<SavingsAccountTransaction> bookedTransactions = transactions.stream()
                    .filter(e -> !pendingTransactionIds.contains(e.getId())).toList();
            BinxSavingsStatementData booked = BinxSavingsStatementData.create(statement, account, clientDetails, accountDetails, fromDate,
                    toDate, identification, creationDateTime, STATEMENT_TYPE_BOOKED, accountDiscriminator, bookedTransactions,
                    transactionDetailsById);
            content.add(booked, null);

            closureBalance = booked.getClosureBalance();
            List<SavingsAccountTransaction> pendingTransactions = transactions.stream()
                    .filter(e -> pendingTransactionIds.contains(e.getId())).toList();
            if (!pendingTransactions.isEmpty()) {
                BinxSavingsStatementData pending = BinxSavingsStatementData.create(statement, account, clientDetails, accountDetails,
                        fromDate, toDate, identification, creationDateTime, STATEMENT_TYPE_PENDING, accountDiscriminator,
                        pendingTransactions, transactionDetailsById);
                content.add(pending, null);
            }
        } else {
            BinxSavingsStatementData all = BinxSavingsStatementData.create(statement, account, clientDetails, accountDetails, fromDate,
                    toDate, identification, creationDateTime, STATEMENT_TYPE_ALL, accountDiscriminator, transactions,
                    transactionDetailsById);
            content.add(all, null);
            closureBalance = all.getClosureBalance();
        }
        statement.setStatementBalance(closureBalance);
    }

    @NotNull
    @Override
    public String mapContentToString(@NotNull Camt053Data content) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(mapContentToJson(content));
    }

    @NotNull
    @Override
    public JsonNode mapContentToJson(@NotNull Camt053Data content) throws JsonProcessingException {
        JsonNode json = super.mapContentToJson(content);
        JsonNode statements = json.get("Statement");
        if (statements != null) {
            int stmIdx = 0;
            for (JsonNode statement : statements) {
                StatementData statementToAdd = content.getStatements()[stmIdx];
                JsonNode entries = statement.get("Entry");
                if (entries != null) {
                    int entIdx = 0;
                    for (JsonNode entry : entries) {
                        String detailsToAddS = ((BinxSavingsTransactionStatementData) statementToAdd.getTransactions()[entIdx])
                                .getStructuredEntryDetails();
                        if (!Strings.isNullOrEmpty(detailsToAddS)) {
                            ArrayNode detailsToAdd = (ArrayNode) JSON_MAPPER.readTree(detailsToAddS);
                            JsonNode entryDetails = entry.get("EntryDetails");
                            if (entryDetails == null) {
                                ((ObjectNode) entry).set("EntryDetails", detailsToAdd);
                            } else {
                                ((ArrayNode) entryDetails).addAll(detailsToAdd);
                            }
                        }
                        entIdx++;
                    }
                }
                stmIdx++;
            }
        }
        return json;
    }

    @Override
    protected @NotNull BinxMetadata createMetadata(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType) {
        return new BinxMetadata();
    }

    @Override
    protected void addStatementMetadata(@NotNull StatementData statementData, @NotNull StatementMetadata metadata) {
        BinxSavingsStatementData savingsData = (BinxSavingsStatementData) statementData;
        if (savingsData.isPendingType()) {
            return;
        }
        log.debug("Generating statement metadata for id {}", statementData.getIdentification());
        String currency = statementData.getAccount().getCurrency();
        String accountType = Strings.nullToEmpty(
                savingsData.isConversionAccount() ? CONVERSION_ACCOUNT : (savingsData.isDisposalAccount() ? DISPOSAL_ACCOUNT : null));
        String customerId = Strings.nullToEmpty(savingsData.getCustomerId());
        String accountId = Strings.nullToEmpty(savingsData.getAccountId());
        String iban = Strings.nullToEmpty(savingsData.getIban());
        metadata.add(customerId, accountId, iban, accountType, currency, savingsData.getFromDate(), savingsData.getToDate());
    }

    @Override
    @NotNull
    public String calcResultPath(@NotNull PortfolioProductType productType, @NotNull StatementPublishType publishType,
            @NotNull Object content, @NotNull LocalDate transactionDate) {
        String path = super.calcResultPath(productType, publishType, content, transactionDate);
        BinxCamt053Data currentData = (BinxCamt053Data) content;
        String accountType = currentData.isConversionAccount() ? "conversion_account"
                : (currentData.isDisposalAccount() ? "disposal_account" : null);
        if (accountType != null) {
            path = path.substring(0, path.lastIndexOf(File.separator) + 1) + accountType;
        }
        return path;
    }
}
