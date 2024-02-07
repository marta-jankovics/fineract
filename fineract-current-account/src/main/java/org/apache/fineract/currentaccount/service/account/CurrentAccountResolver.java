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
package org.apache.fineract.currentaccount.service.account;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.service.common.IdTypeResolver;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;

@Getter
@RequiredArgsConstructor
public class CurrentAccountResolver {

    private final CurrentAccountIdType idType;
    private final InteropIdentifierType interopIdType;
    private final String identifier; // TODO CURRENT! remove the ids
    private final String subIdentifier;

    @NotNull
    public static CurrentAccountResolver resolveDefault(String identifier) {
        return new CurrentAccountResolver(CurrentAccountIdType.ID, null, identifier, null);
    }

    @NotNull
    public static CurrentAccountResolver resolve(String idType, String identifier, String subIdentifier) {
        if (idType == null) {
            return resolveDefault(identifier);
        }
        idType = IdTypeResolver.formatIdType(idType);
        CurrentAccountIdType currentType = CurrentAccountIdType.resolveName(idType);
        InteropIdentifierType interopType = null;
        if (currentType == null) {
            interopType = InteropIdentifierType.resolveName(idType);
            if (interopType == null) {
                throw IdTypeResolver.resolveFailed(idType, null);
            }
        }
        return new CurrentAccountResolver(currentType, interopType, identifier, subIdentifier);
    }

    public boolean isSecondaryIdentifier() {
        return interopIdType != null;
    }
}
