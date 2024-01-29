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
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryData;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReadWriteNonCoreDataService {

    List<DatatableData> retrieveDatatables(String appTable);

    DatatableData retrieveDatatable(String datatable);

    @PreAuthorize(value = "hasAnyAuthority('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(JsonCommand command);

    @PreAuthorize(value = "hasAnyAuthority('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(String datatable, EntityTables entity, String entitySubType);

    @PreAuthorize(value = "hasAnyAuthority('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(JsonCommand command, String permissionTable);

    @PreAuthorize(value = "hasAnyAuthority('ALL_FUNCTIONS', 'DEREGISTER_DATATABLE')")
    void deregisterDatatable(String datatable);

    GenericResultsetData retrieveDatatableGenericResultSet(String datatable, Serializable appTableId, String order, Long datatableId);

    CommandProcessingResult createDatatable(JsonCommand command);

    void updateDatatable(String datatable, JsonCommand command);

    void deleteDatatable(String datatable);

    CommandProcessingResult createNewDatatableEntry(String datatable, Serializable appTableId, JsonCommand command);

    CommandProcessingResult createNewDatatableEntry(String datatable, Serializable appTableId, String json);

    CommandProcessingResult createPPIEntry(String datatable, Serializable appTableId, JsonCommand command);

    CommandProcessingResult updateDatatableEntryOneToOne(String datatable, Serializable appTableId, JsonCommand command);

    CommandProcessingResult updateDatatableEntryOneToMany(String datatable, Serializable appTableId, Long datatableId, JsonCommand command);

    CommandProcessingResult deleteDatatableEntries(String datatable, Serializable appTableId, JsonCommand command);

    CommandProcessingResult deleteDatatableEntry(String datatable, Serializable appTableId, Long datatableId, JsonCommand command);

    String parseTableName(String url);

    String parseDatatableName(String url);

    Long countDatatableEntries(String datatable, Serializable appTableId, EntityTables entity);

    List<JsonObject> queryDataTable(@NotNull String datatable, @NotNull String columnName, String columnValue,
            @NotNull String resultColumns);

    Page<JsonObject> queryDataTableAdvanced(@NotNull String datatable, @NotNull PagedLocalRequest<AdvancedQueryData> pagedRequest);

    boolean buildDataQueryEmbedded(@NotNull EntityTables entityTable, @NotNull String datatable, @NotNull AdvancedQueryData request,
            @NotNull List<String> selectColumns, @NotNull StringBuilder select, @NotNull StringBuilder from, @NotNull StringBuilder where,
            @NotNull List<Object> params, String mainAlias, String alias, String dateFormat, String dateTimeFormat, Locale locale);
}
