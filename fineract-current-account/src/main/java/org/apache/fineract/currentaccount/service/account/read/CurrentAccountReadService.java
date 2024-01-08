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
package org.apache.fineract.currentaccount.service.account.read;

import java.util.UUID;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrentAccountReadService {

    Page<CurrentAccountResponseData> retrieveAll(Pageable pageable);

    CurrentAccountResponseData retrieveById(UUID accountId);

    CurrentAccountTemplateResponseData retrieveTemplate();

    CurrentAccountResponseData retrieveByExternalId(ExternalId externalId);

    UUID retrieveAccountIdByExternalId(ExternalId externalId);
}
