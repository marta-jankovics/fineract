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

import com.google.gson.JsonObject;
import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.fineract.portfolio.search.data.TableQueryData;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ReadWriteNonCoreDataService datatableService;
    private final DataTableValidator dataTableValidator;
    private final JdbcTemplate jdbcTemplate;

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
        final ArrayList<ColumnConditionData> columnConditions = new ArrayList<>();
        List<String> resultColumns;
        List<ResultsetColumnHeaderData> selectColumns;
        if (baseQuery == null) {
            resultColumns = new ArrayList<>();
            selectColumns = new ArrayList<>();
        } else {
            List<ColumnFilterData> columnFilters = baseQuery.getNonNullFilters();
            columnFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                    resolveToJdbcColumn(entity, e.getColumn(), headersByName, joins, alias, INNER, false), e.getFilters())));
            resultColumns = baseQuery.getNonNullResultColumns();
            selectColumns = resolveToJdbcColumns(entity, resultColumns, headersByName, joins, alias, LEFT, true);
        }
        if (addFilters != null) {
            addFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                    resolveToJdbcColumn(entity, e.getColumn(), headersByName, joins, alias, INNER, false), e.getFilters())));
        }
        if (resultColumns.isEmpty() && !queryRequest.hasResultColumn()) {
            resultColumns.add(pkColumn.getColumnName());
            selectColumns.add(pkColumn);
        }
        PageRequest pageable = pagedRequest.toPageable();
        final ArrayList<ColumnSortData> columnSorts = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            List<Sort.Order> orders = pageable.getSort().toList();
            orders.forEach(e -> columnSorts.add(new ColumnSortData(
                    resolveToJdbcColumn(entity, e.getProperty(), headersByName, joins, alias, LEFT, false), e.getDirection())));
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
                List<ResultsetColumnHeaderData> dtColumnHeaders = genericDataService.fillResultsetColumnHeaders(dtTable).stream()
                        .map(e -> new JoinColumnHeaderData(e, null, dtJoins)).collect(Collectors.toList());
                Map<String, ResultsetColumnHeaderData> dtHeadersByName = SearchUtil.mapHeadersToName(dtColumnHeaders);

                List<ResultsetColumnHeaderData> selectHeaders = resolveToJdbcColumns(null, dtResultColumns, dtHeadersByName, dtJoins, dtAlias, LEFT, true);
                selectColumns.addAll(selectHeaders);
                for (ResultsetColumnHeaderData selectHeader : selectHeaders) {
                    String resultColumn = selectHeader.getColumnName();
                    if (resultColumns.contains(resultColumn)) {
                        resultColumn = dtTable + "." + resultColumn;
                    }
                    resultColumns.add(resultColumn);
                }
                if (dtColumnFilters != null) {
                    dtColumnFilters.forEach(e -> columnConditions.add(new ColumnConditionData(
                            resolveToJdbcColumn(null, e.getColumn(), dtHeadersByName, dtJoins, dtAlias, INNER, false), e.getFilters())));
                }
                joins.add(dtJoin);
            }
        }
        String dateFormat = pagedRequest.getDateFormat();
        String dateTimeFormat = pagedRequest.getDateTimeFormat();
        Locale locale = pagedRequest.getLocaleObject();
        String select = buildSelect(selectColumns, alias);
        String from = " " + buildFrom(apptable, alias, joins);
        ArrayList<Object> params = new ArrayList<>();
        String where = buildQueryCondition(columnConditions, params, alias, dateFormat, dateTimeFormat, locale);

        List<JsonObject> results = new ArrayList<>();
        Object[] args = params.toArray();

        // Execute the count Query
        String countQuery = "SELECT COUNT(*)" + from + where;
        Integer totalElements = jdbcTemplate.queryForObject(countQuery, Integer.class, args); // NOSONAR
        if (totalElements == null || totalElements == 0) {
            return PageableExecutionUtils.getPage(results, pageable, () -> 0);
        }

        StringBuilder query = new StringBuilder().append(select).append(from).append(where);
        query.append(" ").append(buildOrderBy(columnSorts, alias));
        if (pageable.isPaged()) {
            query.append(" ").append(sqlGenerator.limit(pageable.getPageSize(), (int) pageable.getOffset()));
        }

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query.toString(), args);

        while (rowSet.next()) {
            SearchUtil.extractJsonResult(rowSet, selectColumns.stream().map(e -> calcAs(e, true)).collect(Collectors.toList()),
                    resultColumns, results);
        }
        return PageableExecutionUtils.getPage(results, pageable, () -> totalElements);
    }

    protected List<ResultsetColumnHeaderData> resolveToJdbcColumns(EntityTables entity, List<String> columns,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, String mainAlias,
            JoinType joinType, boolean allowEmpty) {
        if (columns != null) {
            columns = columns.stream()
                    .map(e -> e != null && e.startsWith(CUSTOM_COLUMN_PREFIX)
                            ? calcAs(resolveCustomColumn(entity, e, headersByName, joins, mainAlias, joinType, allowEmpty), true)
                            : e)
                    .collect(Collectors.toList());
        }
        List<ResultsetColumnHeaderData> columnHeaders = SearchUtil.resolveToJdbcColumns(columns, headersByName, allowEmpty);
        for (ResultsetColumnHeaderData columnHeader : columnHeaders) {
            if (columnHeader instanceof JoinColumnHeaderData) {
                ((JoinColumnHeaderData) columnHeader).getJoins().forEach(e -> e.ensureType(joinType));
            }
        }
        return columnHeaders;
    }

    protected ResultsetColumnHeaderData resolveToJdbcColumn(EntityTables entity, String column,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, String mainAlias,
            JoinType joinType, boolean allowEmpty) {
        if (column != null && column.startsWith(CUSTOM_COLUMN_PREFIX)) {
            ResultsetColumnHeaderData columnHeader = resolveCustomColumn(entity, column, headersByName, joins, mainAlias, joinType,
                    allowEmpty);
            column = columnHeader instanceof JoinColumnHeaderData ? ((JoinColumnHeaderData) columnHeader).getVirtualName()
                    : columnHeader.getColumnName();
        }
        ResultsetColumnHeaderData columnHeader = SearchUtil.resolveToJdbcColumn(column, headersByName, allowEmpty);
        if (columnHeader instanceof JoinColumnHeaderData) {
            ((JoinColumnHeaderData) columnHeader).getJoins().forEach(e -> e.ensureType(joinType));
        }
        return columnHeader;
    }

    protected ResultsetColumnHeaderData resolveCustomColumn(EntityTables entity, @NotNull String virtualColumn,
            @NotNull Map<String, ResultsetColumnHeaderData> headersByName, @NotNull List<JoinData> joins, String mainAlias,
            JoinType joinType, boolean allowEmpty) {
        throw new PlatformApiDataValidationException("error.msg.invalid.custom.column", "Custom column is not supported", API_PARAM_COLUMN,
                null, virtualColumn);
    }

    protected String buildSelect(@NotNull Collection<ResultsetColumnHeaderData> columns, String mainAlias) {
        return "SELECT " + columns.stream().map(e -> sqlGenerator.getSelect(e.getColumnName(), calcAlias(e, mainAlias), calcAs(e, false)))
                .collect(Collectors.joining(", "));
    }

    protected String buildFrom(@NotNull String mainTable, String mainAlias, @NotNull List<JoinData> joins) {
        return "FROM " + sqlGenerator.getFrom(mainTable, mainAlias) + " "
                + joins.stream().map(e -> sqlGenerator.buildJoin(e.getFromColumn(), e.getFromAlias(), e.getToTable(), e.getToColumn(),
                        e.getToAlias(), e.getJoinType())).collect(Collectors.joining(" "));
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
                where.append(" WHERE ");
            }
            FilterData filter = filters.get(i);
            SqlOperator operator = filter.getOperator();
            List<String> values = filter.getValues();
            List<Object> objectValues = values == null ? null
                    : values.stream().map(
                            e -> SearchUtil.parseJdbcColumnValue(columnHeader, e, dateFormat, dateTimeFormat, locale, false, sqlGenerator))
                            .toList();

            SearchUtil.buildCondition(columnHeader.getColumnName(), columnHeader.getColumnType(), operator, objectValues, where, params,
                    calcAlias(columnHeader, mainAlias), sqlGenerator);
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

    @Nullable
    private static String calcAlias(ResultsetColumnHeaderData columnHeader, String mainAlias) {
        return columnHeader instanceof JoinColumnHeaderData ? ((JoinColumnHeaderData) columnHeader).getAlias() : mainAlias;
    }

    @Nullable
    private static String calcAs(ResultsetColumnHeaderData columnHeader, boolean addDefault) {
        return columnHeader instanceof JoinColumnHeaderData ? ((JoinColumnHeaderData) columnHeader).getVirtualName() : (addDefault ? columnHeader.getColumnName() : null);
    }
}
