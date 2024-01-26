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

import java.util.List;
import org.apache.fineract.currentaccount.data.account.CurrentAccountIdentifiersResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountResponseData;
import org.apache.fineract.currentaccount.data.account.CurrentAccountTemplateResponseData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface CurrentAccountReadService {

    Page<CurrentAccountResponseData> retrieveAll(Pageable pageable);

    CurrentAccountResponseData retrieveById(String accountId);

    CurrentAccountTemplateResponseData retrieveTemplate();

    List<CurrentAccountResponseData> retrieveAllByClientId(Long clientId, Sort sort);

    CurrentAccountResponseData retrieveByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier);

    String retrieveIdByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier);

    CurrentAccountIdentifiersResponseData retrieveIdentifiersById(String accountId);

    CurrentAccountIdentifiersResponseData retrieveIdentifiersByIdTypeAndIdentifier(String idType, String identifier, String subIdentifier);
}
