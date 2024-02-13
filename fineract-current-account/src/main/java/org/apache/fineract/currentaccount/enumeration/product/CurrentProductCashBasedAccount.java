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
package org.apache.fineract.currentaccount.enumeration.product;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.infrastructure.core.data.GLStringEnumOptionData;

@Getter
public enum CurrentProductCashBasedAccount {

    REFERENCE(1, GLAccountType.ASSET, "referenceAccountId"), //
    CONTROL(2, GLAccountType.LIABILITY, "controlAccountId"), //
    INCOME_FROM_FEES(3, GLAccountType.INCOME, "incomeFromFeeAccountId"), //
    INCOME_FROM_PENALTIES(4, GLAccountType.INCOME, "incomeFromPenaltyAccountId"), //
    TRANSFERS_SUSPENSE(5, GLAccountType.LIABILITY, "transfersInSuspenseAccountId"), //
    OVERDRAFT_CONTROL(6, GLAccountType.ASSET, "overdraftControlAccountId"), //
    LOSSES_WRITTEN_OFF(7, GLAccountType.EXPENSE, "writeOffAccountId"); //

    private static final Map<Integer, CurrentProductCashBasedAccount> intToEnumMap = new HashMap<>();

    static {
        for (final CurrentProductCashBasedAccount type : CurrentProductCashBasedAccount.values()) {
            intToEnumMap.put(type.getId(), type);
        }
    }

    private final Integer id;
    private final GLAccountType type;
    private final String variableName;

    CurrentProductCashBasedAccount(final Integer id, final GLAccountType type, final String variableName) {
        this.id = id;
        this.type = type;
        this.variableName = variableName;
    }

    public static CurrentProductCashBasedAccount fromInt(final int i) {
        return intToEnumMap.get(i);
    }

    public GLStringEnumOptionData toGLStringEnumOptionData() {
        return new GLStringEnumOptionData(name(), getVariableName(), getType().name(), null);
    }
}
