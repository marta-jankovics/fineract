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
package org.apache.fineract.portfolio.statement.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StatementPublishType {

    S3(0, "statementPublishType.s3"),;

    public static final StatementPublishType[] VALUES = values();

    private static final Map<Integer, StatementPublishType> BY_ID = Arrays.stream(VALUES)
            .collect(Collectors.toMap(StatementPublishType::getId, v -> v));

    private final int id;
    private final String code;

    public static StatementPublishType fromId(final Integer id) {
        return BY_ID.get(id);
    }

    public boolean isDefault() {
        return this == S3;
    }

    public static StatementPublishType getDefault() {
        return S3;
    }
}
