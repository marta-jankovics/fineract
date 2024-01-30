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
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountIdType;
import org.apache.fineract.currentaccount.service.IdTypeResolver;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;

@Getter
public class CurrentAccountIdTypeResolver {

    CurrentAccountIdType currentType;
    InteropIdentifierType interopType;

    protected CurrentAccountIdTypeResolver(CurrentAccountIdType currentType, InteropIdentifierType interopType) {
        this.currentType = currentType;
        this.interopType = interopType;
    }

    @NotNull
    public static CurrentAccountIdTypeResolver resolveDefault() {
        return new CurrentAccountIdTypeResolver(CurrentAccountIdType.ID, null);
    }

    @NotNull
    public static CurrentAccountIdTypeResolver resolve(String idType) {
        if (idType == null) {
            return resolveDefault();
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
        return new CurrentAccountIdTypeResolver(currentType, interopType);
    }

    public boolean isSecondaryIdentifier() {
        return interopType != null;
    }
}
