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

import static org.apache.fineract.portfolio.statement.data.StatementParser.PARAM_STATEMENTS;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.products.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.statement.data.ProductStatementData;
import org.apache.fineract.portfolio.statement.data.StatementParser;
import org.apache.fineract.portfolio.statement.domain.ProductStatement;
import org.apache.fineract.portfolio.statement.domain.ProductStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductStatementServiceImpl implements ProductStatementService {

    private final StatementParser statementParser;
    private final ProductStatementRepository statementRepository;

    @Transactional
    @Override
    public void createProductStatement(Long productId, PortfolioProductType productType, JsonCommand command) {
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
    public Map<String, Object> updateProductStatement(Long productId, PortfolioProductType productType, JsonCommand command) {
        HashMap<String, Object> changes = null;
        if (command.parameterExists(PARAM_STATEMENTS)) {
            final JsonArray statements = command.arrayOfParameterNamed(PARAM_STATEMENTS);
            if (statements != null) {
                changes = new HashMap<>();
                for (int i = 0; i < statements.size(); i++) {
                    final JsonObject statementObject = statements.get(i).getAsJsonObject();
                    ProductStatementData statementData = statementParser.parseProductStatementForUpdate(statementObject);
                    final String code = statementData.getStatementCode();
                    ProductStatement statement = statementRepository
                            .findByProductIdAndProductTypeAndStatementCode(productId, productType, code)
                            .orElseThrow(() -> new ResourceNotFoundException("product.statement", code));
                    HashMap<String, Object> updated = new HashMap<>();
                    statement.update(statementData, updated);
                    statementRepository.save(statement);
                    changes.put(code, updated);
                }
            }
        }
        return changes;
    }
}
