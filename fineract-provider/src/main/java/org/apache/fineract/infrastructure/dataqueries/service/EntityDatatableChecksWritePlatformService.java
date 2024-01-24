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

import com.google.gson.JsonArray;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;

public interface EntityDatatableChecksWritePlatformService {

    CommandProcessingResult createCheck(JsonCommand command);

    CommandProcessingResult deleteCheck(Long entityDatatableCheckId);

    void runTheCheck(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId, String entitySubtype);

    void runTheCheckForProduct(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId,
            @NotNull Long productId);

    boolean saveDatatables(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId, Long productId,
            JsonArray elements);

}
