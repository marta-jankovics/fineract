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
package org.apache.fineract.infrastructure.dataqueries.data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum StatusEnum {

    CREATE("create", 100), //
    APPROVE("approve", 200), //
    ACTIVATE("activate", 300), //
    WITHDRAWN("withdraw", 400), //
    REJECTED("reject", 500), //
    CLOSE("close", 600), //
    WRITE_OFF("write off", 601), //
    RESCHEDULE("reschedule", 602), //
    OVERPAY("overpay", 700), //
    DISBURSE("disburse", 800), //
    ;

    private static final StatusEnum[] VALUES = values();

    private static final Map<Integer, StatusEnum> BY_ID = Arrays.stream(VALUES).collect(Collectors.toMap(StatusEnum::getValue, v -> v));

    private final String name;

    private final Integer value;

    public Integer getValue() {
        return value;
    }

    StatusEnum(String name, Integer code) {
        this.name = name;
        this.value = code;
    }

    public static StatusEnum fromInt(final Integer value) {
        return BY_ID.get(value);
    }

    public static EnumOptionData statusTypeEnum(final Integer id) {
        return statusType(StatusEnum.fromInt(id));
    }

    public static EnumOptionData statusType(final StatusEnum statusType) {
        return statusType == null ? null : new EnumOptionData(statusType.getValue().longValue(), statusType.name(), statusType.name());
    }
}
