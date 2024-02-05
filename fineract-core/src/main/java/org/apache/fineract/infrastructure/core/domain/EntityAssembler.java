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
package org.apache.fineract.infrastructure.core.domain;

import static org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants.DATATABLE_ENTRIES_PARAM;
import static org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants.DATATABLE_ID_PARAM;
import static org.apache.fineract.infrastructure.dataqueries.api.DatatableApiConstants.DATATABLE_NAME_PARAM;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.logging.log4j.util.Strings;

public interface EntityAssembler<T extends AbstractPersistableCustom> {

    T assemble(JsonCommand command);

    Map<String, Object> update(T account, JsonCommand command);

    default Map<String, Object> persistDatatableEntries(@NotNull EntityTables entity, Serializable entityId, @NotNull JsonArray datatables,
            boolean update, @NotNull ReadWriteNonCoreDataService service) {
        final HashMap<String, Object> changes = new HashMap<>();
        for (JsonElement datatable : datatables) {
            JsonObject tableObject = datatable.getAsJsonObject();
            final String tableName = tableObject.get(DATATABLE_NAME_PARAM).getAsString();
            JsonArray entries = tableObject.getAsJsonArray(DATATABLE_ENTRIES_PARAM);

            if (Strings.isBlank(tableName) || entries == null || entries.isEmpty()) {
                throw new PlatformApiDataValidationException("datatable.table.and.data.parameters.mandatory",
                        "Table and data parameters must be present for each datatable items", null);
            }
            // TODO CURRENT! check datatable permission
            // TODO CURRENT! check maker-checker config for permission
            HashMap<String, Object> tableChanges = new HashMap<>();
            for (JsonElement entry : entries) {
                if (update) {
                    JsonObject entryObject = entry.getAsJsonObject();
                    JsonElement idElement = entryObject.get(DATATABLE_ID_PARAM);
                    Long tableId = idElement == null ? null : idElement.getAsLong();
                    CommandProcessingResult result = service.updateDatatableEntry(tableName, entityId, tableId, entry.toString());
                    Map<String, Object> entryChanges = result.getChanges();
                    if (entryChanges != null && !entryChanges.isEmpty()) {
                        if (tableId == null) {
                            tableChanges.putAll(entryChanges);
                        } else {
                            tableChanges.put(tableId.toString(), entryChanges);
                        }
                    }
                } else {
                    service.createNewDatatableEntry(tableName, entityId, entry.toString());
                }
            }
            if (!tableChanges.isEmpty()) {
                changes.put(tableName, tableChanges);
            }
        }
        return changes;
    }
}
