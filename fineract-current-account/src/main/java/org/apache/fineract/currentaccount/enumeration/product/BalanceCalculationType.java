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

import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@Getter
public enum BalanceCalculationType {

    LAZY(1, "Lazy balance calculation for debit/credit transactions"), //
    STRICT_DEBIT(2, "Strict balance calculation for debit transactions"), //
    STRICT(3, "Strict balance calculation for debit/credit transactions"); //

    private final long id;
    private final String value;

    BalanceCalculationType(long id, String value) {
        this.id = id;
        this.value = value;
    }

    public EnumOptionData toEnumOptionData() {
        return new EnumOptionData(getId(), name(), getValue());
    }
}
