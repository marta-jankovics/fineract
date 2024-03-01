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
package org.apache.fineract.statement.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;

@RequiredArgsConstructor
@Getter
public enum StatementStatus {

    INACTIVE(0, "statementStatus.inactive"), //
    ACTIVE(1, "statementStatus.active"), //
    ;

    public static final StatementStatus[] VALUES = values();

    private static final Map<Integer, StatementStatus> BY_ID = Arrays.stream(VALUES)
            .collect(Collectors.toMap(StatementStatus::getId, v -> v));

    private final int id;
    private final String code;

    public static StatementStatus fromId(final Integer id) {
        return BY_ID.get(id);
    }

    public boolean isDefault() {
        return false;
    }

    public StatementStatus inactivate() {
        return INACTIVE;
    }

    public StatementStatus activate() {
        return ACTIVE;
    }

    public boolean canGenerate() {
        return this == ACTIVE;
    }

    public StatementStatus generate() {
        if (!canGenerate()) {
            throw new PlatformDataIntegrityException("error.msg.invalid.statement.status", "Can not perform action: statement generation",
                    this);
        }
        return this;
    }

    public static List<StatementStatus> getFiltered(Predicate<? super StatementStatus> predicate) {
        return Arrays.stream(VALUES).filter(predicate).toList();
    }

    public StringEnumOptionData toStringEnumOptionData() {
        return new StringEnumOptionData(name(), getCode(), name());
    }
}
