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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;

@Getter
public final class JoinColumnHeaderData extends ResultsetColumnHeaderData {

    private final String virtualName;
    private final List<JoinData> joins;

    public JoinColumnHeaderData(@NotNull ResultsetColumnHeaderData columnHeader, String virtualName, List<JoinData> joins) {
        super(columnHeader.getColumnName(), columnHeader.getColumnType(), columnHeader.getColumnLength(), columnHeader.isColumnNullable(),
                columnHeader.isColumnPrimaryKey(), columnHeader.getColumnValues(), columnHeader.getColumnCode(),
                columnHeader.isColumnUnique(), columnHeader.isColumnIndexed());
        this.joins = joins == null ? new ArrayList<>(0) : joins;
        this.virtualName = virtualName == null ? getAlias() + "#" + columnHeader.getColumnName() : virtualName;
    }

    @Override
    public boolean isNamed(final String columnName) {
        return super.isNamed(columnName) || virtualName.equalsIgnoreCase(columnName);
    }

    public String getAlias() {
        return joins.isEmpty() ? null : joins.get(joins.size() - 1).getToAlias();
    }

    public String getTableName() {
        return joins.isEmpty() ? null : joins.get(joins.size() - 1).getToTable();
    }
}
