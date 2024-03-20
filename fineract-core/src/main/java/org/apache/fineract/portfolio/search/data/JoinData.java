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

import static jakarta.persistence.criteria.JoinType.INNER;
import static jakarta.persistence.criteria.JoinType.LEFT;

import jakarta.persistence.criteria.JoinType;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Getter
public final class JoinData implements Serializable {

    @NotNull
    private final JoinPart from;
    @NotNull
    private final JoinPart to;
    @NotNull
    private JoinType joinType;
    private ColumnConditionData joinCondition;

    public JoinData(@NotNull String fromTable, @NotNull String fromColumn, String fromAlias, @NotNull String toTable,
            @NotNull String toColumn, @NotNull String toAlias, JoinType joinType) {
        this.from = new JoinPart(fromTable, fromColumn, fromAlias);
        this.to = new JoinPart(toTable, toColumn, toAlias);
        this.joinType = joinType == null ? LEFT : joinType;
    }

    public String getFromTable() {
        return from.table;
    }

    public String getFromColumn() {
        return from.column;
    }

    public String getFromAlias() {
        return from.alias;
    }

    public String getToTable() {
        return to.table;
    }

    public String getToColumn() {
        return to.column;
    }

    public String getToAlias() {
        return to.alias;
    }

    public JoinData ensureType(@NotNull JoinType joinType) {
        if (this.joinType != INNER) {
            if (joinType == INNER) {
                this.joinType = joinType;
            }
        }
        return this;
    }

    public JoinData ensureJoinCondition(@NotNull ColumnConditionData joinCondition) {
        if (this.joinCondition == null) {
            this.joinCondition = joinCondition;
        }
        return this;
    }

    @RequiredArgsConstructor
    @Getter
    public static final class JoinPart {

        private final String table;
        private final String column;
        private final String alias;
    }
}
