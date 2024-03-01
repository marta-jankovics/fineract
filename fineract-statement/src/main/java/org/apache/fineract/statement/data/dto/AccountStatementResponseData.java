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
package org.apache.fineract.statement.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;

@Getter
@AllArgsConstructor
public class AccountStatementResponseData implements Serializable {

    private final String statementCode;
    private final StringEnumOptionData statementType;
    private final StringEnumOptionData publishType;
    private final StringEnumOptionData batchType;
    private final String recurrence;
    private final String sequencePrefix;
    private final StringEnumOptionData statementStatus;
    private final Integer sequenceNo;
    private final LocalDate statementDate;
    private final BigDecimal statementBalance;
    private final String resultCode;
    private final StringEnumOptionData resultStatus;
    private LocalDate resultPublishedOn;
    private final LocalDate nextStatementDate;
}
