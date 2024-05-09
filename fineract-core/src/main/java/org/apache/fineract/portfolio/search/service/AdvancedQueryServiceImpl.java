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
package org.apache.fineract.portfolio.search.service;

import static jakarta.persistence.criteria.JoinType.INNER;
import static jakarta.persistence.criteria.JoinType.LEFT;
import static org.apache.fineract.portfolio.search.SearchConstants.API_PARAM_COLUMN;
import static org.apache.fineract.portfolio.search.service.SearchUtil.calcAlias;
import static org.apache.fineract.portfolio.search.service.SearchUtil.calcAs;

import com.google.gson.JsonObject;
import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.core.service.database.SqlOperator;
import org.apache.fineract.infrastructure.dataqueries.data.DataTableValidator;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.search.data.AdvancedQueryData;
import org.apache.fineract.portfolio.search.data.AdvancedQueryRequest;
import org.apache.fineract.portfolio.search.data.ColumnConditionData;
import org.apache.fineract.portfolio.search.data.ColumnFilterData;
import org.apache.fineract.portfolio.search.data.ColumnSortData;
import org.apache.fineract.portfolio.search.data.FilterData;
import org.apache.fineract.portfolio.search.data.JoinColumnHeaderData;
import org.apache.fineract.portfolio.search.data.JoinData;
import org.apache.fineract.portfolio.search.data.SelectColumnData;
import org.apache.fineract.portfolio.search.data.TableQueryData;
import org.apache.fineract.portfolio.search.data.WithData;
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

    protected static final String CUSTOM_COLUMN_PREFIX = "#";

    private final PlatformSecurityContext securityContext;
    protected final GenericDataService genericDataService;
    protected final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ReadWriteNonCoreDataService datatableService;
    private final DataTableValidator dataTableValidator;
    private final JdbcTemplate jdbcTemplate;
    private final SearchUtil searchUtil;

    @Override
    public Page<JsonObject> query(@NotNull EntityTables entity, @NotNull PagedLocalRequest<AdvancedQueryRequest> pagedRequest,
            List<ColumnFilterData> addFilters) {
        String apptable = entity.getApptableName();
        AdvancedQueryRequest queryRequest = pagedRequest.getRequest().orElseThrow();
        dataTableValidator.validateTableSearch(queryRequest);

        List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(apptable);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        ResultsetColumnHeaderData pkColumn = SearchUtil.getFiltered(columnHeaders, ResultsetColumnHeaderData::isColumnPrimaryKey);

        AdvancedQueryData baseQuery = queryRequest.getBaseQuery();
        List<TableQueryData> datatableQueries = queryRequest.getDatatableQueries();

        String alias = "m";
        ArrayList<JoinData> joins = new ArrayList<>();
        ArrayList<WithData> withs = new ArrayList<>();
        final ArrayList<ColumnConditionData> columnConditions = new ArrayList<>();
        List<SelectColumnData> selectColumns;
        if (baseQuery == null) {
            selectColumns = new ArrayList<>();
        } else {
            List<ColumnFilterData> columnFilters = baseQuery.getNonNullFilters();
            columnFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                    resolveToJdbcColumn(entity, e.getColumn(), headersByName, joins, withs, alias, INNER, false), e.getFilters())));
            List<String> resultColumns = baseQuery.getNonNullResultColumns();
            selectColumns = resolveToSelectColumns(entity, resultColumns, headersByName, joins, withs, alias);
        }
        if (addFilters != null) {
            addFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                    resolveToJdbcColumn(entity, e.getColumn(), headersByName, joins, withs, alias, INNER, false), e.getFilters())));
        }
        if (selectColumns.isEmpty() && !queryRequest.hasResultColumn()) {
            selectColumns.add(SelectColumnData.of(pkColumn));
        }
        PageRequest pageable = pagedRequest.toPageable();
        final ArrayList<ColumnSortData> columnSorts = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            List<Sort.Order> orders = pageable.getSort().toList();
            orders.forEach(e -> columnSorts.add(new ColumnSortData(
                    resolveToJdbcColumn(entity, e.getProperty(), headersByName, joins, withs, alias, LEFT, false), e.getDirection())));
        } else {
            columnSorts.add(new ColumnSortData(pkColumn, Sort.Direction.DESC));
        }

        if (datatableQueries != null) {
            for (int i = 0; i < datatableQueries.size(); i++) {
                TableQueryData tableQuery = datatableQueries.get(i);
                String dtTable = tableQuery.getTable();
                AdvancedQueryData dtQuery = tableQuery.getQuery();
                List<String> dtResultColumns = dtQuery.getNonNullResultColumns();
                List<ColumnFilterData> dtColumnFilters = dtQuery.getColumnFilters();
                if (dtResultColumns.isEmpty() && (dtColumnFilters == null || dtColumnFilters.isEmpty())) {
                    continue;
                }

                securityContext.authenticatedUser().validateHasDatatableReadPermission(dtTable);
                dtTable = datatableService.validateDatatableRegistered(dtTable);

                String dtAlias = "d" + i;
                JoinData dtJoin = new JoinData(apptable, entity.getRefColumn(), alias, dtTable, entity.getForeignKeyColumnNameOnDatatable(),
                        dtAlias, LEFT);
                List<JoinData> dtJoins = List.of(dtJoin);
                List<WithData> dtWiths = Collections.emptyList();
                List<ResultsetColumnHeaderData> dtColumnHeaders = genericDataService.fillResultsetColumnHeaders(dtTable).stream()
                        .map(e -> new JoinColumnHeaderData(e, null, dtJoins)).collect(Collectors.toList());
                Map<String, ResultsetColumnHeaderData> dtHeadersByName = SearchUtil.mapHeadersToName(dtColumnHeaders);

                List<SelectColumnData> dtSelectColumns = resolveToSelectColumns(null, dtResultColumns, dtHeadersByName, dtJoins, dtWiths,
                        dtAlias);
                for (SelectColumnData dtSelectColumn : dtSelectColumns) {
                    for (SelectColumnData selectColumn : selectColumns) {
                        String dtResultColumn = dtSelectColumn.getResultColumn();
                        if (dtResultColumn.equals(selectColumn.getResultColumn())) {
                            dtSelectColumn.setResultColumn(dtTable + "." + dtResultColumn);
                            break;
                        }
                    }
                }
                selectColumns.addAll(dtSelectColumns);
                if (dtColumnFilters != null) {
                    dtColumnFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                            resolveToJdbcColumn(null, e.getColumn(), dtHeadersByName, dtJoins, dtWiths, dtAlias, INNER, false),
                            e.getFilters())));
                }
                joins.add(dtJoin);
            }
        }
        String dateFormat = pagedRequest.getDateFormat();
        String dateTimeFormat = pagedRequest.getDateTimeFormat();
        Locale locale = pagedRequest.getLocaleObject();
        String with = buildWith(withs);
        String select = searchUtil.buildSelect(selectColumns, alias, false);
        ArrayList<Object> params = new ArrayList<>();
        String from = "\n" + buildFrom(apptable, alias, joins, params, dateFormat, dateTimeFormat, locale);
        String where = "\n" + buildQueryCondition(columnConditions, params, alias, dateFormat, dateTimeFormat, locale);

        List<JsonObject> results = new ArrayList<>();
        Object[] args = params.toArray();

        // Execute the count Query
        String countQuery = with + "SELECT COUNT(*)" + from + where;
        Integer totalElements = jdbcTemplate.queryForObject(countQuery, Integer.class, args); // NOSONAR
        if (totalElements == null || totalElements == 0) {
            return PageableExecutionUtils.getPage(results, pageable, () -> 0);
        }

        StringBuilder query = new StringBuilder(with).append(select).append(from).append(where);
        query.append("\n").append(buildOrderBy(columnSorts, alias));
        if (pageable.isPaged()) {
            query.append(" ").append(sqlGenerator.limit(pageable.getPageSize(), (int) pageable.getOffset()));
        }

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query.toString(), args);

        while (rowSet.next()) {
            SearchUtil.extractJsonResult(rowSet, selectColumns, results);
        }
        return PageableExecutionUtils.getPage(results, pageable, () -> totalElements);
    }

    protected List<SelectColumnData> resolveToSelectColumns(EntityTables entity, @NotNull List<String> columns,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, @NotNull List<WithData> withs,
            String mainAlias) {
        ArrayList<SelectColumnData> result = new ArrayList<>(columns.size());
        for (String column : columns) {
            result.add(
                    SelectColumnData.of(resolveToJdbcColumn(entity, column, headersByName, joins, withs, mainAlias, LEFT, true), column));
        }
        return result;
    }

    protected ResultsetColumnHeaderData resolveToJdbcColumn(EntityTables entity, String column,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, @NotNull List<WithData> withs,
            String mainAlias, JoinType joinType, boolean allowEmpty) {
        if (column != null && column.startsWith(CUSTOM_COLUMN_PREFIX)) {
            ResultsetColumnHeaderData columnHeader = resolveCustomColumn(entity, column, headersByName, joins, withs, mainAlias, joinType,
                    allowEmpty);
            column = calcAs(columnHeader, true);
        }
        ResultsetColumnHeaderData columnHeader = SearchUtil.resolveToJdbcColumn(column, headersByName, allowEmpty);
        if (columnHeader instanceof JoinColumnHeaderData) {
            ((JoinColumnHeaderData) columnHeader).getJoins().forEach(e -> e.ensureType(joinType));
        }
        return columnHeader;
    }

    protected ResultsetColumnHeaderData resolveCustomColumn(EntityTables entity, @NotNull String virtualColumn,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, @NotNull List<WithData> withs,
            String mainAlias, JoinType joinType, boolean allowEmpty) {
        throw new PlatformApiDataValidationException("error.msg.invalid.custom.column", "Custom column is not supported", API_PARAM_COLUMN,
                null, virtualColumn);
    }

    protected String buildWith(@NotNull List<WithData> withs) {
        if (withs.isEmpty()) {
            return "";
        }
        return "WITH " + withs.stream().map(e -> e.getAlias() + " as (\n" + e.getSelect() + "\n)").collect(Collectors.joining(", ")) + '\n';
    }

    protected String buildFrom(@NotNull String mainTable, String mainAlias, @NotNull List<JoinData> joins, @NotNull List<Object> params,
            String dateFormat, String dateTimeFormat, Locale locale) {
        StringBuilder from = new StringBuilder("FROM ").append(sqlGenerator.getFrom(mainTable, mainAlias)).append(" ");
        for (JoinData join : joins) {
            from.append(sqlGenerator.buildJoin(join.getFromColumn(), join.getFromAlias(), join.getToTable(), join.getToColumn(),
                    join.getToAlias(), join.getJoinType()));
            ColumnConditionData joinCondition = join.getJoinCondition();
            if (joinCondition != null) {
                from.append(" AND ");
                buildFilterCondition(joinCondition, from, params, mainAlias, dateFormat, dateTimeFormat, locale);
            }
            from.append(" ");
        }
        return from.toString();
    }

    protected String buildQueryCondition(@NotNull List<ColumnConditionData> columnConditions, @NotNull List<Object> params,
            String mainAlias, String dateFormat, String dateTimeFormat, Locale locale) {
        StringBuilder where = new StringBuilder();
        int isize = columnConditions.size();
        for (int i = 0; i < isize; i++) {
            boolean addedFilter = buildFilterCondition(columnConditions.get(i), where, params, mainAlias, dateFormat, dateTimeFormat,
                    locale);
            if (addedFilter && i < isize - 1) {
                where.append(" AND ");
            }
        }
        return where.toString();
    }

    protected boolean buildFilterCondition(ColumnConditionData columnCondition, @NotNull StringBuilder where, @NotNull List<Object> params,
            String mainAlias, String dateFormat, String dateTimeFormat, Locale locale) {
        ResultsetColumnHeaderData columnHeader = columnCondition.getColumnHeader();
        List<FilterData> filters = columnCondition.getFilters();
        int size = filters.size();
        for (int i = 0; i < size; i++) {
            if (where.isEmpty()) {
                where.append("WHERE ");
            }
            FilterData filter = filters.get(i);
            SqlOperator operator = filter.getOperator();
            List<String> values = filter.getValues();
            List<Object> objectValues = values == null ? null
                    : values.stream().map(e -> searchUtil.parseJdbcColumnValue(columnHeader, e, dateFormat, dateTimeFormat, locale, false))
                            .toList();

            searchUtil.buildCondition(columnHeader.getColumnName(), columnHeader.getColumnType(), operator, objectValues, where, params,
                    calcAlias(columnHeader, mainAlias));
            if (i < size - 1) {
                where.append(" AND ");
            }
        }
        return size > 0;
    }

    protected String buildOrderBy(@NotNull List<ColumnSortData> columnSorts, String mainAlias) {
        if (columnSorts.isEmpty()) {
            return "";
        }
        return "ORDER BY " + columnSorts.stream().map(e -> sqlGenerator.getOrderBy(e.getColumnHeader().getColumnName(),
                calcAlias(e.getColumnHeader(), mainAlias), e.getDirection())).collect(Collectors.joining(", "));
    }
}
