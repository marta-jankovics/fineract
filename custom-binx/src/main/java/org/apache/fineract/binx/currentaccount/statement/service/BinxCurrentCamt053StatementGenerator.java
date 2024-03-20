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
package org.apache.fineract.binx.currentaccount.statement.service;

import static org.apache.fineract.binx.BinxConstants.CONVERSION_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.BinxConstants.DISPOSAL_ACCOUNT_DISCRIMINATOR;
import static org.apache.fineract.binx.statement.data.BinxMetadata.CONVERSION_ACCOUNT;
import static org.apache.fineract.binx.statement.data.BinxMetadata.DISPOSAL_ACCOUNT;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_ALL;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_BOOKED;
import static org.apache.fineract.binx.statement.data.BinxStatementData.STATEMENT_TYPE_PENDING;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.IBAN;
import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.binx.currentaccount.service.BinxCurrentDetailsReadService;
import org.apache.fineract.binx.currentaccount.statement.data.BinxCurrentStatementData;
import org.apache.fineract.binx.currentaccount.statement.data.BinxCurrentTransactionStatementData;
import org.apache.fineract.binx.statement.data.BinxCamt053Data;
import org.apache.fineract.binx.statement.data.BinxMetadata;
import org.apache.fineract.currentaccount.data.account.CurrentAccountData;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.account.ICurrentAccountDailyBalance;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.transaction.CurrentTransactionRepository;
import org.apache.fineract.currentaccount.service.account.write.CurrentAccountDailyBalanceReadService;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionMetadataService;
import org.apache.fineract.currentaccount.statement.service.CurrentCamt053StatementGenerator;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.account.domain.AccountIdentifier;
import org.apache.fineract.portfolio.account.domain.AccountIdentifierRepository;
import org.apache.fineract.statement.data.camt053.Camt053Data;
import org.apache.fineract.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.statement.data.camt053.StatementData;
import org.apache.fineract.statement.data.camt053.StatementMetadata;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.StatementPublishType;

@Slf4j
public class BinxCurrentCamt053StatementGenerator extends CurrentCamt053StatementGenerator {

    private final BinxCurrentDetailsReadService detailsReadService;

