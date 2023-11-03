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
package org.apache.fineract.statement.service;

import jakarta.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.data.AccountStatementPublishData;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementResultStatus;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountStatementPublishReadServiceImpl implements AccountStatementPublishReadService {

    private final NamedParameterJdbcTemplate namedParameterTemplate;

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return true;
    }

    @Override
    public Map<StatementType, Map<StatementPublishType, List<AccountStatementPublishData>>> retrieveStatementsToPublish(
            @NotNull PortfolioProductType productType, @NotNull LocalDate transactionDate) {
        final StatementPublishMapper rm = new StatementPublishMapper();
        final MapSqlParameterSource params = new MapSqlParameterSource("productType", productType.name()).addValue("publishedStatus",
                StatementResultStatus.PUBLISHED.name());
        List<AccountStatementPublishData> statementGenerations = namedParameterTemplate.query(rm.schema(), params, rm);
        return statementGenerations.stream().collect(Collectors.groupingBy(AccountStatementPublishData::getStatementType,
                Collectors.groupingBy(AccountStatementPublishData::getPublishType)));
    }

    private static final class StatementPublishMapper implements RowMapper<AccountStatementPublishData> {

        private static final String SELECT = "SELECT id as resultId, result_code as resultCode, product_type as productType, "
                + "statement_type as statementType, publish_type as publishType ";
        private static final String FROM = "FROM m_account_statement_result ";
        private static final String WHERE = "WHERE product_type = :productType AND result_status <> :publishedStatus";

        private static final String SCHEMA = SELECT + FROM + WHERE;

        public String schema() {
            return SCHEMA;
        }

        @Override
        public AccountStatementPublishData mapRow(@NotNull ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long resultId = JdbcSupport.getLong(rs, "resultId");
            final String resultCode = rs.getString("resultCode");
            final PortfolioProductType productType = PortfolioProductType.valueOf(rs.getString("productType"));
            final StatementType statementType = StatementType.valueOf(rs.getString("statementType"));
            final StatementPublishType publishType = StatementPublishType.valueOf(rs.getString("publishType"));
            return new AccountStatementPublishData(resultId, resultCode, productType, statementType, publishType);
        }
    }
}
