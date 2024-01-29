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
import java.util.List;
import org.apache.fineract.infrastructure.core.service.PagedLocalRequest;
import org.apache.fineract.infrastructure.dataqueries.data.AdvancedQueryRequest;
import org.apache.fineract.infrastructure.dataqueries.data.ColumnFilterData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.springframework.data.domain.Page;

public interface AdvancedQueryService {

    Page<JsonObject> query(@NotNull EntityTables entityTables, @NotNull PagedLocalRequest<AdvancedQueryRequest> pagedRequest,
            List<ColumnFilterData> addFilters);
}
