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
package org.apache.fineract.currentaccount.service.transaction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionIdType;
import org.apache.fineract.currentaccount.service.common.IdTypeResolver;

@Getter
@RequiredArgsConstructor
public class CurrentTransactionResolver {

    private final CurrentTransactionIdType idType;
    private final String identifier;

    @NotNull
    public static CurrentTransactionResolver resolveDefault(String identifier) {
        return new CurrentTransactionResolver(CurrentTransactionIdType.ID, identifier);
    }

    @NotNull
    public static CurrentTransactionResolver resolve(String idType, String identifier) {
        if (idType == null) {
            return resolveDefault(identifier);
        }
        idType = IdTypeResolver.formatIdType(idType);
        CurrentTransactionIdType currentType = CurrentTransactionIdType.resolveName(idType);
        if (currentType == null) {
            throw IdTypeResolver.resolveFailed(idType, null);
        }
        return new CurrentTransactionResolver(currentType, identifier);
    }
}
