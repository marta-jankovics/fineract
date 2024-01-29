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
package org.apache.fineract.infrastructure.dataqueries.service;

import com.google.gson.JsonObject;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryData;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryRequest;
import org.apache.fineract.infrastructure.dataqueries.data.ColumnFilterData;
import org.apache.fineract.infrastructure.dataqueries.data.DataTableValidator;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.TableQueryData;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AdvancedQueryServiceImpl implements AdvancedQueryService {

    private final GenericDataService genericDataService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ReadWriteNonCoreDataServiceImpl datatableService;
    private final DataTableValidator dataTableValidator;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<JsonObject> query(@NotNull EntityTables entityTables, @NotNull PagedLocalRequest<AdvancedQueryRequest> pagedRequest,
            List<ColumnFilterData> addFilters) {
        String apptable = EntityTables.SAVINGS_TRANSACTION.getApptableName();

        AdvancedQueryRequest queryRequest = pagedRequest.getRequest().orElseThrow();
        dataTableValidator.validateTableSearch(queryRequest);

        List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(apptable);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String pkColumn = SearchUtil.getFiltered(columnHeaders, ResultsetColumnHeaderData::getIsColumnPrimaryKey).getColumnName();

        AdvancedQueryData baseQuery = queryRequest.getBaseQuery();
        List<TableQueryData> datatableQueries = queryRequest.getDatatableQueries();

        List<ColumnFilterData> columnFilters;
        List<String> resultColumns;
        List<String> selectColumns;
        if (baseQuery == null) {
            columnFilters = new ArrayList<>();
            resultColumns = new ArrayList<>();
            selectColumns = new ArrayList<>();
        } else {
            columnFilters = baseQuery.getNonNullFilters();
            columnFilters.forEach(e -> e.setColumn(SearchUtil.validateToJdbcColumnName(e.getColumn(), headersByName, false)));
            resultColumns = baseQuery.getNonNullResultColumns();
            selectColumns = new ArrayList<>(SearchUtil.validateToJdbcColumnNames(resultColumns, headersByName, true));
        }
        if (addFilters != null) {
            columnFilters.addAll(0, addFilters);
        }
        if (resultColumns.isEmpty() && !queryRequest.hasResultColumn()) {
            resultColumns.add(pkColumn);
            selectColumns.add(pkColumn);
        }
        PageRequest pageable = pagedRequest.toPageable();
        PageRequest sortPageable;
        if (pageable.getSort().isSorted()) {
            List<Sort.Order> orders = pageable.getSort().toList();
            sortPageable = pageable.withSort(Sort.by(orders.stream()
                    .map(e -> e.withProperty(SearchUtil.validateToJdbcColumnName(e.getProperty(), headersByName, false))).toList()));
        } else {
            pageable = pageable.withSort(Sort.Direction.DESC, pkColumn);
            sortPageable = pageable;
        }

        String alias = "main";
        String dateFormat = pagedRequest.getDateFormat();
        String dateTimeFormat = pagedRequest.getDateTimeFormat();
        Locale locale = pagedRequest.getLocaleObject();
        StringBuilder select = new StringBuilder(sqlGenerator.buildSelect(selectColumns, alias, false));
        StringBuilder from = new StringBuilder(" ").append(sqlGenerator.buildFrom(apptable, alias, false));
        StringBuilder where = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        SearchUtil.buildQueryCondition(columnFilters, where, params, alias, headersByName, dateFormat, dateTimeFormat, locale, false,
                sqlGenerator);

        if (datatableQueries != null) {
            StringBuilder dataSelect = new StringBuilder();
            StringBuilder dataFrom = new StringBuilder();
            StringBuilder dataWhere = new StringBuilder();
            ArrayList<Object> dataParams = new ArrayList<>();
            for (int i = 0; i < datatableQueries.size(); i++) {
                TableQueryData tableQuery = datatableQueries.get(i);
                boolean added = datatableService.buildDataQueryEmbedded(EntityTables.SAVINGS_TRANSACTION, tableQuery.getTable(),
                        tableQuery.getQuery(), selectColumns, dataSelect, dataFrom, dataWhere, dataParams, alias, ("d" + i), dateFormat,
                        dateTimeFormat, locale);
                if (added) {
                    if (!dataSelect.isEmpty()) {
                        select.append(select.isEmpty() ? "SELECT " : ", ").append(dataSelect);
                    }
                    if (!dataFrom.isEmpty()) {
                        from.append(" ").append(dataFrom);
                    }
                    if (!dataWhere.isEmpty()) {
                        where.append(where.isEmpty() ? " WHERE " : " AND ").append(dataWhere);
                    }
                    params.addAll(dataParams);
                    dataSelect.setLength(0);
                    dataFrom.setLength(0);
                    dataWhere.setLength(0);
                    dataParams.clear();
                }
                resultColumns.addAll(tableQuery.getQuery().getNonNullResultColumns());
            }
        }

        List<JsonObject> results = new ArrayList<>();
        Object[] args = params.toArray();

        // Execute the count Query
        String countQuery = "SELECT COUNT(*)" + from + where;
        Integer totalElements = jdbcTemplate.queryForObject(countQuery, Integer.class, args); // NOSONAR
        if (totalElements == null || totalElements == 0) {
            return PageableExecutionUtils.getPage(results, pageable, () -> 0);
        }

        StringBuilder query = new StringBuilder().append(select).append(from).append(where);
        query.append(" ").append(sqlGenerator.buildOrderBy(sortPageable.getSort().toList(), null, false));
        if (pageable.isPaged()) {
            query.append(" ").append(sqlGenerator.limit(pageable.getPageSize(), (int) pageable.getOffset()));
        }

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query.toString(), args);

        while (rowSet.next()) {
            SearchUtil.extractJsonResult(rowSet, selectColumns, resultColumns, results);
        }
        return PageableExecutionUtils.getPage(results, pageable, () -> totalElements);
    }
}
