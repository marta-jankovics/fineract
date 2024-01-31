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

import static org.apache.fineract.statement.data.StatementParser.PARAM_STATEMENTS;
import static org.apache.fineract.statement.data.StatementParser.PARAM_STATEMENT_CODE;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.AccountStatementData;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatement;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class AccountStatementServiceImpl implements AccountStatementService {

    protected final StatementParser statementParser;
    protected final ProductStatementRepository productStatementRepository;
    protected final AccountStatementRepository statementRepository;

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return false;
    }

    @Transactional
    @Override
    public void createAccountStatements(Serializable accountId, Serializable productId, PortfolioProductType productType,
            JsonCommand command) {
        if (!preStatementCreate(accountId)) {
            throw new PlatformDataIntegrityException("error.msg.account.statement.create",
                    "Statement can not be created on Account " + accountId, accountId);
        }
        boolean addDefault = true;
        if (command.parameterExists(PARAM_STATEMENTS)) {
            final JsonArray statements = command.arrayOfParameterNamed(PARAM_STATEMENTS);
            if (statements != null) {
                addDefault = false;
                for (int i = 0; i < statements.size(); i++) {
                    final JsonObject statementObject = statements.get(i).getAsJsonObject();
                    AccountStatementData statementData = statementParser.parseAccountStatementForCreate(statementObject, accountId);
                    final String code = statementData.getStatementCode();
                    ProductStatement prodStatement = productStatementRepository.findByStatementCode(code)
                            .orElseThrow(() -> new ResourceNotFoundException("product.statement", code));
                    AccountStatement statement = AccountStatement.create(prodStatement, statementData);
                    statementRepository.save(statement);
                }
            }
        }
        if (addDefault) {
            createDefaultAccountStatements(accountId, productId, productType);
        }
    }

    protected void createDefaultAccountStatements(Serializable accountId, Serializable productId, PortfolioProductType productType) {
        // TODO CURRENT!
        List<ProductStatement> prodStatements = productStatementRepository.findByProductIdAndProductType((Long) productId, productType);
        if (prodStatements.isEmpty()) {
            return;
        }
        AccountStatementData statementData = createDefaultAccountStatementData(accountId);
        for (ProductStatement prodStatement : prodStatements) {
            AccountStatement statement = AccountStatement.create(prodStatement, statementData);
            statementRepository.save(statement);
        }
    }

    @NotNull
    protected AccountStatementData createDefaultAccountStatementData(Serializable accountId) {
        return new AccountStatementData(accountId, null, null, null);
    }

    @Transactional
    @Override
    public Map<String, Object> updateAccountStatements(Serializable accountId, PortfolioProductType productType, JsonCommand command) {
        if (!preStatementCreate(accountId)) {
            throw new PlatformDataIntegrityException("error.msg.account.statement.update",
                    "Statement can not be updated on Account " + accountId, accountId);
        }
        HashMap<String, HashMap<String, String>> changes = null;
        if (command.parameterExists(PARAM_STATEMENTS)) {
            final JsonArray statementArray = command.arrayOfParameterNamed(PARAM_STATEMENTS);
            if (statementArray != null) {
                // TODO CURRENT!
                List<AccountStatement> existingStatements = statementRepository
                        .findByAccountIdAndProductStatementProductType((Long) accountId, productType);
                Map<String, AccountStatement> statementsByCode = existingStatements.stream()
                        .collect(Collectors.toMap(AccountStatement::getStatementCode, v -> v));
                changes = new HashMap<>();
                for (JsonElement statementElement : statementArray) {
                    final JsonObject statementObject = statementElement.getAsJsonObject();
                    // TODO CURRENT!
                    AccountStatementData statementData = statementParser.parseAccountStatementForUpdate(statementObject, (Long) accountId);
                    final String code = statementData.getStatementCode();
                    AccountStatement statement = statementsByCode.get(code);
                    if (statement == null) {
                        ProductStatement prodStatement = productStatementRepository.findByStatementCode(code)
                                .orElseThrow(() -> new ResourceNotFoundException("product.statement", code));
                        statement = AccountStatement.create(prodStatement, statementData);
                        postStatementCreate(statement);
                        changes.computeIfAbsent("added", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, code);
                    } else {
                        HashMap<String, Object> updated = new HashMap<>();
                        statementsByCode.remove(code);
                        if (statement.update(statementData, updated)) {
                            changes.computeIfAbsent("updated", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, code);
                        }
                    }
                    statementRepository.save(statement);
                }
                for (AccountStatement statement : statementsByCode.values()) {
                    statementRepository.delete(statement);
                    changes.computeIfAbsent("deleted", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, statement.getStatementCode());
                }
                if (changes.isEmpty()) {
                    changes = null;
                }
            }
        }
        return (Map) changes;
    }

    @Transactional
    @Override
    public void inheritProductStatement(Serializable productId, PortfolioProductType productType, String statementCode) {
        ProductStatement prodStatement = productStatementRepository.findByStatementCode(statementCode)
                .orElseThrow(() -> new ResourceNotFoundException("product.statement", statementCode));
        List<Serializable> accountIds = getAccountIds(productId, productType);
        for (Serializable accountId : accountIds) {
            if (!preStatementCreate(accountId)) {
                continue;
            }
            // TODO CURRENT!
            Optional<AccountStatement> exisingStatement = statementRepository
                    .findByAccountIdAndProductStatementStatementCode((Long) accountId, statementCode);
            AccountStatement statement;
            if (exisingStatement.isEmpty()) {
                // TODO CURRENT!
                statement = AccountStatement.create(prodStatement, (Long) accountId);
                postStatementCreate(statement);
            } else {
                statement = exisingStatement.get();
                statement.inherit(prodStatement);
            }
            statementRepository.save(statement);
        }
    }

    protected List<Serializable> getAccountIds(Serializable productId, PortfolioProductType productType) {
        return Collections.emptyList();
    }

    protected boolean preStatementCreate(@NotNull Serializable accountId) {
        return true;
    }

    protected void postStatementCreate(@NotNull AccountStatement accountStatement) {
        // nothing to do
    }

    @Override
    public void activateAccountStatements(Serializable accountId, PortfolioProductType productType) {
        // TODO CURRENT!
        List<AccountStatement> statements = statementRepository.findByAccountIdAndProductStatementProductType((Long) accountId,
                productType);
        for (AccountStatement statement : statements) {
            statement.activate();
        }
    }

    @Override
    public void inactivateAccountStatements(Serializable accountId, PortfolioProductType productType) {
        // TODO CURRENT!
        List<AccountStatement> statements = statementRepository.findByAccountIdAndProductStatementProductType((Long) accountId,
                productType);
        for (AccountStatement statement : statements) {
            statement.inactivate();
        }
    }
}
