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
package org.apache.fineract.infrastructure.dataqueries.data;

import com.google.common.collect.ImmutableList;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.fineract.infrastructure.core.service.database.JdbcJavaType;

public enum EntityTables {

    CLIENT("m_client", "client_id", "id", StatusEnum.CREATE, StatusEnum.ACTIVATE, StatusEnum.CLOSE), //
    GROUP("m_group", "group_id", "id", StatusEnum.CREATE, StatusEnum.ACTIVATE, StatusEnum.CLOSE), //
    CENTER("m_center", "m_group", "center_id", "id"), //
    OFFICE("m_office", "office_id", "id"), //
    LOAN_PRODUCT("m_product_loan", "product_loan_id", "id"), //
    LOAN("m_loan", "loan_id", "id", StatusEnum.CREATE, StatusEnum.APPROVE, StatusEnum.DISBURSE, StatusEnum.WITHDRAWN, StatusEnum.REJECTED,
            StatusEnum.WRITE_OFF), //
    SAVINGS_PRODUCT("m_savings_product", "savings_product_id", "id"), //
    SAVINGS("m_savings_account", "savings_account_id", "id", StatusEnum.CREATE, StatusEnum.APPROVE, StatusEnum.ACTIVATE,
            StatusEnum.WITHDRAWN, StatusEnum.REJECTED, StatusEnum.CLOSE), //
    SAVINGS_TRANSACTION("m_savings_account_transaction", "savings_transaction_id", "id"), //
    SHARE_PRODUCT("m_share_product", "share_product_id", "id"), //
    CURRENT_PRODUCT("m_current_product", "current_product_id", "id", JdbcJavaType.VARCHAR), //
    CURRENT("m_current_account", "current_account_id", "id", JdbcJavaType.VARCHAR), //
    CURRENT_TRANSACTION("m_current_transaction", "current_transaction_id", "id", JdbcJavaType.VARCHAR), //
    ;

    public static final EntityTables[] VALUES = values();

    private static final List<String> ENTITY_NAMES = Arrays.stream(VALUES).map(EntityTables::getName).toList();

    private static final Map<String, EntityTables> BY_ENTITY_NAME = Arrays.stream(VALUES)
            .collect(Collectors.toMap(EntityTables::getName, e -> e));

    @NotNull
    private final String name;
    @NotNull
    private final String apptableName;

    @NotNull
    private final String foreignKeyColumnNameOnDatatable;
    @NotNull
    private final String refColumn; // referenced column name on apptable
    @NotNull
    private final JdbcJavaType refColumnType; // referenced column type on apptable

    private final ImmutableList<StatusEnum> checkStatuses;

    EntityTables(@NotNull String name, @NotNull String apptableName, @NotNull String foreignKeyColumnNameOnDatatable,
            @NotNull String refColumn, @NotNull JdbcJavaType refColumnType, StatusEnum... statuses) {
        this.name = name;
        this.apptableName = apptableName;
        this.foreignKeyColumnNameOnDatatable = foreignKeyColumnNameOnDatatable;
        this.refColumn = refColumn;
        this.refColumnType = refColumnType;
        this.checkStatuses = statuses == null ? ImmutableList.of() : ImmutableList.copyOf(statuses);
    }

    EntityTables(@NotNull String name, @NotNull String foreignKeyColumnNameOnDatatable, @NotNull String refColumn,
            @NotNull JdbcJavaType refColumnType, StatusEnum... statuses) {
        this(name, name, foreignKeyColumnNameOnDatatable, refColumn, refColumnType, statuses);
    }

    EntityTables(@NotNull String name, @NotNull String apptableName, @NotNull String foreignKeyColumnNameOnDatatable,
            @NotNull String refColumn, StatusEnum... statuses) {
        this(name, apptableName, foreignKeyColumnNameOnDatatable, refColumn, JdbcJavaType.BIGINT, statuses);
    }

    EntityTables(@NotNull String name, @NotNull String foreignKeyColumnNameOnDatatable, @NotNull String refColumn, StatusEnum... statuses) {
        this(name, name, foreignKeyColumnNameOnDatatable, refColumn, statuses);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getApptableName() {
        return apptableName;
    }

    @NotNull
    public String getForeignKeyColumnNameOnDatatable() {
        return this.foreignKeyColumnNameOnDatatable;
    }

    @NotNull
    public String getRefColumn() {
        return refColumn;
    }

    @NotNull
    public JdbcJavaType getRefColumnType() {
        return refColumnType;
    }

    public List<StatusEnum> getCheckStatuses() {
        return checkStatuses;
    }

    public boolean hasCheck() {
        return checkStatuses != null && !checkStatuses.isEmpty();
    }

    public static List<String> getEntityNames() {
        return ENTITY_NAMES;
    }

    public static EntityTables fromEntityName(String name) {
        return name == null ? null : BY_ENTITY_NAME.get(name.toLowerCase());
    }

    public static String getForeignKeyColumnNameOnDatatable(String name) {
        EntityTables entityTable = fromEntityName(name);
        return entityTable == null ? null : entityTable.getForeignKeyColumnNameOnDatatable();
    }

    @NotNull
    public static List<StatusEnum> getCheckStatuses(String name) {
        EntityTables entityTable = fromEntityName(name);
        return entityTable == null ? List.of() : entityTable.getCheckStatuses();
    }

    @NotNull
    public static List<Integer> getCheckStatusCodes(String name) {
        return getCheckStatuses(name).stream().map(StatusEnum::getValue).toList();
    }

    @NotNull
    public static List<EntityTables> getFiltered(Predicate<EntityTables> filter) {
        return Arrays.stream(VALUES).filter(filter).toList();
    }
}
