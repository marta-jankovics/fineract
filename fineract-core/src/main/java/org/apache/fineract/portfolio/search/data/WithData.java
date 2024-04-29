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
package org.apache.fineract.portfolio.search.data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;

@Data
public final class WithData implements Serializable {

    @NotNull
    private final String identifier;
    @NotNull
    private final String alias;
    List<ResultsetColumnHeaderData> selectColumns;
    private String select;
    JoinData join;

    public static WithData of(String identifier, String alias) {
        return new WithData(identifier, alias);
    }

    public ResultsetColumnHeaderData getSelectColumn() {
        return selectColumns == null || selectColumns.size() != 1 ? null : selectColumns.get(0);
    }

    public void setSelectColumn(@NotNull ResultsetColumnHeaderData selectColumn) {
        selectColumns = List.of(selectColumn);
    }
}
