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

@Getter
public enum CurrentProductCashAccounts {

    REFERENCE(1, GLAccountType.ASSET), //
    CONTROL(2, GLAccountType.LIABILITY), //
    INCOME_FROM_FEES(3, GLAccountType.INCOME), //
    INCOME_FROM_PENALTIES(4, GLAccountType.INCOME), //
    TRANSFERS_SUSPENSE(5, GLAccountType.LIABILITY), //
    OVERDRAFT_CONTROL(6, GLAccountType.ASSET), //
    LOSSES_WRITTEN_OFF(7, GLAccountType.EXPENSE); //

    private static final Map<Integer, CurrentProductCashAccounts> intToEnumMap = new HashMap<>();

    static {
        for (final CurrentProductCashAccounts type : CurrentProductCashAccounts.values()) {
            intToEnumMap.put(type.getId(), type);
        }
    }

    private final Integer id;
    private final GLAccountType type;

    CurrentProductCashAccounts(final Integer id, final GLAccountType type) {
        this.id = id;
        this.type = type;
    }

    public static CurrentProductCashAccounts fromInt(final int i) {
        return intToEnumMap.get(i);
    }
}
