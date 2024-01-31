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
package org.apache.fineract.currentaccount.statement.service;

import static org.apache.fineract.portfolio.PortfolioProductType.CURRENT;
import static org.apache.fineract.statement.domain.StatementStatus.ACTIVE;

import jakarta.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.StatementBatchType;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentStatementGenerationReadServiceImpl implements AccountStatementGenerationReadService {

    private final NamedParameterJdbcTemplate namedParameterTemplate;

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == CURRENT;
    }

    @Override
    public Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> retrieveStatementsToGenerate(
            @NotNull PortfolioProductType productType, @NotNull LocalDate transactionDate) {
        final StatementGenerationMapper rm = new StatementGenerationMapper();

        HashMap<String, Object> params = new HashMap<>();
        params.put("transactionDate", transactionDate);
        params.put("statementStatus", ACTIVE.name());
        params.put("accountStatus", CurrentAccountStatus.ACTIVE.name());

        List<AccountStatementGenerationData> statementGenerations = namedParameterTemplate.query(rm.schema(), params, rm);
        return statementGenerations.stream().collect(Collectors.groupingBy(AccountStatementGenerationData::getStatementType,
                Collectors.groupingBy(AccountStatementGenerationData::getPublishType, Collectors.groupingBy(this::calcBatchId))));
    }

    private static final class StatementGenerationMapper implements RowMapper<AccountStatementGenerationData> {

        private static final String SELECT = "SELECT ca.client_id as clientId, ca.product_id as productId, "
                + "st.account_id as accountId, st.id as accountStatementId, st.next_statement_date as generationDate, ps.statement_code as statementCode, "
                + "ps.statement_type as statementType, ps.publish_type as publishType, ps.batch_type as batchType ";
        private static final String FROM = "FROM m_account_statement st JOIN m_current_account ca on st.account_id = ca.id "
                + "JOIN m_product_statement ps on st.product_statement_id = ps.id ";
        private static final String WHERE = "WHERE st.next_statement_date < :transactionDate AND st.statement_status = :statementStatus AND ca.status_enum = :accountStatus";

        private static final String SCHEMA = SELECT + FROM + WHERE;

        public String schema() {
            return SCHEMA;
        }

        @Override
        public AccountStatementGenerationData mapRow(@NotNull ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final Long accountId = JdbcSupport.getLong(rs, "accountId");
            final Long accountStatementId = JdbcSupport.getLong(rs, "accountStatementId");
            final LocalDate generationDate = JdbcSupport.getLocalDate(rs, "generationDate");
            final String statementCode = rs.getString("statementCode");
            final StatementType statementType = StatementType.valueOf(rs.getString("statementType"));
            final StatementPublishType publishType = StatementPublishType.valueOf(rs.getString("publishType"));
            final StatementBatchType batchType = StatementBatchType.valueOf(rs.getString("batchType"));
            return new AccountStatementGenerationData(accountStatementId, accountId, generationDate, productId, clientId.toString(),
                    CURRENT, statementCode, statementType, publishType, batchType);
        }
    }
}
