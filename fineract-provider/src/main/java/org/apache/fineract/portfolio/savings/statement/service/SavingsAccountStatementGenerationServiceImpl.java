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

import static java.lang.String.format;
import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;

import jakarta.ws.rs.core.Response;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.products.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.portfolio.savings.statement.data.SavingsStatementData;
import org.apache.fineract.portfolio.savings.statement.data.SupplementaryEnvelopeData;
import org.apache.fineract.portfolio.statement.data.AccountStatementGenerationData;
import org.apache.fineract.portfolio.statement.data.camt053.Camt053Data;
import org.apache.fineract.portfolio.statement.data.camt053.GroupHeaderData;
import org.apache.fineract.portfolio.statement.data.camt053.StatementData;
import org.apache.fineract.portfolio.statement.data.camt053.SupplementaryData;
import org.apache.fineract.portfolio.statement.domain.AccountStatement;
import org.apache.fineract.portfolio.statement.domain.AccountStatementRepository;
import org.apache.fineract.portfolio.statement.domain.StatementBatchType;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.apache.fineract.statement.service.AccountStatementGenerationService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsAccountStatementGenerationServiceImpl implements AccountStatementGenerationService {

    private final NamedParameterJdbcTemplate namedParameterTemplate;
    private final ReadWriteNonCoreDataService nonCoreDataService;
    private final GenericDataService genericDataService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final AccountStatementRepository statementRepository;
    private final SavingsAccountRepository accountRepository;

    public boolean isSupportProductType(PortfolioProductType productType) {
        return productType == SAVING;
    }

    @Override
    public List<AccountStatementGenerationData> retrieveStatementsToGenerate(LocalDate transactionDate) {
        final StatementGenerationMapper rm = new StatementGenerationMapper();
        final MapSqlParameterSource params = new MapSqlParameterSource("transactionDate", transactionDate);

        return namedParameterTemplate.query(rm.schema(), params, rm);
    }

    @Override
    public Response generateStatement(AccountStatementGenerationData statementGeneration, LocalDate transactionDate) {
        Long statementId = statementGeneration.getAccountStatementId();
        AccountStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("account.statement", statementId.toString()));
        Long accountId = statement.getAccountId();
        SavingsAccount account = accountRepository.findById(accountId).orElseThrow(() -> new SavingsAccountNotFoundException(accountId));

        GroupHeaderData headerData = new GroupHeaderData(UUID.randomUUID().toString(), DateUtils.getAuditOffsetDateTime());

        Long clientId = account.clientId();
        HashMap<String, Object> clientDetails = retrieveClientDetails(clientId);
        SupplementaryData supplementaryData = new SupplementaryData(SupplementaryEnvelopeData.create(clientDetails));

        HashMap<String, Object> accountDetails = retrieveAccountDetails(clientId, accountId);
        boolean isConversionAccount = accountDetails != null && accountId.equals(accountDetails.get("conversion_account_id"));

        String pfx = statement.getSequencePrefix();
        int year = transactionDate.getYear();
        String seq = StringUtils.leftPad(statement.getSequenceNo().toString(), 2, '0');
        String identification = pfx == null ? format("%s/%s", year, seq) : format("%s-%s/%s", pfx, year, seq);
        StatementData[] statements;
        if (isConversionAccount) {
            statements = new StatementData[2];
        } else {
            statements = new StatementData[1];
            SavingsStatementData.create(statement, account, accountDetails, transactionDate, identification);
        }

        Camt053Data result = new Camt053Data(headerData, statements, supplementaryData);
        statement.initNext(transactionDate);
        return null;
    }

    private static final class StatementGenerationMapper implements RowMapper<AccountStatementGenerationData> {

        private static final String SELECT = "SELECT as.id as accountStatementId, ps.statement_code as statementCode, ps.statement_type as statementType, "
                + " ps.publish_type as publishType, ps.batch_type as batchType ";
        private static final String FROM = "FORM m_account_statement as " + "JOIN m_savings_account sa on as.account_id = sa.id "
                + "JOIN m_product_statement ps on as.product_statement_id = ps.id ";
        private static final String WHERE = "WHERE as.next_date = :transactionDate AND " + "(sa.status_enum = "
                + SavingsAccountStatusType.ACTIVE.getValue() + " OR (sa.status_enum = " + SavingsAccountStatusType.CLOSED.getValue()
                + " AND (as.last_date IS NULL OR sa.closedon_date > as.last_date)))";

        private static final String SCHEMA = SELECT + FROM + WHERE;

        public String schema() {
            return SCHEMA;
        }

        @Override
        public AccountStatementGenerationData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long accountStatementId = JdbcSupport.getLong(rs, "accountStatementId");
            final String statementCode = rs.getString("statementCode");
            final StatementType statementType = StatementType.valueOf(rs.getString("statementType"));
            final StatementPublishType publishType = StatementPublishType.valueOf(rs.getString("publishType"));
            final StatementBatchType batchType = StatementBatchType.valueOf(rs.getString("batchType"));
            return new AccountStatementGenerationData(accountStatementId, SAVING, statementCode, statementType, publishType, batchType);
        }
    }

    private HashMap<String, Object> retrieveClientDetails(Long clientId) {
        String dataTableName = "dt_client_details";
        GenericResultsetData clientDetails = nonCoreDataService.retrieveDataTableGenericResultSet(dataTableName, clientId, null, null);
        if (clientDetails == null || clientDetails.getData().isEmpty()) {
            return null;
        }
        List<ResultsetColumnHeaderData> columnHeaders = clientDetails.getColumnHeaders();
        final List<Object> clientValues = clientDetails.getData().get(0).getRow();
        return columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), clientValues.get(map.size())),
                (map, map2) -> {});
    }

    private HashMap<String, Object> retrieveAccountDetails(Long clientId, Long accountId) {
        String dataTableName = "dt_client_account_mapping";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        String whereClause = "client_id = " + clientId + " AND (conversion_account_id = " + accountId + " OR disposal_account_id = "
                + accountId + ")";
        String sql = "select * from " + sqlGenerator.escape(dataTableName) + " where " + whereClause;

        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return null;
        }
        final List<Object> accountValues = accountDetails.get(0).getRow();
        return columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), accountValues.get(map.size())),
                (map, map2) -> {});
    }
}