    public BinxCurrentCamt053StatementGenerator(CurrentAccountRepository accountRepository,
            AccountIdentifierRepository accountIdentifierRepository, CurrentTransactionRepository transactionRepository,
            CurrentAccountDailyBalanceReadService dailyBalanceReadService, CurrentTransactionMetadataService transactionMetadataService,
            BinxCurrentDetailsReadService detailsReadService) {
        super(accountRepository, accountIdentifierRepository, transactionRepository, dailyBalanceReadService, transactionMetadataService);
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

        AccountIdentifier iban = identifiersByType.get(IBAN);
        boolean isConversionAccount = false;
        String accountDiscriminator = null;
        if (iban != null) {
            String subValue = iban.getSubValue();
            if (CONVERSION_ACCOUNT_DISCRIMINATOR.equalsIgnoreCase(subValue)) {
                isConversionAccount = true;
                accountDiscriminator = CONVERSION_ACCOUNT_DISCRIMINATOR;
            } else if (DISPOSAL_ACCOUNT_DISCRIMINATOR.equalsIgnoreCase(subValue)) {
                accountDiscriminator = DISPOSAL_ACCOUNT_DISCRIMINATOR;
            }
        }

        Long clientId = accountData.getClientId();
        Map<String, Object> clientDetails = detailsReadService.getClientDetails(clientId);

        String identification = calcIdentification(statement);

        List<CurrentTransactionType> types = CurrentTransactionType.getFiltered(e -> e.isMonetaryDebit() || e.isMonetaryCredit());
        List<CurrentTransactionData> transactions = transactionRepository.getTransactionsDataForStatement(accountId, fromDate, toDate,
                types);
        calculateTransactionNames(accountData, transactions);

        Map<String, Map<String, Object>> transactionDetailsById = detailsReadService
                .getTransactionDetails(transactions.stream().map(CurrentTransactionData::getId).collect(Collectors.toList()));

        OffsetDateTime creationDateTime = content.getGroupHeader().getCreationDateTime();
        BigDecimal closureBalance;
        if (isConversionAccount) {
            List<String> pendingTransactionIds = detailsReadService.getPendingTransactionIds(accountId, transactions.stream()
                    .filter(e -> e.getTransactionType().isDeposit()).map(CurrentTransactionData::getId).collect(Collectors.toList()));
            List<CurrentTransactionData> bookedTransactions = transactions.stream().filter(e -> !pendingTransactionIds.contains(e.getId()))
                    .toList();
            BinxCurrentStatementData booked = BinxCurrentStatementData.create(statement, accountData, dailyBalance, identifiersByType,
                    clientDetails, fromDate, toDate, identification, creationDateTime, STATEMENT_TYPE_BOOKED, accountDiscriminator,
                    bookedTransactions, transactionDetailsById);
            content.add(booked, null);

            closureBalance = booked.getClosureBalance();
            List<CurrentTransactionData> pendingTransactions = transactions.stream().filter(e -> pendingTransactionIds.contains(e.getId()))
                    .toList();
            if (!pendingTransactions.isEmpty()) {
                BinxCurrentStatementData pending = BinxCurrentStatementData.create(statement, accountData, dailyBalance, identifiersByType,
                        clientDetails, fromDate, toDate, identification, creationDateTime, STATEMENT_TYPE_PENDING, accountDiscriminator,
                        pendingTransactions, transactionDetailsById);
                content.add(pending, null);
            }
        } else {
            BinxCurrentStatementData all = BinxCurrentStatementData.create(statement, accountData, dailyBalance, identifiersByType,
                    clientDetails, fromDate, toDate, identification, creationDateTime, STATEMENT_TYPE_ALL, accountDiscriminator,
                    transactions, transactionDetailsById);
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
                        String detailsToAddS = ((BinxCurrentTransactionStatementData) statementToAdd.getTransactions()[entIdx])
                                .getStructuredEntryDetails();
                        if (!Strings.isNullOrEmpty(detailsToAddS)) {
                            ArrayNode entryDetails = (ArrayNode) entry.get("EntryDetails");
                            if (entryDetails == null) {
                                entryDetails = JSON_MAPPER.createArrayNode();
                                ((ObjectNode) entry).set("EntryDetails", entryDetails);
                            }
                            JsonNode entryDetail;
                            if (entryDetails.isEmpty()) {
                                entryDetail = JSON_MAPPER.createObjectNode();
                                entryDetails.add(entryDetail);
                            } else {
                                entryDetail = entryDetails.get(0);
                            }
                            JsonNode detailsToAdd = JSON_MAPPER.readTree(detailsToAddS);
                            ArrayNode transactionDetails = (ArrayNode) entryDetail.get("TransactionDetails");
                            if (transactionDetails == null) {
                                transactionDetails = JSON_MAPPER.createArrayNode();
                                ((ObjectNode) entryDetail).set("TransactionDetails", transactionDetails);
                                transactionDetails.add(detailsToAdd);
                            } else {
                                transactionDetails.set(0, detailsToAdd);
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
        BinxCurrentStatementData currentData = (BinxCurrentStatementData) statementData;
        if (currentData.isPendingType()) {
            return;
        }
        log.debug("Generating statement metadata for id {}", statementData.getIdentification());
        String currency = statementData.getAccount().getCurrency();
        String accountType = Strings.nullToEmpty(
                currentData.isConversionAccount() ? CONVERSION_ACCOUNT : (currentData.isDisposalAccount() ? DISPOSAL_ACCOUNT : null));
        String customerId = Strings.nullToEmpty(currentData.getCustomerId());
        String accountId = Strings.nullToEmpty(currentData.getAccountId());
        String iban = Strings.nullToEmpty(currentData.getIban());
        metadata.add(customerId, accountId, iban, accountType, currency, currentData.getFromDate(), currentData.getToDate());
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
