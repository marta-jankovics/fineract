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
package org.apache.fineract.portfolio.savings.statement.service;

import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;
import static org.apache.fineract.portfolio.statement.domain.StatementStatus.ACTIVE;

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
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.statement.data.AccountStatementGenerationData;
import org.apache.fineract.portfolio.statement.domain.StatementBatchType;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationReadService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsStatementGenerationReadServiceImpl implements AccountStatementGenerationReadService {

    private final NamedParameterJdbcTemplate namedParameterTemplate;

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == SAVING;
    }

    @Override
    public Map<StatementType, Map<StatementPublishType, Map<String, List<AccountStatementGenerationData>>>> retrieveStatementsToGenerate(
            @NotNull PortfolioProductType productType, @NotNull LocalDate transactionDate) {
        final StatementGenerationMapper rm = new StatementGenerationMapper();

        HashMap<String, Object> params = new HashMap<>();
        params.put("transactionDate", transactionDate);
        params.put("statementStatus", ACTIVE.name());

        List<AccountStatementGenerationData> statementGenerations = namedParameterTemplate.query(rm.schema(), params, rm);
        return statementGenerations.stream().collect(Collectors.groupingBy(AccountStatementGenerationData::getStatementType,
                Collectors.groupingBy(AccountStatementGenerationData::getPublishType, Collectors.groupingBy(this::calcBatchId))));
    }

    private String calcBatchId(AccountStatementGenerationData generation) {
        return switch (generation.getBatchType()) {
            case SINGLE -> String.valueOf(generation.getAccountStatementId());
            case ACCOUNT -> String.valueOf(generation.getAccountId());
            case PRODUCT -> String.valueOf(generation.getProductId());
            case CLIENT -> String.valueOf(generation.getProductId()) + '/' + generation.getClientId();
            default -> String.valueOf(generation.getAccountStatementId());
        };
    }

    private static final class StatementGenerationMapper implements RowMapper<AccountStatementGenerationData> {

        private static final String SELECT = "SELECT sa.client_id as clientId, sa.group_id as groupId, sa.product_id as productId, "
                + "st.account_id as accountId, st.id as accountStatementId, st.next_statement_date as generationDate, ps.statement_code as statementCode, "
                + "ps.statement_type as statementType, ps.publish_type as publishType, ps.batch_type as batchType ";
        private static final String FROM = "FROM m_account_statement st JOIN m_savings_account sa on st.account_id = sa.id "
                + "JOIN m_product_statement ps on st.product_statement_id = ps.id ";
        private static final String WHERE = "WHERE st.next_statement_date <= :transactionDate AND st.statement_status = :statementStatus AND sa.status_enum = "
                + SavingsAccountStatusType.ACTIVE.getValue();

        private static final String SCHEMA = SELECT + FROM + WHERE;

        public String schema() {
            return SCHEMA;
        }

        @Override
        public AccountStatementGenerationData mapRow(@NotNull ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final Long accountId = JdbcSupport.getLong(rs, "accountId");
            final Long accountStatementId = JdbcSupport.getLong(rs, "accountStatementId");
            final LocalDate generationDate = JdbcSupport.getLocalDate(rs, "generationDate");
            final String statementCode = rs.getString("statementCode");
            final StatementType statementType = StatementType.valueOf(rs.getString("statementType"));
            final StatementPublishType publishType = StatementPublishType.valueOf(rs.getString("publishType"));
            final StatementBatchType batchType = StatementBatchType.valueOf(rs.getString("batchType"));
            String clientOrGroupId = clientId == null ? ("G" + groupId) : ("C" + clientId);
            return new AccountStatementGenerationData(accountStatementId, accountId, generationDate, productId, clientOrGroupId, SAVING,
                    statementCode, statementType, publishType, batchType);
        }
    }
}
