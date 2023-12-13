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
package org.apache.fineract.portfolio.statement.data.camt053;

import static org.apache.fineract.infrastructure.core.service.DateUtils.DEFAULT_DATE_FORMATTER;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DateTimePeriodData {

    public static final String DATETIME_PATTERN = "YYYY-MM-DD'T'hh:mm:ss";
    public static final String START_OF_DAY = "T00:00:00";
    public static final String END_OF_DAY = "T24:00:00";

    @NotNull
    @JsonProperty(value = "FromDateTime", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_PATTERN)
    private final String fromDateTime;
    @NotNull
    @JsonProperty(value = "ToDateTime", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATETIME_PATTERN)
    private final String toDateTime;

    public static DateTimePeriodData create(@NotNull LocalDate fromDate, @NotNull LocalDate toDate) {
        return new DateTimePeriodData(fromDate.format(DEFAULT_DATE_FORMATTER) + START_OF_DAY,
                toDate.format(DEFAULT_DATE_FORMATTER) + END_OF_DAY);
    }
}
