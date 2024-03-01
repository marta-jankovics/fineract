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
package org.apache.fineract.statement.mapper;

import java.util.List;
import org.apache.fineract.infrastructure.core.config.MapstructMapperConfig;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.statement.data.dto.AccountStatementResponseData;
import org.apache.fineract.statement.data.dto.ProductStatementResponseData;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementResult;
import org.apache.fineract.statement.domain.ProductStatement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapstructMapperConfig.class)
public interface StatementResponseDataMapper {

    default List<ProductStatementResponseData> mapProductStatements(List<ProductStatement> prodStatements) {
        return prodStatements.stream().map(this::map).toList();
    }

    default List<AccountStatementResponseData> mapAccountStatements(List<AccountStatement> accountStatements) {
        return accountStatements.stream().map(this::map).toList();
    }

    @Mapping(target = "statementType", source = "statement", qualifiedByName = "mapStatementType")
    @Mapping(target = "publishType", source = "statement", qualifiedByName = "mapPublishType")
    @Mapping(target = "batchType", source = "statement", qualifiedByName = "mapBatchType")
    ProductStatementResponseData map(ProductStatement statement);

    @Mapping(target = "statementType", source = "statement.productStatement", qualifiedByName = "mapStatementType")
    @Mapping(target = "publishType", source = "statement.productStatement", qualifiedByName = "mapPublishType")
    @Mapping(target = "batchType", source = "statement.productStatement", qualifiedByName = "mapBatchType")
    @Mapping(target = "statementStatus", source = "statement", qualifiedByName = "mapStatementStatus")
    @Mapping(target = "resultCode", source = "statement.statementResult.resultCode")
    @Mapping(target = "resultStatus", source = "statement.statementResult", qualifiedByName = "mapResultStatus")
    @Mapping(target = "resultPublishedOn", source = "statement.statementResult.publishedOn")
    AccountStatementResponseData map(AccountStatement statement);

    @Named("mapStatementType")
    default StringEnumOptionData mapStatementType(ProductStatement statement) {
        return statement.getStatementType().toStringEnumOptionData();
    }

    @Named("mapPublishType")
    default StringEnumOptionData mapPublishType(ProductStatement statement) {
        return statement.getPublishType().toStringEnumOptionData();
    }

    @Named("mapBatchType")
    default StringEnumOptionData mapBatchType(ProductStatement statement) {
        return statement.getBatchType().toStringEnumOptionData();
    }

    @Named("mapStatementStatus")
    default StringEnumOptionData mapStatementStatus(AccountStatement statement) {
        return statement.getStatementStatus().toStringEnumOptionData();
    }

    @Named("mapResultStatus")
    default StringEnumOptionData mapResultStatus(AccountStatementResult statementResult) {
        return statementResult == null ? null : statementResult.getResultStatus().toStringEnumOptionData();
    }
}
