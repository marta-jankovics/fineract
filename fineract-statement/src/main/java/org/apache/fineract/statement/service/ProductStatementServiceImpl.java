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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.data.dto.ProductStatementData;
import org.apache.fineract.statement.domain.ProductStatement;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.provider.AccountStatementServiceProvider;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class ProductStatementServiceImpl implements ProductStatementService {

    private final StatementParser statementParser;
    private final ProductStatementRepository statementRepository;
    private final AccountStatementServiceProvider serviceProvider;

    @Transactional
    @Override
    public void createProductStatements(@NotNull String productId, @NotNull PortfolioProductType productType,
            @NotNull JsonCommand command) {
        if (command.parameterExists(PARAM_STATEMENTS)) {
            final JsonArray statements = command.arrayOfParameterNamed(PARAM_STATEMENTS);
            if (statements != null) {
                for (int i = 0; i < statements.size(); i++) {
                    final JsonObject statementObject = statements.get(i).getAsJsonObject();
                    ProductStatementData statementData = statementParser.parseProductStatementForCreate(statementObject, productId,
                            productType);
                    ProductStatement statement = ProductStatement.create(statementData);
                    statementRepository.save(statement);
                }
            }
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateProductStatements(@NotNull String productId, @NotNull PortfolioProductType productType,
            @NotNull JsonCommand command) {
        HashMap<String, HashMap<String, String>> changes = null;
        if (command.parameterExists(PARAM_STATEMENTS)) {
            final JsonArray statementArray = command.arrayOfParameterNamed(PARAM_STATEMENTS);
            if (statementArray != null) {
                List<ProductStatement> existingStatements = statementRepository.findByProductIdAndProductType(productId, productType);
                Map<String, ProductStatement> statementsByCode = existingStatements.stream()
                        .collect(Collectors.toMap(ProductStatement::getStatementCode, v -> v));
                changes = new HashMap<>();
                AccountStatementService accountStatementService = null;
                for (JsonElement statementElement : statementArray) {
                    final JsonObject statementObject = statementElement.getAsJsonObject();
                    ProductStatementData statementData = statementParser.parseProductStatementForUpdate(statementObject, productId,
                            productType);
                    final String code = statementData.getStatementCode();
                    ProductStatement statement = statementsByCode.get(code);
                    if (statement == null) {
                        statement = ProductStatement.create(statementData);
                        changes.computeIfAbsent("added", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, code);
                    } else {
                        HashMap<String, Object> updated = new HashMap<>();
                        statementsByCode.remove(code);
                        if (statement.update(statementData, updated)) {
                            changes.computeIfAbsent("updated", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, code);
                        }
                    }
                    statementRepository.save(statement);
                    if (statementData.isInherit()) {
                        if (accountStatementService == null) {
                            accountStatementService = serviceProvider.getAccountStatementService(productType);
                        }
                        accountStatementService.inheritProductStatement(productId, productType, code);
                    }
                }
                for (ProductStatement statement : statementsByCode.values()) {
                    String statementCode = statement.getStatementCode();
                    if (statementRepository.hasAccountReference(statement.getId())) {
                        throw new PlatformDataIntegrityException("error.msg.product.statement.delete",
                                "Product statement can not be deleted because it is used on Account level " + statementCode, statementCode);
                    }
                    statementRepository.delete(statement);
                    changes.computeIfAbsent("deleted", e -> new HashMap<>()).put(PARAM_STATEMENT_CODE, statementCode);
                }
                if (changes.isEmpty()) {
                    changes = null;
                }
            }
        }
        return (Map) changes;
    }
}
