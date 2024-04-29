/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.search.data;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.portfolio.search.service.SearchUtil;

/**
 * Immutable data object representing datatable data.
 */
@Data
@AllArgsConstructor
public final class SelectColumnData implements Serializable {

    private final ResultsetColumnHeaderData columnHeader;

    private String resultColumn;

    public static SelectColumnData of(ResultsetColumnHeaderData columnHeader, String resultColumn) {
        return new SelectColumnData(columnHeader, resultColumn);
    }

    public static SelectColumnData of(ResultsetColumnHeaderData columnHeader) {
        return new SelectColumnData(columnHeader, columnHeader.getColumnName());
    }

    public String getColumnName() {
        return columnHeader.getColumnName();
    }

    public String calcAs() {
        return calcAs(true);
    }

    public String calcAs(boolean addDefault) {
        return SearchUtil.calcAs(columnHeader, addDefault);
    }

}
