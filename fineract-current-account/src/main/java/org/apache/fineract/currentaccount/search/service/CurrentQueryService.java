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
package org.apache.fineract.currentaccount.search.service;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENCY_VIRTUAL_COLUMN;
import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.PAYMENT_TYPE_VIRTUAL_COLUMN;
import static org.apache.fineract.infrastructure.dataqueries.data.EntityTables.CURRENT;
import static org.apache.fineract.infrastructure.dataqueries.data.EntityTables.CURRENT_PRODUCT;
import static org.apache.fineract.infrastructure.dataqueries.data.EntityTables.CURRENT_TRANSACTION;

import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.DataTableValidator;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.search.data.JoinColumnHeaderData;
import org.apache.fineract.portfolio.search.data.JoinData;
import org.apache.fineract.portfolio.search.service.AdvancedQueryServiceImpl;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class CurrentQueryService extends AdvancedQueryServiceImpl {

    public CurrentQueryService(PlatformSecurityContext securityContext, GenericDataService genericDataService,
            DatabaseSpecificSQLGenerator sqlGenerator, ReadWriteNonCoreDataService datatableService, DataTableValidator dataTableValidator,
            JdbcTemplate jdbcTemplate) {
        super(securityContext, genericDataService, sqlGenerator, datatableService, dataTableValidator, jdbcTemplate);
    }

    public ResultsetColumnHeaderData resolveCustomColumn(EntityTables entity, @NotNull String virtualColumn,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, String mainAlias,
            JoinType joinType, boolean allowEmpty) {
        if (entity == null) {
            return super.resolveCustomColumn(entity, virtualColumn, headersByName, joins, mainAlias, joinType, allowEmpty);
        }
        String column = virtualColumn.substring(CUSTOM_COLUMN_PREFIX.length());
        ArrayList<JoinData> columnJoins = new ArrayList<>();
        switch (column) {
            case CURRENCY_VIRTUAL_COLUMN -> {
                switch (entity) {
                    case CURRENT_TRANSACTION -> {
                        JoinData join = ensureJoin(joins, CURRENT_TRANSACTION.getApptableName(), "account_id", mainAlias,
                                CURRENT.getApptableName(), "id", joinType);
                        columnJoins.add(join);
                        columnJoins.add(ensureJoin(joins, CURRENT.getApptableName(), "product_id", join.getToAlias(),
                                CURRENT_PRODUCT.getApptableName(), "id", joinType));
                    }
                    case CURRENT -> {
                        columnJoins.add(ensureJoin(joins, CURRENT.getApptableName(), "product_id", mainAlias,
                                CURRENT_PRODUCT.getApptableName(), "id", joinType));
                    }
                    case CURRENT_PRODUCT -> {
                        return headersByName.get("currency_code");
                    }
                    default -> super.resolveCustomColumn(entity, virtualColumn, headersByName, joins, mainAlias, joinType, allowEmpty);
                }
                List<ResultsetColumnHeaderData> columnHeaders = genericDataService
                        .fillResultsetColumnHeaders(CURRENT_PRODUCT.getApptableName());
                ResultsetColumnHeaderData columnHeader = SearchUtil.getFiltered(columnHeaders, e -> e.isNamed("currency_code"));
                JoinColumnHeaderData joinHeader = new JoinColumnHeaderData(columnHeader, virtualColumn, columnJoins);
                headersByName.put(virtualColumn, joinHeader);
                return joinHeader;
            }
            case PAYMENT_TYPE_VIRTUAL_COLUMN -> {
                switch (entity) {
                    case CURRENT_TRANSACTION -> {
                        JoinData join = ensureJoin(joins, CURRENT_TRANSACTION.getApptableName(), "payment_type_id", mainAlias,
                                "m_payment_type", "id", joinType);
                        columnJoins.add(join);
                    }
                    default -> super.resolveCustomColumn(entity, virtualColumn, headersByName, joins, mainAlias, joinType, allowEmpty);
                }
                List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders("m_payment_type");
                ResultsetColumnHeaderData columnHeader = SearchUtil.getFiltered(columnHeaders, e -> e.isNamed("value"));
                JoinColumnHeaderData joinHeader = new JoinColumnHeaderData(columnHeader, virtualColumn, columnJoins);
                headersByName.put(virtualColumn, joinHeader);
                return joinHeader;
            }
            default -> super.resolveCustomColumn(entity, virtualColumn, headersByName, joins, mainAlias, joinType, allowEmpty);
        }
        return null;
    }

    @NotNull
    private static JoinData ensureJoin(@NotNull List<JoinData> joins, @NotNull String fromTable, @NotNull String fromColumn,
            String fromAlias, @NotNull String toTable, @NotNull String toColumn, JoinType joinType) {
        JoinData join = SearchUtil.findFiltered(joins, e -> e.getFromTable().equals(fromTable) && e.getFromColumn().equals(fromColumn)
                && e.getToTable().equals(toTable) && e.getToColumn().equals(toColumn));
        if (join == null) {
            join = new JoinData(fromTable, fromColumn, fromAlias, toTable, toColumn, ("j" + joins.size()), joinType);
            joins.add(join);
        } else {
            join.ensureType(joinType);
        }
        return join;
    }
}
