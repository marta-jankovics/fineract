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

import static java.util.Arrays.asList;
import static org.apache.fineract.infrastructure.core.service.database.JdbcJavaType.BIGINT;
import static org.apache.fineract.infrastructure.core.service.database.JdbcJavaType.DATETIME;
import static org.apache.fineract.infrastructure.core.service.database.SqlOperator.EQ;
import static org.apache.fineract.infrastructure.core.service.database.SqlOperator.IN;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_AFTER;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_CODE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_INDEXED;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_LENGTH;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_MANDATORY;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_NAME;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_NEWCODE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_NEWNAME;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_TYPE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_TYPE_DROPDOWN;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_FIELD_UNIQUE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_ADDCOLUMNS;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_APPTABLE_NAME;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_CHANGECOLUMNS;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_COLUMNS;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_DATATABLE_NAME;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_DROPCOLUMNS;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_MULTIROW;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.API_PARAM_SUBTYPE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.CREATEDAT_FIELD_NAME;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.TABLE_FIELD_ID;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.TABLE_REGISTERED_TABLE;
import static org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant.UPDATEDAT_FIELD_NAME;
import static org.apache.fineract.portfolio.search.SearchConstants.API_PARAM_DATETIME_FORMAT;
import static org.apache.fineract.portfolio.search.SearchConstants.API_PARAM_DATE_FORMAT;
import static org.apache.fineract.portfolio.search.SearchConstants.API_PARAM_LOCALE;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.PersistenceException;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.service.CodeReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.serialization.DatatableCommandFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.MathUtil;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.core.service.database.DatabaseType;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.apache.fineract.infrastructure.core.service.database.JdbcJavaType;
import org.apache.fineract.infrastructure.dataqueries.api.DataTableApiConstant;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryData;
import org.apache.fineract.infrastructure.dataqueries.data.ColumnFilterData;
import org.apache.fineract.infrastructure.dataqueries.data.DataTableValidator;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableEntryRequiredException;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableSystemErrorException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.SqlInjectionPreventerService;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.infrastructure.security.utils.SQLInjectionValidator;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class ReadWriteNonCoreDataServiceImpl implements ReadWriteNonCoreDataService {

    private static final String CODE_VALUES_TABLE = "m_code_value";

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseTypeResolver databaseTypeResolver;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;
    private final FromJsonHelper fromJsonHelper;
    private final GenericDataService genericDataService;
    private final DatatableCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final ConfigurationDomainService configurationDomainService;
    private final CodeReadPlatformService codeReadPlatformService;
    private final DataTableValidator datatableValidator;
    private final ColumnValidator columnValidator;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SqlInjectionPreventerService preventSqlInjectionService;
    private final DatatableKeywordGenerator datatableKeywordGenerator;

    @Override
    public List<DatatableData> retrieveDatatables(final String appTable) {
        // PERMITTED datatables
        String sql = "select application_table_name, registered_table_name, entity_subtype from x_registered_table where exists"
                + " (select 'f' from m_appuser_role ur join m_role r on r.id = ur.role_id"
                + " left join m_role_permission rp on rp.role_id = r.id left join m_permission p on p.id = rp.permission_id"
                + " where ur.appuser_id = ? and (p.code in ('ALL_FUNCTIONS', 'ALL_FUNCTIONS_READ') or p.code = concat"
                + "('READ_', registered_table_name))) ";

        Object[] params;
        if (appTable != null) {
            sql = sql + " and application_table_name like ? ";
            params = new Object[] { this.context.authenticatedUser().getId(), appTable };
        } else {
            params = new Object[] { this.context.authenticatedUser().getId() };
        }
        sql = sql + " order by application_table_name, registered_table_name";

        final List<DatatableData> datatables = new ArrayList<>();

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, params); // NOSONAR
        while (rowSet.next()) {
            final String appTableName = rowSet.getString("application_table_name");
            final String registeredDatatableName = rowSet.getString("registered_table_name");
            final String entitySubType = rowSet.getString("entity_subtype");
            final List<ResultsetColumnHeaderData> columnHeaderData = genericDataService.fillResultsetColumnHeaders(registeredDatatableName);

            datatables.add(DatatableData.create(appTableName, registeredDatatableName, entitySubType, columnHeaderData));
        }

        return datatables;
    }

    @Override
    public DatatableData retrieveDatatable(final String datatable) {
        // PERMITTED datatables
        SQLInjectionValidator.validateSQLInput(datatable);
        final String sql = "select application_table_name, registered_table_name, entity_subtype from x_registered_table "
                + " where exists (select 'f' from m_appuser_role ur join m_role r on r.id = ur.role_id"
                + " left join m_role_permission rp on rp.role_id = r.id left join m_permission p on p.id = rp.permission_id"
                + " where ur.appuser_id = ? and registered_table_name=? and (p.code in ('ALL_FUNCTIONS', "
                + "'ALL_FUNCTIONS_READ') or p.code = concat('READ_', registered_table_name))) "
                + " order by application_table_name, registered_table_name";

        DatatableData datatableData = null;

        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, new Object[] { this.context.authenticatedUser().getId(), datatable }); // NOSONAR
        if (rowSet.next()) {
            final String appTableName = rowSet.getString("application_table_name");
            final String registeredDatatableName = rowSet.getString("registered_table_name");
            final String entitySubType = rowSet.getString("entity_subtype");
            final List<ResultsetColumnHeaderData> columnHeaderData = this.genericDataService
                    .fillResultsetColumnHeaders(registeredDatatableName);

            datatableData = DatatableData.create(appTableName, registeredDatatableName, entitySubType, columnHeaderData);
        }

        return datatableData;
    }

    @Override
    public List<JsonObject> queryDataTable(@NotNull String datatable, @NotNull String columnName, String columnValueString,
            @NotNull String resultColumnsString) {
        datatable = validateDatatableRegistered(datatable);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil
                .mapHeadersToName(genericDataService.fillResultsetColumnHeaders(datatable));

        List<String> resultColumns = asList(resultColumnsString.split(","));
        List<String> selectColumns = SearchUtil.validateToJdbcColumnNames(resultColumns, headersByName, false);
        ResultsetColumnHeaderData column = SearchUtil.validateToJdbcColumn(columnName, headersByName, false);

        Object columnValue = SearchUtil.parseJdbcColumnValue(column, columnValueString, null, null, null, false, sqlGenerator);
        String sql = sqlGenerator.buildSelect(selectColumns, null, false) + " " + sqlGenerator.buildFrom(datatable, null, false) + " WHERE "
                + EQ.formatPlaceholder(sqlGenerator, column.getColumnName(), 1, null);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, columnValue); // NOSONAR

        List<JsonObject> results = new ArrayList<>();
        while (rowSet.next()) {
            SearchUtil.extractJsonResult(rowSet, selectColumns, resultColumns, results);
        }
        return results;
    }

    @Override
    public Page<JsonObject> queryDataTableAdvanced(@NotNull String datatable, @NotNull PagedLocalRequest<AdvancedQueryData> pagedRequest) {
        datatable = validateDatatableRegistered(datatable);
        context.authenticatedUser().validateHasDatatableReadPermission(datatable);

        AdvancedQueryData request = pagedRequest.getRequest().orElseThrow();
        datatableValidator.validateTableSearch(request);

        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil
                .mapHeadersToName(genericDataService.fillResultsetColumnHeaders(datatable));
        String pkColumn = SearchUtil.getFiltered(headersByName.values(), ResultsetColumnHeaderData::getIsColumnPrimaryKey).getColumnName();

        List<ColumnFilterData> columnFilters = request.getNonNullFilters();
        columnFilters.forEach(e -> e.setColumn(SearchUtil.validateToJdbcColumnName(e.getColumn(), headersByName, false)));

        List<String> resultColumns = request.getNonNullResultColumns();
        List<String> selectColumns;
        if (resultColumns.isEmpty()) {
            resultColumns.add(pkColumn);
            selectColumns = new ArrayList<>();
            selectColumns.add(pkColumn);
        } else {
            selectColumns = SearchUtil.validateToJdbcColumnNames(resultColumns, headersByName, false);
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

        String dateFormat = pagedRequest.getDateFormat();
        String dateTimeFormat = pagedRequest.getDateTimeFormat();
        Locale locale = pagedRequest.getLocaleObject();

        String select = sqlGenerator.buildSelect(selectColumns, null, false);
        String from = " " + sqlGenerator.buildFrom(datatable, null, false);
        StringBuilder where = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        SearchUtil.buildQueryCondition(columnFilters, where, params, null, headersByName, dateFormat, dateTimeFormat, locale, false,
                sqlGenerator);

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

    @Override
    public boolean buildDataQueryEmbedded(@NotNull EntityTables entity, @NotNull String datatable, @NotNull AdvancedQueryData request,
            @NotNull List<String> selectColumns, @NotNull StringBuilder select, @NotNull StringBuilder from, @NotNull StringBuilder where,
            @NotNull List<Object> params, String mainAlias, String alias, String dateFormat, String dateTimeFormat, Locale locale) {
        List<String> resultColumns = request.getResultColumns();
        List<ColumnFilterData> columnFilters = request.getColumnFilters();
        if ((resultColumns == null || resultColumns.isEmpty()) && (columnFilters == null || columnFilters.isEmpty())) {
            return false;
        }

        datatable = validateDatatableRegistered(datatable);
        context.authenticatedUser().validateHasDatatableReadPermission(datatable);

        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil
                .mapHeadersToName(genericDataService.fillResultsetColumnHeaders(datatable));

        List<String> thisSelectColumns = SearchUtil.validateToJdbcColumnNames(resultColumns, headersByName, true);
        if (columnFilters != null) {
            columnFilters.forEach(e -> e.setColumn(SearchUtil.validateToJdbcColumnName(e.getColumn(), headersByName, false)));
        }

        select.append(sqlGenerator.buildSelect(thisSelectColumns, alias, true));
        selectColumns.addAll(thisSelectColumns);

        String joinType = "LEFT";
        if (SearchUtil.buildQueryCondition(columnFilters, where, params, alias, headersByName, dateFormat, dateTimeFormat, locale, true,
                sqlGenerator)) {
            joinType = null; // INNER
        }
        from.append(sqlGenerator.buildJoin(datatable, alias, entity.getForeignKeyColumnNameOnDatatable(), mainAlias, entity.getRefColumn(),
                joinType));
        return true;
    }

    @Transactional
    @Override
    public void registerDatatable(final String datatable, EntityTables entity, final String entitySubType) {
        Integer category = DataTableApiConstant.CATEGORY_DEFAULT;

        final String permissionSql = getPermissionSql(datatable);
        registerDatatable(entity, datatable, entitySubType, category, permissionSql);
    }

    @Transactional
    @Override
    public void registerDatatable(final JsonCommand command) {
        final String datatable = parseDatatableName(command.getUrl());
        registerDatatable(command, getPermissionSql(datatable));
    }

    @Transactional
    @Override
    public void registerDatatable(final JsonCommand command, final String permissionSql) {
        final String entityName = this.parseTableName(command.getUrl());
        EntityTables entity = resolveEntity(entityName);
        final String datatable = this.parseDatatableName(command.getUrl());
        final String entitySubType = command.stringValueOfParameterNamed("entitySubType");
        Integer category = this.getCategory(command);

        this.datatableValidator.validateDataTableRegistration(command.json());

        this.registerDatatable(entity, datatable, entitySubType, category, permissionSql);
    }

    private void registerDatatable(@NotNull EntityTables entity, final String datatable, final String entitySubType, final Integer category,
            final String permissionsSql) {
        String entityName = entity.getName();
        validateDatatable(datatable);
        validateDatatableExists(datatable);

        Map<String, Object> paramMap = new HashMap<>(3);
        final String registerDatatableSql = "insert into x_registered_table "
                + "(registered_table_name, application_table_name, entity_subtype, category) "
                + "values (:dataTableName, :applicationTableName, :entitySubType, :category)";
        paramMap.put("dataTableName", datatable);
        paramMap.put("applicationTableName", entityName);
        paramMap.put("entitySubType", entitySubType);
        paramMap.put("category", category);

        try {
            this.namedParameterJdbcTemplate.update(registerDatatableSql, paramMap);
            this.jdbcTemplate.update(permissionsSql);

            // add the registered table to the config if it is a ppi
            if (category.equals(DataTableApiConstant.CATEGORY_PPI)) {
                this.namedParameterJdbcTemplate
                        .update("insert into c_configuration (name, value, enabled ) values( :dataTableName, '0', false)", paramMap);
            }
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(datatable, null, dve.getMostSpecificCause(), dve);
        } catch (final PersistenceException dve) {
            handleDataIntegrityIssues(datatable, null, ExceptionUtils.getRootCause(dve.getCause()), dve);
        }
    }

    private String getPermissionSql(final String datatable) {
        final String createPermission = "'CREATE_" + datatable + "'";
        final String createPermissionChecker = "'CREATE_" + datatable + "_CHECKER'";
        final String readPermission = "'READ_" + datatable + "'";
        final String updatePermission = "'UPDATE_" + datatable + "'";
        final String updatePermissionChecker = "'UPDATE_" + datatable + "_CHECKER'";
        final String deletePermission = "'DELETE_" + datatable + "'";
        final String deletePermissionChecker = "'DELETE_" + datatable + "_CHECKER'";
        final List<String> escapedColumns = Stream.of("grouping", "code", "action_name", "entity_name", "can_maker_checker")
                .map(sqlGenerator::escape).toList();
        final String columns = String.join(", ", escapedColumns);

        return "insert into m_permission (" + columns + ") values " + "('datatable', " + createPermission + ", 'CREATE', '" + datatable
                + "', true)," + "('datatable', " + createPermissionChecker + ", 'CREATE', '" + datatable + "', false)," + "('datatable', "
                + readPermission + ", 'READ', '" + datatable + "', false)," + "('datatable', " + updatePermission + ", 'UPDATE', '"
                + datatable + "', true)," + "('datatable', " + updatePermissionChecker + ", 'UPDATE', '" + datatable + "', false),"
                + "('datatable', " + deletePermission + ", 'DELETE', '" + datatable + "', true)," + "('datatable', "
                + deletePermissionChecker + ", 'DELETE', '" + datatable + "', false)";
    }

    private Integer getCategory(final JsonCommand command) {
        Integer category = command.integerValueOfParameterNamedDefaultToNullIfZero(DataTableApiConstant.categoryParamName);
        return category == null ? DataTableApiConstant.CATEGORY_DEFAULT : category;
    }

    @Override
    public String parseDatatableName(String url) {
        List<String> urlParts = Splitter.on('/').splitToList(url);
        return urlParts.get(3);
    }

    @Override
    public String parseTableName(String url) {
        List<String> urlParts = Splitter.on('/').splitToList(url);
        return urlParts.get(4);
    }

    @Transactional
    @Override
    public void deregisterDatatable(final String datatable) {
        validateDatatableRegistered(datatable);
        final String permissionList = "('CREATE_" + datatable + "', 'CREATE_" + datatable + "_CHECKER', 'READ_" + datatable + "', 'UPDATE_"
                + datatable + "', 'UPDATE_" + datatable + "_CHECKER', 'DELETE_" + datatable + "', 'DELETE_" + datatable + "_CHECKER')";

        final String deleteRolePermissionsSql = "delete from m_role_permission where m_role_permission.permission_id in (select id from m_permission where code in "
                + permissionList + ")";

        final String deletePermissionsSql = "delete from m_permission where code in " + permissionList;
        final String deleteRegisteredDatatableSql = "delete from x_registered_table where registered_table_name = '" + datatable + "'";
        final String deleteFromConfigurationSql = "delete from c_configuration where name ='" + datatable + "'";

        String[] sqlArray = new String[4];
        sqlArray[0] = deleteRolePermissionsSql;
        sqlArray[1] = deletePermissionsSql;
        sqlArray[2] = deleteRegisteredDatatableSql;
        sqlArray[3] = deleteFromConfigurationSql;

        this.jdbcTemplate.batchUpdate(sqlArray); // NOSONAR
    }

    private void parseDatatableColumnObjectForCreate(final JsonObject column, StringBuilder sqlBuilder,
            final StringBuilder constrainBuilder, final String datatableAlias, final Map<String, Long> codeMappings,
            final boolean isConstraintApproach) {
        String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        final String type = column.has(API_FIELD_TYPE) ? column.get(API_FIELD_TYPE).getAsString().toLowerCase() : null;
        final Integer length = column.has(API_FIELD_LENGTH) ? column.get(API_FIELD_LENGTH).getAsInt() : null;
        final boolean mandatory = column.has(API_FIELD_MANDATORY) && column.get(API_FIELD_MANDATORY).getAsBoolean();
        final boolean unique = column.has(API_FIELD_UNIQUE) && column.get(API_FIELD_UNIQUE).getAsBoolean();
        final String code = column.has(API_FIELD_CODE) ? column.get(API_FIELD_CODE).getAsString() : null;

        if (StringUtils.isNotBlank(code)) {
            if (isConstraintApproach) {
                codeMappings.put(datatableAlias + "_" + name, this.codeReadPlatformService.retriveCode(code).getId());
                String fkName = "fk_" + datatableAlias + "_" + name;
                constrainBuilder.append(", CONSTRAINT ").append(sqlGenerator.escape(fkName)).append(" ").append("FOREIGN KEY (")
                        .append(sqlGenerator.escape(name)).append(") ").append("REFERENCES ").append(sqlGenerator.escape(CODE_VALUES_TABLE))
                        .append(" (id)");
            } else {
                name = datatableColumnNameToCodeValueName(name, code);
            }
        }
        sqlBuilder.append(sqlGenerator.escape(name));
        if (type != null) {
            sqlBuilder.append(" ").append(mapApiTypeToDbType(type, length));
        }

        if (unique) {
            String uniqueKeyName = datatableKeywordGenerator.generateUniqueKeyName(datatableAlias, name);
            constrainBuilder.append(", CONSTRAINT ").append(sqlGenerator.escape(uniqueKeyName)).append(" ").append("UNIQUE (")
                    .append(sqlGenerator.escape(name)).append(")");
        }

        if (mandatory) {
            sqlBuilder.append(" NOT NULL");
        } else {
            sqlBuilder.append(" DEFAULT NULL");
        }

        sqlBuilder.append(", ");
    }

    @Transactional
    @Override
    public CommandProcessingResult createDatatable(final JsonCommand command) {
        String datatableName = null;
        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final JsonElement element = this.fromJsonHelper.parse(command.json());
            final JsonArray columns = this.fromJsonHelper.extractJsonArrayNamed(API_PARAM_COLUMNS, element);
            datatableName = this.fromJsonHelper.extractStringNamed(API_PARAM_DATATABLE_NAME, element);
            String entitySubType = this.fromJsonHelper.extractStringNamed(API_PARAM_SUBTYPE, element);
            final String entityName = this.fromJsonHelper.extractStringNamed(API_PARAM_APPTABLE_NAME, element);
            Boolean multiRow = this.fromJsonHelper.extractBooleanNamed(API_PARAM_MULTIROW, element);

            /*
             * In cases of tables storing hierarchical entities (like m_group), different entities would end up being
             * stored in the same table. Ex: Centers are a specific type of group, add abstractions for the same
             */
            if (multiRow == null) {
                multiRow = false;
            }

            validateDatatable(datatableName);
            EntityTables entity = resolveEntity(entityName);
            final boolean isConstraintApproach = this.configurationDomainService.isConstraintApproachEnabledForDatatables();
            final String fkColumnName = entity.getForeignKeyColumnNameOnDatatable();
            final String datatableAlias = datatableName.toLowerCase().replaceAll("\\s", "_");
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("CREATE TABLE ").append(sqlGenerator.escape(datatableName)).append(" (");

            if (multiRow) {
                if (databaseTypeResolver.isMySQL()) {
                    sqlBuilder.append(TABLE_FIELD_ID).append(" BIGINT NOT NULL AUTO_INCREMENT, ");
                } else if (databaseTypeResolver.isPostgreSQL()) {
                    sqlBuilder.append(TABLE_FIELD_ID).append(
                            " bigint NOT NULL GENERATED BY DEFAULT AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ), ");
                } else {
                    throw new IllegalStateException("Current database is not supported");
                }
            }
            JsonElement fkColumn = addColumn(fkColumnName, entity.getRefColumnType(), true, null, false, false);
            parseDatatableColumnObjectForCreate(fkColumn.getAsJsonObject(), sqlBuilder, null, datatableAlias, null, isConstraintApproach);

            // Add Created At and Updated At
            columns.add(addColumn(CREATEDAT_FIELD_NAME, DATETIME, false, null, false, false));
            columns.add(addColumn(UPDATEDAT_FIELD_NAME, DATETIME, false, null, false, false));

            final Map<String, Long> codeMappings = new HashMap<>();
            final StringBuilder constrainBuilder = new StringBuilder();
            for (final JsonElement column : columns) {
                parseDatatableColumnObjectForCreate(column.getAsJsonObject(), sqlBuilder, constrainBuilder, datatableAlias, codeMappings,
                        isConstraintApproach);
            }

            // Remove trailing comma and space
            sqlBuilder.delete(sqlBuilder.length() - 2, sqlBuilder.length());

            String fkConstraintName = "fk_" + datatableAlias + "_" + fkColumnName;
            if (multiRow) {
                sqlBuilder.append(", PRIMARY KEY (").append(TABLE_FIELD_ID).append(")");
                if (databaseTypeResolver.isMySQL()) {
                    sqlBuilder.append(", KEY ").append(sqlGenerator.escape("fk_" + fkColumnName)).append(" (")
                            .append(sqlGenerator.escape(fkColumnName)).append(")");
                }
            } else {
                sqlBuilder.append(", PRIMARY KEY (").append(sqlGenerator.escape(fkColumnName)).append(")");
            }
            sqlBuilder.append(", CONSTRAINT ").append(sqlGenerator.escape(fkConstraintName)).append(" FOREIGN KEY (")
                    .append(sqlGenerator.escape(fkColumnName)).append(") ").append("REFERENCES ")
                    .append(sqlGenerator.escape(entity.getApptableName())).append(" (").append(entity.getRefColumn()).append(")");

            sqlBuilder.append(constrainBuilder);
            sqlBuilder.append(")");
            if (databaseTypeResolver.isMySQL()) {
                sqlBuilder.append(" ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4;");
            }
            log.debug("SQL:: {}", sqlBuilder);

            jdbcTemplate.execute(sqlBuilder.toString());

            // create indexes
            if (multiRow) {
                createFkIndex(datatableName, fkColumnName);
            }
            // in case of non-multirow, the primary key of the table is the FK and MySQL and PostgreSQL
            // automatically puts an index onto it so no need to create it explicitly

            createIndexesForTable(datatableName, columns);
            registerDatatable(datatableName, entity, entitySubType);
            registerColumnCodeMapping(codeMappings);
        } catch (final PersistenceException | DataAccessException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");

            if (realCause.getMessage().toLowerCase().contains("duplicate column name")) {
                baseDataValidator.reset().parameter(API_FIELD_NAME).failWithCode("duplicate.column.name");
            } else if ((realCause.getMessage().contains("Table") || realCause.getMessage().contains("relation"))
                    && realCause.getMessage().contains("already exists")) {
                baseDataValidator.reset().parameter(API_PARAM_DATATABLE_NAME).value(datatableName).failWithCode("datatable.already.exists");
            } else if (realCause.getMessage().contains("Column") && realCause.getMessage().contains("big")) {
                baseDataValidator.reset().parameter("column").failWithCode("length.too.big");
            } else if (realCause.getMessage().contains("Row") && realCause.getMessage().contains("large")) {
                baseDataValidator.reset().parameter("row").failWithCode("size.too.large");
            }
            baseDataValidator.throwValidationErrors();
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withResourceIdentifier(datatableName).build();
    }

    private JsonElement addColumn(final String name, final JdbcJavaType dataType, final boolean isMandatory, final Integer length,
            final boolean isUnique, final boolean isIndexed) {
        JsonObject column = new JsonObject();
        column.addProperty(API_FIELD_NAME, name);
        column.addProperty(API_FIELD_TYPE, dataType.formatSql(databaseTypeResolver.databaseType(), length));
        column.addProperty(API_FIELD_MANDATORY, Boolean.toString(isMandatory));
        column.addProperty(API_FIELD_UNIQUE, Boolean.toString(isUnique));
        column.addProperty(API_FIELD_INDEXED, Boolean.toString(isIndexed));
        return column;
    }

    @Transactional
    @Override
    public void updateDatatable(final String datatable, final JsonCommand command) {
        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final JsonElement element = this.fromJsonHelper.parse(command.json());
            final JsonArray changeColumns = this.fromJsonHelper.extractJsonArrayNamed(API_PARAM_CHANGECOLUMNS, element);
            final JsonArray addColumns = this.fromJsonHelper.extractJsonArrayNamed(API_PARAM_ADDCOLUMNS, element);
            final JsonArray dropColumns = this.fromJsonHelper.extractJsonArrayNamed(API_PARAM_DROPCOLUMNS, element);
            final String entityName = this.fromJsonHelper.extractStringNamed(API_PARAM_APPTABLE_NAME, element);
            final String entitySubType = this.fromJsonHelper.extractStringNamed(API_PARAM_SUBTYPE, element);

            validateDatatable(datatable);
            int rowCount = getDatatableRowCount(datatable);
            final List<ResultsetColumnHeaderData> columnHeaders = this.genericDataService.fillResultsetColumnHeaders(datatable);
            boolean multiRow = isMultirowDatatable(columnHeaders);

            final boolean isConstraintApproach = this.configurationDomainService.isConstraintApproachEnabledForDatatables();

            if (!StringUtils.isBlank(entitySubType)) {
                jdbcTemplate.update("update x_registered_table SET entity_subtype=? WHERE registered_table_name = ?", // NOSONAR
                        new Object[] { entitySubType, datatable });
            }

            String sqlDatatable = sqlGenerator.escape(datatable);
            if (!StringUtils.isBlank(entityName)) {
                EntityTables oldEntityTable = queryForApplicationEntity(datatable);
                EntityTables entity = resolveEntity(entityName);
                if (entity != oldEntityTable) {
                    JdbcJavaType fkColumnType = entity.getRefColumnType();
                    final String oldFkColumnName = oldEntityTable.getForeignKeyColumnNameOnDatatable();
                    final String fkColumnName = entity.getForeignKeyColumnNameOnDatatable();

                    String oldFkName = sqlGenerator.escape("fk_" + oldFkColumnName);
                    String oldFkConstraintName = sqlGenerator
                            .escape("fk_" + datatable.toLowerCase().replaceAll("\\s", "_") + "_" + oldFkColumnName);
                    String fkName = sqlGenerator.escape("fk_" + fkColumnName);
                    String fkConstraintName = sqlGenerator
                            .escape("fk_" + datatable.toLowerCase().replaceAll("\\s", "_") + "_" + fkColumnName);
                    if (multiRow) {
                        dropIndex(datatable, datatableKeywordGenerator.generateIndexName(datatable, oldFkColumnName));
                    }

                    DatabaseType dialect = databaseTypeResolver.databaseType();
                    String sep = dialect.isMySql() ? ", " : " ";
                    StringBuilder sqlBuilder = new StringBuilder("ALTER TABLE ").append(sqlDatatable);
                    if (multiRow) {
                        sqlBuilder.append(" DROP KEY ").append(oldFkName).append(sep);
                    }
                    sqlBuilder.append("DROP FOREIGN KEY ").append(oldFkConstraintName).append(sep);
                    if (dialect.isMySql()) {
                        sqlBuilder.append("CHANGE ").append(oldFkColumnName).append(" ").append(fkColumnName).append(" ")
                                .append(fkColumnType.formatSql(dialect)).append(" NOT NULL").append(sep);
                    } else {
                        sqlBuilder.append("RENAME COLUMN ").append(oldFkColumnName).append(" TO ").append(fkColumnName).append(" ALTER ")
                                .append(fkColumnName).append(" type ").append(fkColumnType.formatSql(dialect)).append(sep);
                    }
                    if (multiRow) {
                        sqlBuilder.append("ADD KEY ").append(fkName).append(" (").append(fkColumnName).append("),").append(sep);
                    }
                    sqlBuilder.append("ADD CONSTRAINT ").append(fkConstraintName).append(" FOREIGN KEY (").append(fkColumnName).append(") ")
                            .append("REFERENCES ").append(entity.getApptableName()).append(" (").append(entity.getRefColumn()).append(")");

                    this.jdbcTemplate.execute(sqlBuilder.toString());

                    if (multiRow) {
                        createFkIndex(datatable, datatableKeywordGenerator.generateIndexName(datatable, fkColumnName));
                    }

                    deregisterDatatable(datatable);
                    registerDatatable(datatable, entity, entitySubType);
                }
            }

            if (changeColumns == null && addColumns == null && dropColumns == null) {
                return;
            }

            if (dropColumns != null) {
                if (rowCount > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.non.empty.datatable.column.cannot.be.deleted",
                            "Non-empty datatable columns can not be deleted.");
                }
                StringBuilder sqlBuilder = new StringBuilder("ALTER TABLE " + sqlDatatable);
                final StringBuilder constrainBuilder = new StringBuilder();
                final List<String> codeMappings = new ArrayList<>();
                for (final JsonElement column : dropColumns) {
                    parseDatatableColumnForDrop(column.getAsJsonObject(), sqlBuilder, datatable, constrainBuilder, codeMappings);
                }

                // Remove the first comma, right after ALTER TABLE datatable
                final int indexOfFirstComma = sqlBuilder.indexOf(",");
                if (indexOfFirstComma != -1) {
                    sqlBuilder.deleteCharAt(indexOfFirstComma);
                }
                sqlBuilder.append(constrainBuilder);
                this.jdbcTemplate.execute(sqlBuilder.toString());
                deleteColumnCodeMapping(codeMappings);
            }
            if (addColumns != null) {
                StringBuilder sqlBuilder = new StringBuilder("ALTER TABLE " + sqlDatatable);
                final StringBuilder constrainBuilder = new StringBuilder();
                final Map<String, Long> codeMappings = new HashMap<>();
                for (final JsonElement column : addColumns) {
                    JsonObject columnAsJson = column.getAsJsonObject();
                    if (rowCount > 0 && columnAsJson.has(API_FIELD_MANDATORY) && columnAsJson.get(API_FIELD_MANDATORY).getAsBoolean()) {
                        throw new GeneralPlatformDomainRuleException("error.msg.non.empty.datatable.mandatory.column.cannot.be.added",
                                "Non empty datatable mandatory columns can not be added.");
                    }
                    parseDatatableColumnForAdd(columnAsJson, sqlBuilder, datatable.toLowerCase().replaceAll("\\s", "_"), constrainBuilder,
                            codeMappings, isConstraintApproach);
                }

                // Remove the first comma, right after ALTER TABLE datatable
                final int indexOfFirstComma = sqlBuilder.indexOf(",");
                if (indexOfFirstComma != -1) {
                    sqlBuilder.deleteCharAt(indexOfFirstComma);
                }
                sqlBuilder.append(constrainBuilder);
                jdbcTemplate.execute(sqlBuilder.toString());
                createIndexesForTable(datatable, addColumns);
                registerColumnCodeMapping(codeMappings);
            }
            if (changeColumns != null) {
                final StringBuilder renameBuilder = new StringBuilder();
                StringBuilder changeBuilder = new StringBuilder();
                final StringBuilder constrainBuilder = new StringBuilder();
                final Map<String, Long> codeMappings = new HashMap<>();
                final List<String> removeMappings = new ArrayList<>();
                final Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
                for (final JsonElement column : changeColumns) {
                    // remove NULL values from column where mandatory is true
                    removeNullValuesFromStringColumn(datatable, column.getAsJsonObject(), headersByName);
                    parseDatatableColumnForUpdate(column.getAsJsonObject(), headersByName, datatable, renameBuilder, changeBuilder,
                            constrainBuilder, codeMappings, removeMappings, isConstraintApproach);
                }

                // Remove the first comma, right after ALTER TABLE datatable
                StringBuilder sqlBuilder = renameBuilder;
                if (!changeBuilder.isEmpty() || !constrainBuilder.isEmpty()) {
                    int idx = changeBuilder.indexOf(",");
                    if (idx > -1) {
                        changeBuilder.deleteCharAt(idx);
                    } else if ((idx = constrainBuilder.indexOf(",")) > -1) {
                        constrainBuilder.deleteCharAt(idx);
                    }
                    sqlBuilder.append("ALTER TABLE ").append(sqlDatatable).append(changeBuilder).append(constrainBuilder);
                }

                try {
                    if (!sqlBuilder.isEmpty()) {
                        jdbcTemplate.execute(sqlBuilder.toString());
                    }
                    deleteColumnCodeMapping(removeMappings);
                    registerColumnCodeMapping(codeMappings);
                    // update unique constraint
                    updateUniqueConstraintsForTable(datatable, changeColumns, headersByName);
                    // update indexes
                    updateIndexesForTable(datatable, changeColumns, headersByName);
                } catch (final Exception e) {
                    log.error("Exception while modifying a datatable", e);
                    // throw a 503 HTTP error - PlatformServiceUnavailableException
                    if (e.getMessage().contains("Error on rename")) {
                        throw new PlatformServiceUnavailableException("error.msg.datatable.column.update.not.allowed",
                                "One of the column name modification not allowed", e);
                    }
                    if (e.getMessage().toLowerCase().contains("invalid use of null value")) {
                        throw new PlatformServiceUnavailableException("error.msg.datatable.column.update.not.allowed",
                                "One of the data table columns contains null values", e);
                    }
                    // handle all other exceptions in here
                }
            }
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");

            if (realCause.getMessage().toLowerCase().contains("unknown column")) {
                baseDataValidator.reset().parameter(API_FIELD_NAME).failWithCode("does.not.exist");
            } else if (realCause.getMessage().toLowerCase().contains("can't drop")) {
                baseDataValidator.reset().parameter(API_FIELD_NAME).failWithCode("does.not.exist");
            } else if (realCause.getMessage().toLowerCase().contains("duplicate column")) {
                baseDataValidator.reset().parameter(API_FIELD_NAME).failWithCode("column.already.exists");
            }
            baseDataValidator.throwValidationErrors();
        } catch (final PersistenceException ee) {
            Throwable realCause = ExceptionUtils.getRootCause(ee.getCause());
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");
            if (realCause.getMessage().toLowerCase().contains("duplicate column name")) {
                baseDataValidator.reset().parameter(API_FIELD_NAME).failWithCode("duplicate.column.name");
            } else if ((realCause.getMessage().contains("Table") || realCause.getMessage().contains("relation"))
                    && realCause.getMessage().contains("already exists")) {
                baseDataValidator.reset().parameter(API_PARAM_DATATABLE_NAME).value(datatable).failWithCode("datatable.already.exists");
            } else if (realCause.getMessage().contains("Column") && realCause.getMessage().contains("big")) {
                baseDataValidator.reset().parameter("column").failWithCode("length.too.big");
            } else if (realCause.getMessage().contains("Row") && realCause.getMessage().contains("large")) {
                baseDataValidator.reset().parameter("row").failWithCode("size.too.large");
            }
            baseDataValidator.throwValidationErrors();
        }
    }

    private void parseDatatableColumnForAdd(final JsonObject column, StringBuilder sqlBuilder, final String datatableAlias,
            final StringBuilder constrainBuilder, final Map<String, Long> codeMappings, final boolean isConstraintApproach) {
        String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        final String type = column.has(API_FIELD_TYPE) ? column.get(API_FIELD_TYPE).getAsString().toLowerCase() : null;
        final Integer length = column.has(API_FIELD_LENGTH) ? column.get(API_FIELD_LENGTH).getAsInt() : null;
        final boolean mandatory = column.has(API_FIELD_MANDATORY) && column.get(API_FIELD_MANDATORY).getAsBoolean();
        final boolean unique = column.has(API_FIELD_UNIQUE) && column.get(API_FIELD_UNIQUE).getAsBoolean();
        final String after = column.has(API_FIELD_AFTER) ? column.get(API_FIELD_AFTER).getAsString() : null;
        final String code = column.has(API_FIELD_CODE) ? column.get(API_FIELD_CODE).getAsString() : null;

        if (StringUtils.isNotBlank(code)) {
            if (isConstraintApproach) {
                String fkName = "fk_" + datatableAlias + "_" + name;
                codeMappings.put(datatableAlias + "_" + name, this.codeReadPlatformService.retriveCode(code).getId());
                constrainBuilder.append(",ADD CONSTRAINT  ").append(sqlGenerator.escape(fkName)).append(" ").append("FOREIGN KEY (")
                        .append(sqlGenerator.escape(name)).append(") ").append("REFERENCES ").append(sqlGenerator.escape(CODE_VALUES_TABLE))
                        .append(" (").append(TABLE_FIELD_ID).append(")");
            } else {
                name = datatableColumnNameToCodeValueName(name, code);
            }
        }
        sqlBuilder.append(", ADD ").append(sqlGenerator.escape(name)).append(" ").append(mapApiTypeToDbType(type, length));

        if (unique) {
            String uniqueKeyName = datatableKeywordGenerator.generateUniqueKeyName(datatableAlias, name);
            constrainBuilder.append(",ADD CONSTRAINT  ").append(sqlGenerator.escape(uniqueKeyName)).append(" ").append("UNIQUE (")
                    .append(sqlGenerator.escape(name)).append(")");
        }

        if (mandatory) {
            sqlBuilder.append(" NOT NULL");
        } else {
            sqlBuilder.append(" DEFAULT NULL");
        }

        if (after != null) {
            sqlBuilder.append(" AFTER ").append(sqlGenerator.escape(after));
        }
    }

    private void parseDatatableColumnForUpdate(final JsonObject column,
            final Map<String, ResultsetColumnHeaderData> mapColumnNameDefinition, final String datatable, StringBuilder renameBuilder,
            StringBuilder changeBuilder, final StringBuilder constrainBuilder, final Map<String, Long> codeMappings,
            final List<String> removeMappings, final boolean isConstraintApproach) {
        String oldName = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        if (!mapColumnNameDefinition.containsKey(oldName)) {
            throw new PlatformDataIntegrityException("error.msg.datatable.column.missing.update.parse",
                    "Column " + oldName + " does not exist.", oldName);
        }
        final String lengthStr = column.has(API_FIELD_LENGTH) ? column.get(API_FIELD_LENGTH).getAsString() : null;
        Long length = StringUtils.isNotBlank(lengthStr) ? Long.parseLong(lengthStr) : null;
        String newName = column.has(API_FIELD_NEWNAME) ? column.get(API_FIELD_NEWNAME).getAsString() : null;
        final Boolean newMandatory = column.has(API_FIELD_MANDATORY) ? column.get(API_FIELD_MANDATORY).getAsBoolean() : null;
        final String after = column.has(API_FIELD_AFTER) ? column.get(API_FIELD_AFTER).getAsString() : null;
        final String code = column.has(API_FIELD_CODE) ? column.get(API_FIELD_CODE).getAsString() : null;
        final String newCode = column.has(API_FIELD_NEWCODE) ? column.get(API_FIELD_NEWCODE).getAsString() : null;
        final String datatableAlias = datatable.toLowerCase().replaceAll("\\s", "_");
        if (isConstraintApproach) {
            if (StringUtils.isBlank(newName)) {
                newName = oldName;
            }
            String fkName = "fk_" + datatableAlias + "_" + oldName;
            String newFkName = "fk_" + datatableAlias + "_" + newName;
            if (!StringUtils.equalsIgnoreCase(code, newCode) || !StringUtils.equalsIgnoreCase(oldName, newName)) {
                if (StringUtils.equalsIgnoreCase(code, newCode)) {
                    final int codeId = getCodeIdForColumn(datatableAlias, oldName);
                    if (codeId > 0) {
                        removeMappings.add(datatableAlias + "_" + oldName);
                        constrainBuilder.append(", DROP CONSTRAINT ").append(sqlGenerator.escape(fkName)).append(" ");
                        codeMappings.put(datatableAlias + "_" + newName, (long) codeId);
                        constrainBuilder.append(", ADD CONSTRAINT ").append(sqlGenerator.escape(newFkName)).append(" ")
                                .append("FOREIGN KEY (").append(sqlGenerator.escape(newName)).append(") ").append("REFERENCES ")
                                .append(sqlGenerator.escape(CODE_VALUES_TABLE)).append(" (").append(TABLE_FIELD_ID).append(")");
                    }

                } else {
                    if (code != null) {
                        removeMappings.add(datatableAlias + "_" + oldName);
                        if (newCode == null || !StringUtils.equalsIgnoreCase(oldName, newName)) {
                            constrainBuilder.append(", DROP CONSTRAINT ").append(sqlGenerator.escape(fkName)).append(" ");
                        }
                    }
                    if (newCode != null) {
                        codeMappings.put(datatableAlias + "_" + newName, this.codeReadPlatformService.retriveCode(newCode).getId());
                        if (code == null || !StringUtils.equalsIgnoreCase(oldName, newName)) {
                            constrainBuilder.append(", ADD CONSTRAINT  ").append(sqlGenerator.escape(newFkName)).append(" ")
                                    .append("FOREIGN KEY (").append(sqlGenerator.escape(newName)).append(") ").append("REFERENCES ")
                                    .append(sqlGenerator.escape(CODE_VALUES_TABLE)).append(" (").append(TABLE_FIELD_ID).append(")");
                        }
                    }
                }
            }
        } else {
            if (StringUtils.isNotBlank(code)) {
                oldName = datatableColumnNameToCodeValueName(oldName, code);
                if (StringUtils.isNotBlank(newCode)) {
                    newName = datatableColumnNameToCodeValueName(newName, newCode);
                } else {
                    newName = datatableColumnNameToCodeValueName(newName, code);
                }
            }
        }
        DatabaseType dialect = databaseTypeResolver.databaseType();
        ResultsetColumnHeaderData columnHeader = mapColumnNameDefinition.get(oldName);
        final JdbcJavaType type = columnHeader.getColumnType();
        boolean nameChanged = !StringUtils.isBlank(newName) && !newName.equals(oldName);
        boolean lengthChanged = length != null && !length.equals(columnHeader.getColumnLength()) && type.hasPrecision(dialect);
        boolean nullityChanged = newMandatory != null && newMandatory != columnHeader.isMandatory();
        boolean afterChanged = after != null && databaseTypeResolver.isMySQL();
        if (nameChanged || lengthChanged || nullityChanged || afterChanged) {
            Integer precision = length == null ? null : length.intValue();
            Integer scale = null;
            if (type.isDecimalType()) {
                precision = 19;
                scale = 6;
            }
            String colName = StringUtils.isBlank(newName) ? oldName : newName;
            boolean mandatory = newMandatory == null ? columnHeader.isMandatory() : newMandatory;
            if (databaseTypeResolver.isMySQL()) {
                String modifySql = nameChanged ? ("CHANGE " + sqlGenerator.escape(oldName) + " " + sqlGenerator.escape(colName))
                        : (" MODIFY " + sqlGenerator.escape(colName));
                changeBuilder.append(", ").append(modifySql).append(" ").append(type.formatSql(dialect, precision, scale))
                        .append(mandatory ? " NOT NULL" : " DEFAULT NULL");
                if (after != null) {
                    changeBuilder.append(" AFTER ").append(sqlGenerator.escape(after));
                }
            } else {
                if (nameChanged) {
                    renameBuilder.append("ALTER TABLE ").append(sqlGenerator.escape(datatable)).append(" RENAME COLUMN ")
                            .append(sqlGenerator.escape(oldName)).append(" TO ").append(sqlGenerator.escape(newName)).append("; ");
                }
                if (lengthChanged) {
                    changeBuilder.append(", ALTER ").append(sqlGenerator.escape(colName)).append(" type ")
                            .append(type.formatSql(dialect, precision, scale));
                }
                if (nullityChanged) {
                    changeBuilder.append(", ALTER ").append(sqlGenerator.escape(colName))
                            .append(mandatory ? " set not null" : " drop not null");
                }
            }
        }
    }

    private void parseDatatableColumnForDrop(final JsonObject column, StringBuilder sqlBuilder, final String datatable,
            final StringBuilder constrainBuilder, final List<String> codeMappings) {
        final String datatableAlias = datatable.toLowerCase().replaceAll("\\s", "_");
        final String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        if (name == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.missing.datatable.column.name",
                    "Datatable column name to drop is missing.");
        }
        sqlBuilder.append(", DROP COLUMN ").append(sqlGenerator.escape(name)).append(" ");

        String fkName = "fk_" + datatableAlias + "_" + name;
        String schemaSql = databaseTypeResolver.isMySQL() ? "i.TABLE_SCHEMA = SCHEMA()"
                : "i.table_catalog = current_catalog AND i.table_schema = current_schema";
        String findFKSql = "SELECT count(*) FROM information_schema.TABLE_CONSTRAINTS i" + " WHERE i.CONSTRAINT_TYPE = 'FOREIGN KEY' AND "
                + schemaSql + " AND i.TABLE_NAME = '" + datatable + "' AND i.CONSTRAINT_NAME = '" + fkName + "' ";

        final Integer count = this.jdbcTemplate.queryForObject(findFKSql, Integer.class); // NOSONAR
        if (count != null && count > 0) {
            codeMappings.add(datatableAlias + "_" + name);
            constrainBuilder.append(", DROP FOREIGN KEY ").append(sqlGenerator.escape(fkName)).append(" ");
        }
    }

    private void registerColumnCodeMapping(final Map<String, Long> codeMappings) {
        if (codeMappings != null && !codeMappings.isEmpty()) {
            final String[] addSqlList = new String[codeMappings.size()];
            int i = 0;
            for (final Map.Entry<String, Long> mapEntry : codeMappings.entrySet()) {
                addSqlList[i++] = "insert into x_table_column_code_mappings (column_alias_name, code_id) values ('" + mapEntry.getKey()
                        + "'," + mapEntry.getValue() + ");";
            }

            this.jdbcTemplate.batchUpdate(addSqlList);
        }
    }

    private void deleteColumnCodeMapping(final List<String> columnNames) {
        if (columnNames != null && !columnNames.isEmpty()) {
            final String[] deleteSqlList = new String[columnNames.size()];
            int i = 0;
            for (final String columnName : columnNames) {
                deleteSqlList[i++] = "DELETE FROM x_table_column_code_mappings WHERE  column_alias_name='" + columnName + "';";
            }

            this.jdbcTemplate.batchUpdate(deleteSqlList);
        }
    }

    private int getCodeIdForColumn(final String datatableAlias, final String name) {
        final StringBuilder checkColumnCodeMapping = new StringBuilder();
        checkColumnCodeMapping.append("select ccm.code_id from x_table_column_code_mappings ccm where ccm.column_alias_name='")
                .append(datatableAlias).append("_").append(name).append("'");
        Integer codeId = 0;
        try {
            codeId = this.jdbcTemplate.queryForObject(checkColumnCodeMapping.toString(), Integer.class);
        } catch (final EmptyResultDataAccessException e) {
            log.warn("Error occurred.", e);
        }
        return ObjectUtils.defaultIfNull(codeId, 0);
    }

    /**
     * Update data table, set column value to empty string where current value is NULL. Run update SQL only if the
     * "mandatory" property is set to true
     *
     * @param datatable
     *            Name of data table
     * @param column
     *            JSON encoded array of column properties
     * @see <a href="https://mifosforge.jira.com/browse/MIFOSX-1145">MIFOSX-1145</a>
     **/
    private void removeNullValuesFromStringColumn(final String datatable, final JsonObject column,
            final Map<String, ResultsetColumnHeaderData> mapColumnNameDefinition) {
        final boolean mandatory = column.has(API_FIELD_MANDATORY) && column.get(API_FIELD_MANDATORY).getAsBoolean();
        final String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : "";
        final JdbcJavaType type = mapColumnNameDefinition.containsKey(name) ? mapColumnNameDefinition.get(name).getColumnType() : null;

        if (type != null && mandatory && type.isStringType()) {
            String sql = "UPDATE " + sqlGenerator.escape(datatable) + " SET " + sqlGenerator.escape(name) + " = '' WHERE "
                    + sqlGenerator.escape(name) + " IS NULL";
            this.jdbcTemplate.update(sql); // NOSONAR
        }
    }

    private void updateUniqueConstraintsForTable(String datatable, JsonArray changeColumns,
            Map<String, ResultsetColumnHeaderData> mapColumnNameDefinition) {
        for (final JsonElement column : changeColumns) {
            String name = column.getAsJsonObject().has(API_FIELD_NAME) ? column.getAsJsonObject().get(API_FIELD_NAME).getAsString() : null;

            if (!mapColumnNameDefinition.containsKey(name)) {
                throw new PlatformDataIntegrityException("error.msg.datatable.column.missing.update.parse",
                        "Column " + name + " does not exist.", name);
            }

            updateColumnUniqueConstraints(datatable, column.getAsJsonObject(),
                    mapColumnNameDefinition.get(column.getAsJsonObject().get(API_FIELD_NAME).getAsString()));
        }
    }

    private void updateColumnUniqueConstraints(String datatable, JsonObject column, ResultsetColumnHeaderData columnMetaData) {
        // check for unique constraint update
        String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        String columnNewName = column.has(API_FIELD_NEWNAME) ? column.get(API_FIELD_NEWNAME).getAsString() : null;

        final boolean isAlreadyUnique = genericDataService.isExplicitlyUnique(datatable, name);
        boolean setUnique = column.has(API_FIELD_UNIQUE) ? column.get(API_FIELD_UNIQUE).getAsBoolean() : isAlreadyUnique;
        String uniqueKeyName = datatableKeywordGenerator.generateUniqueKeyName(datatable, name);

        if (isAlreadyUnique) {
            if (!setUnique) {
                // drop existing constraint
                dropUniqueConstraint(datatable, uniqueKeyName);
            } else {
                // if columnname changed
                checkColumnRenameAndModifyUniqueConstraint(datatable, columnNewName, uniqueKeyName);
            }
        } else if (setUnique) {
            checkColumnRenameAndCreateUniqueConstraint(datatable, name, columnNewName, uniqueKeyName);
        }
    }

    private void checkColumnRenameAndCreateUniqueConstraint(String datatable, String name, String columnNewName, String constraintKey) {
        if (columnNewName != null) {
            // create constraint with new column name
            String uniqueKeyName = datatableKeywordGenerator.generateUniqueKeyName(datatable, columnNewName);
            createUniqueConstraint(datatable, columnNewName, uniqueKeyName);
        } else {
            // create constraint for column
            createUniqueConstraint(datatable, name, constraintKey);
        }
    }

    private void checkColumnRenameAndModifyUniqueConstraint(String datatable, String columnNewName, String existingConstraint) {
        if (columnNewName != null) {
            // drop existing constraint
            dropUniqueConstraint(datatable, existingConstraint);
            // create constraint with new column name
            String uniqueKeyName = datatableKeywordGenerator.generateUniqueKeyName(datatable, columnNewName);
            createUniqueConstraint(datatable, columnNewName, uniqueKeyName);
        }
    }

    private void createUniqueConstraint(String datatable, String columnName, String uniqueKeyName) {
        String sql = "ALTER TABLE " + sqlGenerator.escape(datatable) + " ADD CONSTRAINT " + sqlGenerator.escape(uniqueKeyName) + " UNIQUE ("
                + sqlGenerator.escape(columnName) + ");";
        this.jdbcTemplate.execute(sql); // NOSONAR
    }

    private void dropUniqueConstraint(String datatable, String uniqueKeyName) {
        String sql = "ALTER TABLE " + sqlGenerator.escape(datatable) + " DROP CONSTRAINT " + sqlGenerator.escape(uniqueKeyName) + ";";
        this.jdbcTemplate.execute(sql); // NOSONAR
    }

    private void createFkIndex(String datatable, String fkColumnName) {
        String indexName = datatableKeywordGenerator.generateIndexName(datatable, fkColumnName);
        createIndex(indexName, datatable, fkColumnName);
    }

    private void createIndexesForTable(String datatable, JsonArray columns) {
        for (final JsonElement column : columns) {
            createIndexForColumn(datatable, column.getAsJsonObject());
        }
    }

    private void createIndexForColumn(String datatable, JsonObject column) {
        String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        final boolean unique = column.has(API_FIELD_UNIQUE) && column.get(API_FIELD_UNIQUE).getAsBoolean();
        final boolean indexed = column.has(API_FIELD_INDEXED) && column.get(API_FIELD_INDEXED).getAsBoolean();
        if (!unique && indexed) {
            String indexName = datatableKeywordGenerator.generateIndexName(datatable, name);
            createIndex(indexName, datatable, name);
        }
    }

    private void updateIndexesForTable(String datatable, JsonArray changeColumns,
            Map<String, ResultsetColumnHeaderData> mapColumnNameDefinition) {
        for (final JsonElement column : changeColumns) {
            String name = column.getAsJsonObject().has(API_FIELD_NAME) ? column.getAsJsonObject().get(API_FIELD_NAME).getAsString() : null;
            if (!mapColumnNameDefinition.containsKey(name)) {
                throw new PlatformDataIntegrityException("error.msg.datatable.column.missing.update.parse",
                        "Column " + name + " does not exist.", name);
            }
            updateIndexForColumn(datatable, column.getAsJsonObject(),
                    mapColumnNameDefinition.get(column.getAsJsonObject().get(API_FIELD_NAME).getAsString()));
        }
    }

    private void updateIndexForColumn(String datatable, JsonObject column, ResultsetColumnHeaderData columnMetaData) {
        String name = column.has(API_FIELD_NAME) ? column.get(API_FIELD_NAME).getAsString() : null;
        String columnNewName = column.has(API_FIELD_NEWNAME) ? column.get(API_FIELD_NEWNAME).getAsString() : null;
        final boolean isAlreadyUnique = genericDataService.isExplicitlyUnique(datatable, name);
        final boolean setForUnique = column.has(API_FIELD_UNIQUE) ? column.get(API_FIELD_UNIQUE).getAsBoolean() : isAlreadyUnique;
        if (setForUnique) {
            return;
        }
        final boolean isAlreadyIndexed = genericDataService.isExplicitlyIndexed(datatable, name);
        boolean setForIndexed = column.has(API_FIELD_INDEXED) ? column.get(API_FIELD_INDEXED).getAsBoolean() : isAlreadyIndexed;

        String indexName = datatableKeywordGenerator.generateIndexName(datatable, name);
        if (isAlreadyIndexed) {
            if (!setForIndexed) {
                // drop index
                dropIndex(datatable, indexName);
            } else { // if column name changed
                checkColumnRenameAndModifyIndex(datatable, columnNewName, indexName);
            }
        } else if (setForIndexed) {
            checkColumnRenameAndCreateIndex(datatable, name, columnNewName, indexName);
        }
    }

    private void checkColumnRenameAndCreateIndex(String datatable, String columnExistingName, String columnNewName, String indexName) {
        if (columnNewName != null) {
            String newIndexName = datatableKeywordGenerator.generateIndexName(datatable, columnNewName);
            // create index with new column name
            createIndex(newIndexName, datatable, columnNewName);
        } else {
            // create index with previous name
            createIndex(indexName, datatable, columnExistingName);
        }
    }

    private void checkColumnRenameAndModifyIndex(String datatable, String columnNewName, String existingIndex) {
        if (columnNewName != null) {
            // drop index with previous name
            dropIndex(datatable, existingIndex);
            // create index with new name
            String newIndexName = datatableKeywordGenerator.generateIndexName(datatable, columnNewName);
            createIndex(newIndexName, datatable, columnNewName);
        }
    }

    private void createIndex(String indexName, String tableName, String columnName) {
        String safeIndexName = sqlGenerator.escape(indexName);
        String safeTableName = sqlGenerator.escape(tableName);
        String safeColumnName = sqlGenerator.escape(columnName);
        String sqlIndexUpdateBuilder = "CREATE INDEX %s ON %s (%s);".formatted(safeIndexName, safeTableName, safeColumnName);
        jdbcTemplate.execute(sqlIndexUpdateBuilder);
    }

    private void dropIndex(String datatable, String uniqueIndexName) {
        StringBuilder sqlIndexUpdateBuilder = new StringBuilder();
        if (databaseTypeResolver.isMySQL()) {
            sqlIndexUpdateBuilder.append("ALTER TABLE ").append(sqlGenerator.escape(datatable)).append(" ");
        }
        sqlIndexUpdateBuilder.append("DROP INDEX ").append(sqlGenerator.escape(uniqueIndexName)).append(";");
        jdbcTemplate.execute(sqlIndexUpdateBuilder.toString());
    }

    @Transactional
    @Override
    public void deleteDatatable(final String datatable) {
        try {
            this.context.authenticatedUser();
            validateDatatable(datatable);
            assertDatatableEmpty(datatable);
            deregisterDatatable(datatable);
            String[] sqlArray;
            if (this.configurationDomainService.isConstraintApproachEnabledForDatatables()) {
                final String deleteColumnCodeSql = "delete from x_table_column_code_mappings where column_alias_name like'"
                        + datatable.toLowerCase().replaceAll("\\s", "_") + "_%'";
                sqlArray = new String[2];
                sqlArray[1] = deleteColumnCodeSql;
            } else {
                sqlArray = new String[1];
            }
            final String sql = "DROP TABLE " + sqlGenerator.escape(datatable);
            sqlArray[0] = sql;
            this.jdbcTemplate.batchUpdate(sqlArray);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");
            if (realCause.getMessage().contains("Unknown table")) {
                baseDataValidator.reset().parameter(API_PARAM_DATATABLE_NAME).failWithCode("does.not.exist");
            }
            baseDataValidator.throwValidationErrors();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult createNewDatatableEntry(final String datatable, final Serializable appTableId,
            final JsonCommand command) {
        return createNewDatatableEntry(datatable, appTableId, command.json(), false);
    }

    @Transactional
    @Override
    public CommandProcessingResult createNewDatatableEntry(final String datatable, final Serializable appTableId, final String json) {
        return createNewDatatableEntry(datatable, appTableId, json, false);
    }

    @Transactional
    @Override
    public CommandProcessingResult createPPIEntry(final String datatable, final Serializable appTableId, final JsonCommand command) {
        return createNewDatatableEntry(datatable, appTableId, command.json(), true);
    }

    private CommandProcessingResult createNewDatatableEntry(final String datatable, final Serializable appTableId, final String json,
            boolean addScore) {
        final EntityTables entity = queryForApplicationEntity(datatable);
        CommandProcessingResult commandProcessingResult = checkMainResourceExistsWithinScope(entity, appTableId);

        List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(datatable);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);

        final Type typeOfMap = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> dataParams = fromJsonHelper.extractDataMap(typeOfMap, json);

        final String dateFormat = dataParams.get(API_PARAM_DATE_FORMAT);
        // fall back to dateFormat to keep backward compatibility
        final String dateTimeFormat = dataParams.getOrDefault(API_PARAM_DATETIME_FORMAT, dateFormat);
        final String localeString = dataParams.get(API_PARAM_LOCALE);
        Locale locale = localeString == null ? null : JsonParserHelper.localeFromString(localeString);

        ArrayList<String> insertColumns = new ArrayList<>(
                List.of(entity.getForeignKeyColumnNameOnDatatable(), CREATEDAT_FIELD_NAME, UPDATEDAT_FIELD_NAME));
        LocalDateTime auditDateTime = DateUtils.getAuditLocalDateTime();
        ArrayList<Object> params = new ArrayList<>(List.of(appTableId, auditDateTime, auditDateTime));
        for (Map.Entry<String, String> entry : dataParams.entrySet()) {
            if (isTechnicalParam(entry.getKey())) {
                continue;
            }
            ResultsetColumnHeaderData columnHeader = SearchUtil.validateToJdbcColumn(entry.getKey(), headersByName, false);
            if (!isUserInsertable(entity, columnHeader)) {
                continue;
            }
            insertColumns.add(columnHeader.getColumnName());
            params.add(SearchUtil.parseJdbcColumnValue(columnHeader, entry.getValue(), dateFormat, dateTimeFormat, locale, false,
                    sqlGenerator));
        }
        if (addScore) {
            List<Object> scoreIds = params.stream().filter(e -> e != null && !String.valueOf(e).isBlank()).toList();
            int scoreValue;
            if (scoreIds.isEmpty()) {
                scoreValue = 0;
            } else {
                StringBuilder scoreSql = new StringBuilder("SELECT SUM(code_score) FROM m_code_value WHERE m_code_value.");
                ArrayList<Object> scoreParams = new ArrayList<>();
                SearchUtil.buildCondition("id", BIGINT, IN, scoreIds, scoreSql, scoreParams, null, sqlGenerator);
                Integer score = jdbcTemplate.queryForObject(scoreSql.toString(), Integer.class, scoreParams.toArray(Object[]::new));
                scoreValue = score == null ? 0 : score;
            }
            insertColumns.add("score");
            params.add(scoreValue);
        }

        final String sql = sqlGenerator.buildInsert(datatable, insertColumns, headersByName);
        try {
            int updated = jdbcTemplate.update(sql, params.toArray(Object[]::new));
            if (updated != 1) {
                throw new PlatformDataIntegrityException("error.msg.invalid.insert", "Expected one inserted row.");
            }

            Serializable resourceId = appTableId;
            if (isMultirowDatatable(columnHeaders)) {
                resourceId = jdbcTemplate.queryForObject(DatabaseSpecificSQLGenerator.SELECT_CLAUSE.formatted(sqlGenerator.lastInsertId()),
                        Long.class);
            }
            return commandProcessingResult.withResource(resourceId);
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(datatable, appTableId, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException e) {
            handleDataIntegrityIssues(datatable, appTableId, ExceptionUtils.getRootCause(e.getCause()), e);
            return CommandProcessingResult.empty();
        }
    }

    private static boolean isUserInsertable(@NotNull EntityTables entity, @NotNull ResultsetColumnHeaderData columnHeader) {
        String columnName = columnHeader.getColumnName();
        return !columnHeader.getIsColumnPrimaryKey() && !CREATEDAT_FIELD_NAME.equals(columnName) && !UPDATEDAT_FIELD_NAME.equals(columnName)
                && !entity.getForeignKeyColumnNameOnDatatable().equals(columnName);
    }

    @Transactional
    @Override
    public CommandProcessingResult updateDatatableEntryOneToOne(final String datatable, final Serializable appTableId,
            final JsonCommand command) {
        return updateDatatableEntry(datatable, appTableId, null, command.json());
    }

    @Transactional
    @Override
    public CommandProcessingResult updateDatatableEntryOneToMany(final String datatable, final Serializable appTableId,
            final Long datatableId, final JsonCommand command) {
        return updateDatatableEntry(datatable, appTableId, datatableId, command.json());
    }

    @SuppressWarnings({ "WhitespaceAround" })
    @Override
    public CommandProcessingResult updateDatatableEntry(final String datatable, final Serializable appTableId, final Long datatableId,
            final String json) {
        final EntityTables entity = queryForApplicationEntity(datatable);
        CommandProcessingResult commandProcessingResult = checkMainResourceExistsWithinScope(entity, appTableId);

        final GenericResultsetData existingRows = retrieveDatatableGenericResultSet(entity, datatable, appTableId, null, datatableId);
        if (existingRows.hasNoEntries()) {
            throw new DatatableNotFoundException(datatable, appTableId);
        }
        if (existingRows.hasMoreThanOneEntry()) {
            throw new PlatformDataIntegrityException("error.msg.attempting.multiple.update",
                    "Application table: " + datatable + " Foreign key id: " + appTableId);
        }

        List<ResultsetColumnHeaderData> columnHeaders = existingRows.getColumnHeaders();
        if (isMultirowDatatable(columnHeaders) && datatableId == null) {
            throw new PlatformDataIntegrityException("error.msg.attempting.multiple.update",
                    "Application table: " + datatable + " Foreign key id: " + appTableId);
        }
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        final List<Object> existingValues = existingRows.getData().get(0).getRow();
        HashMap<ResultsetColumnHeaderData, Object> valuesByHeader = columnHeaders.stream().collect(HashMap::new,
                (map, e) -> map.put(e, existingValues.get(map.size())), (map, map2) -> {});

        final Type typeOfMap = new TypeToken<Map<String, String>>() {}.getType();
        final Map<String, String> dataParams = fromJsonHelper.extractDataMap(typeOfMap, json);

        final String dateFormat = dataParams.get(API_PARAM_DATE_FORMAT);
        // fall back to dateFormat to keep backward compatibility
        final String dateTimeFormat = dataParams.getOrDefault(API_PARAM_DATETIME_FORMAT, dateFormat);
        final String localeString = dataParams.get(API_PARAM_LOCALE);
        Locale locale = localeString == null ? null : JsonParserHelper.localeFromString(localeString);

        DatabaseType dialect = sqlGenerator.getDialect();
        ArrayList<String> updateColumns = new ArrayList<>(List.of(UPDATEDAT_FIELD_NAME));
        ArrayList<Object> params = new ArrayList<>(List.of(DateUtils.getAuditLocalDateTime()));
        final HashMap<String, Object> changes = new HashMap<>();
        for (Map.Entry<String, String> entry : dataParams.entrySet()) {
            if (isTechnicalParam(entry.getKey())) {
                continue;
            }
            ResultsetColumnHeaderData columnHeader = SearchUtil.validateToJdbcColumn(entry.getKey(), headersByName, false);
            if (!isUserUpdatable(entity, columnHeader)) {
                continue;
            }
            String columnName = columnHeader.getColumnName();
            Object existingValue = valuesByHeader.get(columnHeader);
            Object columnValue = SearchUtil.parseColumnValue(columnHeader, entry.getValue(), dateFormat, dateTimeFormat, locale, false,
                    sqlGenerator);
            if ((columnHeader.getColumnType().isDecimalType() && MathUtil.isEqualTo((BigDecimal) existingValue, (BigDecimal) columnValue))
                    || (existingValue == null ? columnValue == null : existingValue.equals(columnValue))) {
                log.debug("Ignore change on update {}:{}", datatable, columnName);
                continue;
            }
            updateColumns.add(columnName);
            params.add(columnHeader.getColumnType().toJdbcValue(dialect, columnValue, false));
            changes.put(columnName, columnValue);
        }
        Serializable primaryKey = datatableId == null ? appTableId : datatableId;
        if (!updateColumns.isEmpty()) {
            ResultsetColumnHeaderData pkColumn = SearchUtil.getFiltered(columnHeaders, ResultsetColumnHeaderData::getIsColumnPrimaryKey);
            params.add(primaryKey);
            final String sql = sqlGenerator.buildUpdate(datatable, updateColumns, headersByName) + " WHERE " + pkColumn.getColumnName()
                    + " = ?";
            int updated = jdbcTemplate.update(sql, params.toArray(Object[]::new)); // NOSONAR
            if (updated != 1) {
                throw new PlatformDataIntegrityException("error.msg.invalid.update", "Expected one updated row.");
            }
        } else {
            log.debug("No change on update {}", datatable);
        }
        String commandId = dataParams.get("commandId");
        return new CommandProcessingResultBuilder().withCommandId(commandId == null ? null : Long.valueOf(commandId)) //
                .withResource(primaryKey) //
                .withOfficeId(commandProcessingResult.getOfficeId()) //
                .withGroupId(commandProcessingResult.getGroupId()) //
                .withClientId(commandProcessingResult.getClientId()) //
                .withSavingsId(commandProcessingResult.getSavingsId()) //
                .withLoanId(commandProcessingResult.getLoanId()) //
                .withTransactionId(commandProcessingResult.getTransactionId()) //
                .with(changes).build();
    }

    private static boolean isUserUpdatable(@NotNull EntityTables entity, @NotNull ResultsetColumnHeaderData columnHeader) {
        return isUserInsertable(entity, columnHeader);
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteDatatableEntries(final String datatable, final Serializable appTableId, JsonCommand command) {
        return deleteDatatableEntries(datatable, appTableId, null, command);
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteDatatableEntry(final String datatable, final Serializable appTableId, final Long datatableId,
            JsonCommand command) {
        return deleteDatatableEntries(datatable, appTableId, datatableId, command);
    }

    private CommandProcessingResult deleteDatatableEntries(final String datatable, final Serializable appTableId, final Long datatableId,
            JsonCommand command) {
        validateDatatable(datatable);
        if (isDatatableAttachedToEntityDatatableCheck(datatable)) {
            throw new DatatableEntryRequiredException(datatable, appTableId);
        }
        final EntityTables entity = queryForApplicationEntity(datatable);
        final CommandProcessingResult commandProcessingResult = checkMainResourceExistsWithinScope(entity, appTableId);

        String whereColumn;
        Serializable whereValue;
        if (datatableId == null) {
            whereColumn = entity.getForeignKeyColumnNameOnDatatable();
            whereValue = appTableId;
        } else {
            whereColumn = TABLE_FIELD_ID;
            whereValue = datatableId;
        }
        String sql = "DELETE FROM " + sqlGenerator.escape(datatable) + " WHERE " + sqlGenerator.escape(whereColumn) + " = "
                + sqlGenerator.formatValue(entity.getRefColumnType(), whereValue);

        this.jdbcTemplate.update(sql); // NOSONAR
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResource(whereValue) //
                .withOfficeId(commandProcessingResult.getOfficeId()) //
                .withGroupId(commandProcessingResult.getGroupId()) //
                .withClientId(commandProcessingResult.getClientId()) //
                .withSavingsId(commandProcessingResult.getSavingsId()) //
                .withLoanId(commandProcessingResult.getLoanId()) //
                .withTransactionId(commandProcessingResult.getTransactionId()) //
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GenericResultsetData retrieveDatatableGenericResultSet(final String datatable, final Serializable appTableId, final String order,
            final Long id) {
        final EntityTables entity = queryForApplicationEntity(datatable);
        checkMainResourceExistsWithinScope(entity, appTableId);
        return retrieveDatatableGenericResultSet(entity, datatable, appTableId, order, id);
    }

    private GenericResultsetData retrieveDatatableGenericResultSet(final EntityTables entity, final String datatable,
            final Serializable appTableId, final String order, final Long id) {
        final List<ResultsetColumnHeaderData> columnHeaders = this.genericDataService.fillResultsetColumnHeaders(datatable);
        final boolean multiRow = isMultirowDatatable(columnHeaders);

        String whereClause = entity.getForeignKeyColumnNameOnDatatable() + " = "
                + sqlGenerator.formatValue(entity.getRefColumnType(), appTableId);
        SQLInjectionValidator.validateSQLInput(whereClause);
        String sql = "select * from " + sqlGenerator.escape(datatable) + " where " + whereClause;

        // id only used for reading a specific entry that belongs to appTableId (in a one to many datatable)
        if (multiRow && id != null) {
            sql = sql + " and " + TABLE_FIELD_ID + " = " + id;
        }
        if (StringUtils.isNotBlank(order)) {
            this.columnValidator.validateSqlInjection(sql, order);
            sql = sql + " order by " + order;
        }

        final List<ResultsetRowData> result = genericDataService.fillResultsetRowData(sql, columnHeaders);
        return new GenericResultsetData(columnHeaders, result);
    }

    private CommandProcessingResult checkMainResourceExistsWithinScope(@NotNull EntityTables entity, final Serializable appTableId) {
        final String sql = dataScopedSQL(entity, appTableId);
        log.debug("data scoped sql: {}", sql);
        final SqlRowSet rs = this.jdbcTemplate.queryForRowSet(sql);

        if (!rs.next()) {
            throw new DatatableNotFoundException(entity, appTableId);
        }
        final Long officeId = (Long) rs.getObject("officeId");
        final Long groupId = (Long) rs.getObject("groupId");
        final Long clientId = (Long) rs.getObject("clientId");
        final Long savingsId = (Long) rs.getObject("savingsId");
        final Long loanId = (Long) rs.getObject("loanId");
        final Object transactionId = rs.getObject("transactionId");
        final Object entityId = rs.getObject("entityId");

        if (rs.next()) {
            throw new DatatableSystemErrorException("System Error: More than one row returned from data scoping query");
        }

        return new CommandProcessingResultBuilder() //
                .withOfficeId(officeId) //
                .withGroupId(groupId) //
                .withClientId(clientId) //
                .withSavingsId(savingsId) //
                .withLoanId(loanId) //
                .withTransactionId(transactionId == null ? null : String.valueOf(transactionId)) //
                .withResource(entityId) //
                .build();
    }

    private String dataScopedSQL(@NotNull EntityTables entity, final Serializable appTableId) {
        // unfortunately have to, one way or another, be able to restrict data to the users office hierarchy. Here, a
        // few key tables are done. But if additional fields are needed on other tables the same pattern applies
        String sqlId = sqlGenerator.formatValue(entity.getRefColumnType(), appTableId);
        final AppUser currentUser = this.context.authenticatedUser();
        String officeHierarchy = currentUser.getOffice().getHierarchy();
        // m_loan and m_savings_account are connected to an m_office through either an m_client or an m_group If both it
        // means it relates to an m_client that is in a group (still an m_client account)
        return switch (entity) {
            case LOAN -> "select distinct x.* from ( "
                    + "(select o.id as officeId, l.group_id as groupId, l.client_id as clientId, null as savingsId, l.id as loanId, null as transactionId, null as entityId from m_loan l "
                    + getClientOfficeJoinCondition(officeHierarchy, "l") + " where l.id = " + sqlId + ")" + " union all "
                    + "(select o.id as officeId, l.group_id as groupId, l.client_id as clientId, null as savingsId, l.id as loanId, null as transactionId, null as entityId from m_loan l "
                    + getGroupOfficeJoinCondition(officeHierarchy, "l") + " where l.id = " + sqlId + ")" + " ) as x";
            case SAVINGS -> "select distinct x.* from ( "
                    + "(select o.id as officeId, s.group_id as groupId, s.client_id as clientId, s.id as savingsId, null as loanId, null as transactionId, null as entityId "
                    + "from m_savings_account s " + getClientOfficeJoinCondition(officeHierarchy, "s") + " where s.id = " + sqlId + ")"
                    + " union all "
                    + "(select o.id as officeId, s.group_id as groupId, s.client_id as clientId, s.id as savingsId, null as loanId, null as transactionId, null as entityId "
                    + "from m_savings_account s " + getGroupOfficeJoinCondition(officeHierarchy, "s") + " where s.id = " + sqlId + ")"
                    + " ) as x";
            case SAVINGS_TRANSACTION -> "select distinct x.* from ( "
                    + "(select o.id as officeId, s.group_id as groupId, s.client_id as clientId, s.id as savingsId, null as loanId, t.id as transactionId, null as entityId "
                    + "from m_savings_account_transaction t join m_savings_account s on t.savings_account_id = s.id "
                    + getClientOfficeJoinCondition(officeHierarchy, "s") + " where t.id = " + sqlId + ")" + " union all "
                    + "(select o.id as officeId, s.group_id as groupId, s.client_id as clientId, s.id as savingsId, null as loanId, t.id as transactionId, null as entityId "
                    + "from m_savings_account_transaction t join m_savings_account s on t.savings_account_id = s.id "
                    + getGroupOfficeJoinCondition(officeHierarchy, "s") + " where t.id = " + sqlId + ")" + " ) as x";
            case CLIENT ->
                "select o.id as officeId, null as groupId, c.id as clientId, null as savingsId, null as loanId, null as transactionId, null as entityId from m_client c "
                        + getOfficeJoinCondition(officeHierarchy, "c") + " where c.id = " + sqlId;
            case GROUP, CENTER ->
                "select o.id as officeId, g.id as groupId, null as clientId, null as savingsId, null as loanId, null as transactionId, null as entityId from m_group g "
                        + getOfficeJoinCondition(officeHierarchy, "g") + " where g.id = " + sqlId;
            case OFFICE ->
                "select o.id as officeId, null as groupId, null as clientId, null as savingsId, null as loanId, null as transactionId, null as entityId from m_office o "
                        + "where o.hierarchy like '" + officeHierarchy + "%'" + " and o.id = " + sqlId;
            case CURRENT ->
                "select o.id as officeId, null as groupId, app.client_id as clientId, null as savingsId, null as loanId, null as transactionId, app.id as entityId "
                        + "from " + entity.getName() + " app " + getClientOfficeJoinCondition(officeHierarchy, "app") + " where app.id = "
                        + sqlId;
            case CURRENT_TRANSACTION ->
                "select o.id as officeId, null as groupId, app.client_id as clientId, null as savingsId, null as loanId, app.id as transactionId, acc.id as entityId "
                        + "from " + entity.getName() + " app join m_current_account acc on app.account_id = acc.id "
                        + getClientOfficeJoinCondition(officeHierarchy, "acc") + " where app.id = " + sqlId;
            case LOAN_PRODUCT, SAVINGS_PRODUCT, SHARE_PRODUCT, CURRENT_PRODUCT ->
                "select null as officeId, null as groupId, null as clientId, null as savingsId, null as loanId, null as transactionId, p.id as entityId from "
                        + entity.getName() + " as p WHERE p.id = " + sqlId;
            default -> throw new PlatformDataIntegrityException("error.msg.invalid.dataScopeCriteria",
                    "Application Table: " + entity.getName() + " not catered for in data Scoping");
        };
    }

    private String getClientOfficeJoinCondition(String officeHierarchy, String appTableAlias) {
        return " join m_client c on c.id = " + appTableAlias + ".client_id " + getOfficeJoinCondition(officeHierarchy, "c");
    }

    private String getGroupOfficeJoinCondition(String officeHierarchy, String appTableAlias) {
        return " join m_group g on g.id = " + appTableAlias + ".group_id " + getOfficeJoinCondition(officeHierarchy, "g");
    }

    private String getOfficeJoinCondition(String officeHierarchy, String joinTableAlias) {
        return " join m_office o on o.id = " + joinTableAlias + ".office_id and o.hierarchy like '" + officeHierarchy + "%' ";
    }

    @NotNull
    private EntityTables queryForApplicationEntity(final String datatable) {
        SQLInjectionValidator.validateSQLInput(datatable);
        final String sql = "SELECT application_table_name FROM x_registered_table where registered_table_name = ?";
        final SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, datatable); // NOSONAR

        String applicationTableName;
        if (rowSet.next()) {
            applicationTableName = rowSet.getString("application_table_name");
        } else {
            throw new DatatableNotFoundException(datatable);
        }
        return resolveEntity(applicationTableName);
    }

    @Override
    public Long countDatatableEntries(final String datatable, final Serializable appTableId, EntityTables entity) {
        String sqlId = sqlGenerator.formatValue(entity.getRefColumnType(), appTableId);
        String foreignKeyColumn = entity.getForeignKeyColumnNameOnDatatable();
        final String sqlString = "SELECT COUNT(" + sqlGenerator.escape(foreignKeyColumn) + ") FROM " + sqlGenerator.escape(datatable)
                + " WHERE " + sqlGenerator.escape(foreignKeyColumn) + " = " + sqlId;
        return this.jdbcTemplate.queryForObject(sqlString, Long.class); // NOSONAR
    }

    // --- Validation ---

    public boolean isDatatableAttachedToEntityDatatableCheck(final String datatable) {
        String sql = "SELECT COUNT(edc.x_registered_table_name) FROM x_registered_table xrt"
                + " JOIN m_entity_datatable_check edc ON edc.x_registered_table_name = xrt.registered_table_name"
                + " WHERE edc.x_registered_table_name = '" + datatable + "'";
        final Long count = this.jdbcTemplate.queryForObject(sql, Long.class); // NOSONAR
        return count != null && count > 0;
    }

    private EntityTables resolveEntity(final String entityName) {
        EntityTables entity = EntityTables.fromEntityName(entityName);
        if (entity == null) {
            throw new PlatformDataIntegrityException("error.msg.invalid.application.table", "Invalid Datatable entity: " + entityName,
                    API_FIELD_NAME, entityName);
        }
        return entity;
    }

    private void validateDatatable(final String datatable) {
        if (datatable == null || datatable.isEmpty()) {
            throw new PlatformDataIntegrityException("error.msg.datatables.datatable.null.name", "Data table name must not be blank.");
        } else if (!datatable.matches(DatatableCommandFromApiJsonDeserializer.DATATABLE_NAME_REGEX_PATTERN)) {
            throw new PlatformDataIntegrityException("error.msg.datatables.datatable.invalid.name.regex", "Invalid data table name.",
                    datatable);
        }
        SQLInjectionValidator.validateSQLInput(datatable);
    }

    private String validateDatatableRegistered(String datatable) {
        validateDatatable(datatable);
        if (!isRegisteredDatatable(datatable)) {
            throw new DatatableNotFoundException(datatable);
        }
        return datatable;
    }

    private boolean isRegisteredDatatable(final String datatable) {
        final String sql = "SELECT COUNT(application_table_name) FROM " + TABLE_REGISTERED_TABLE + " WHERE registered_table_name = ?";
        final Integer count = jdbcTemplate.queryForObject(sql, Integer.class, datatable);
        return count != null && count > 0;
    }

    private void validateDatatableExists(final String datatable) {
        final String sql = "select (CASE WHEN exists (select 1 from information_schema.tables where table_schema = "
                + sqlGenerator.currentSchema() + " and table_name = ?) THEN 'true' ELSE 'false' END)";
        final boolean datatableExists = Boolean.parseBoolean(this.jdbcTemplate.queryForObject(sql, String.class, datatable)); // NOSONAR
        if (!datatableExists) {
            throw new PlatformDataIntegrityException("error.msg.invalid.datatable", "Invalid Data Table: " + datatable, API_FIELD_NAME,
                    datatable);
        }
    }

    private void assertDatatableEmpty(final String datatable) {
        final int rowCount = getDatatableRowCount(datatable);
        if (rowCount != 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.non.empty.datatable.cannot.be.deleted",
                    "Non-empty datatable cannot be deleted.");
        }
    }

    // --- DbUtils ---

    @NotNull
    private String mapApiTypeToDbType(String apiType, Integer length) {
        if (StringUtils.isEmpty(apiType)) {
            return "";
        }
        JdbcJavaType jdbcType = DatatableCommandFromApiJsonDeserializer.mapApiTypeToJdbcType(apiType);
        DatabaseType dialect = databaseTypeResolver.databaseType();
        if (jdbcType.isDecimalType()) {
            return jdbcType.formatSql(dialect, 19, 6); // TODO: parameter length is not used
        } else if (apiType.equalsIgnoreCase(API_FIELD_TYPE_DROPDOWN)) {
            return jdbcType.formatSql(dialect, 11); // TODO: parameter length is not used
        }
        return jdbcType.formatSql(dialect, length);
    }

    private int getDatatableRowCount(final String datatable) {
        final String sql = "select count(*) from " + sqlGenerator.escape(datatable);
        Integer count = this.jdbcTemplate.queryForObject(sql, Integer.class); // NOSONAR
        return count == null ? 0 : count;
    }

    // --- Utils ---

    private static boolean isTechnicalParam(String param) {
        return API_PARAM_DATE_FORMAT.equals(param) || API_PARAM_DATETIME_FORMAT.equals(param) || API_PARAM_LOCALE.equals(param)
                || TABLE_FIELD_ID.equals(param);
    }

    private boolean isMultirowDatatable(final List<ResultsetColumnHeaderData> columnHeaders) {
        return SearchUtil.findFiltered(columnHeaders, e -> e.isNamed(TABLE_FIELD_ID)) != null;
    }

    private String datatableColumnNameToCodeValueName(final String columnName, final String code) {
        return code + "_cd_" + columnName;
    }

    private void handleDataIntegrityIssues(String datatable, Serializable appTableId, final Throwable realCause, final Exception e) {
        String msgCode = "error.msg.datatable";
        String msg = "Unknown data integrity issue with datatable `" + datatable + "`";
        String param = null;
        Object[] msgArgs;
        final Throwable cause = e.getCause();
        if ((realCause != null && realCause.getMessage().contains("Duplicate entry"))
                || (cause != null && cause.getMessage().contains("Duplicate entry"))) {
            msgCode += ".entry.duplicate";
            param = API_PARAM_DATATABLE_NAME;
            if (appTableId == null) {
                msg = "Datatable `" + datatable + "` is already registered against an application table.";
                msgArgs = new Object[] { datatable, e };
            } else {
                msg = "An entry already exists for datatable `" + datatable + "` and application table with identifier `" + appTableId
                        + "`.";
                msgArgs = new Object[] { datatable, appTableId, e };
            }
        } else if ((realCause != null && realCause.getMessage().contains("doesn't have a default value"))
                || (cause != null && cause.getMessage().contains("doesn't have a default value"))) {
            msgCode += ".no.value.provided.for.required.fields";
            msg = "No values provided for the datatable `" + datatable + "` and application table with identifier `" + appTableId + "`.";
            param = API_PARAM_DATATABLE_NAME;
            msgArgs = new Object[] { datatable, appTableId, e };
        } else {
            msgCode += ".unknown.data.integrity.issue";
            msgArgs = new Object[] { datatable, e };
        }
        log.error("Error occured.", e);
        throw ErrorHandler.getMappable(e, msgCode, msg, param, msgArgs);
    }
}
