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
package org.apache.fineract.currentaccount.enumeration.transaction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * An enumeration of different transactions that can occur on a
 * {@link org.apache.fineract.currentaccount.domain.account.CurrentAccount}.
 */
@Getter
public enum CurrentTransactionStatus {

    EXECUTED("currentTransactionStatus.executed", "Executed"), //
    RELEASED("currentTransactionStatus.released", "Released"), //
    ; //

    private static final CurrentTransactionStatus[] VALUES = values();

    @NotNull
    private final String code;
    @NotNull
    private final String description;

    CurrentTransactionStatus(@NotNull String code, @NotNull String description) {
        this.code = code;
        this.description = description;
    }

    public boolean isActive() {
        return this == EXECUTED;
    }

}
