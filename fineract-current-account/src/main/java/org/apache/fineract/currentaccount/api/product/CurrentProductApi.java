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
package org.apache.fineract.currentaccount.api.product;

import org.apache.fineract.currentaccount.data.product.CurrentProductResponseData;
import org.apache.fineract.currentaccount.data.product.CurrentProductTemplateResponseData;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrentProductApi {

    Page<CurrentProductResponseData> retrieveAll(Pageable pageable);

    CurrentProductResponseData retrieveOne(String productId);

    CurrentProductResponseData retrieveOne(String idType, String id);

    CurrentProductTemplateResponseData retrieveTemplate();

    CommandProcessingResult create(String requestJson);

    CommandProcessingResult update(String productId, String requestJson);

    CommandProcessingResult update(String idType, String id, String requestJson);

    CommandProcessingResult delete(String productId);

    CommandProcessingResult delete(String idType, String id);
}
