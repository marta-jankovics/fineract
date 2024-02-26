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

import static org.apache.fineract.portfolio.PortfolioProductType.SAVING;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SavingsStatementServiceImpl extends AccountStatementServiceImpl implements SavingsStatementService {

    private final ReadWriteNonCoreDataService nonCoreDataService;
    private final GenericDataService genericDataService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;

    public SavingsStatementServiceImpl(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, ReadWriteNonCoreDataService nonCoreDataService,
            GenericDataService genericDataService, DatabaseSpecificSQLGenerator sqlGenerator, JdbcTemplate jdbcTemplate,
            SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper) {
        super(statementParser, productStatementRepository, statementRepository);
        this.nonCoreDataService = nonCoreDataService;
        this.genericDataService = genericDataService;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.savingsAccountRepository = savingsAccountRepositoryWrapper;
    }

    // ----- AccountStatementServiceImpl -----

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == SAVING;
    }

    @Override
    protected String getAccountDiscriminator(@NotNull String accountId) {
        Long lAccountId = Long.valueOf(accountId);
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(lAccountId);
        HashMap<String, Object> accountDetails = getAccountDetails(account.clientId(), lAccountId);
        if (accountDetails != null) {
            if (accountId.equals(accountDetails.get("conversion_account_id"))) {
                return CONVERSION_ACCOUNT_DISCRIMINATOR;
            }
            if (accountId.equals(accountDetails.get("disposal_account_id"))) {
                return DISPOSAL_ACCOUNT_DISCRIMINATOR;
            }
        }
        return null;
    }

    @Override
    protected List<String> getStatementAccountIds(@NotNull String productId, @NotNull PortfolioProductType productType) {
        return savingsAccountRepository.findIdsForStatement(Long.valueOf(productId)).stream().map(Object::toString).toList();
    }

    @Override
    protected boolean preStatementCreate(@NotNull String accountId) {
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(Long.valueOf(accountId));
        return !account.isClosed();
    }

    @Override
    protected void postStatementCreate(@NotNull AccountStatement statement) {
        SavingsAccount account = savingsAccountRepository.findOneWithNotFoundDetection(Long.valueOf(statement.getAccountId()));
        if (account.isActive()) {
            statement.activate();
        }
    }

    // ----- SavingsStatementService -----

    @Override
    public HashMap<String, Object> getClientDetails(@NotNull Long clientId) {
        String dataTableName = "dt_client_details";
        GenericResultsetData clientDetails = nonCoreDataService.retrieveDatatableGenericResultSet(dataTableName, clientId, null, null);
        if (clientDetails == null || clientDetails.getData().isEmpty()) {
            return null;
        }
        List<ResultsetColumnHeaderData> columnHeaders = clientDetails.getColumnHeaders();
        final List<Object> clientValues = clientDetails.getData().get(0).getRow();
        return columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), clientValues.get(map.size())),
                (map, map2) -> { //
                });
    }

    @Override
    public HashMap<String, Object> getAccountDetails(@NotNull Long clientId, @NotNull Long accountId) {
        String dataTableName = "dt_client_account_mapping";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String clientIdQ = sqlGenerator.formatValue(headersByName.get("client_id").getColumnType(), clientId);
        String accountIdC = sqlGenerator.formatValue(headersByName.get("conversion_account_id").getColumnType(), accountId);
        String accountIdD = sqlGenerator.formatValue(headersByName.get("disposal_account_id").getColumnType(), accountId);
        String where = "client_id = " + clientIdQ + " and (conversion_account_id = " + accountIdC + " OR disposal_account_id = "
                + accountIdD + ")";
        String sql = "select * from " + sqlGenerator.escape(dataTableName) + " where " + where;

        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return null;
        }
        final List<Object> accountValues = accountDetails.get(0).getRow();
        return columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), accountValues.get(map.size())),
                (map, map2) -> { //
                });
    }

    @Override
    @NotNull
    public List<Long> getPendingTransactionIds(@NotNull Long accountId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return Collections.emptyList();
        }
        String dataTableName = "dt_savings_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String ids = transactionIds.stream()
                .map(e -> sqlGenerator.formatValue(headersByName.get("savings_transaction_id").getColumnType(), e))
                .collect(Collectors.joining(", "));
        String noPurpose = sqlGenerator.formatValue(headersByName.get("category_purpose_code").getColumnType(), "NA");
        String dbname = sqlGenerator.escape(dataTableName);
        String where = "dt.savings_transaction_id IN (" + ids + ") and not exists (select 1 from dt_savings_transaction_details dt2 "
                + "join m_savings_account_transaction st on dt2.savings_transaction_id = st.id "
                + "where dt.internal_correlation_id = dt2.internal_correlation_id "
                + "and dt.savings_transaction_id <> dt2.savings_transaction_id and st.savings_account_id = " + accountId
                + " and COALESCE(dt.category_purpose_code, " + noPurpose + ") = COALESCE(dt2.category_purpose_code, " + noPurpose + "))";
        String sql = "select dt.savings_transaction_id from " + dbname + " dt where " + where;

        columnHeaders.stream().filter(e -> "savings_transaction_id".equals(e.getColumnName())).findFirst()
                .orElseThrow(() -> new PlatformDataIntegrityException("error.msg.datatable.column.missing",
                        "Column savings_transaction_id does not exist.", "savings_transaction_id"));
        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return Collections.emptyList();
        }
        return accountDetails.stream().map(e -> (Long) e.getRow().get(0)).collect(Collectors.toList());
    }

    @Override
    public Map<Long, Map<String, Object>> getTransactionDetails(List<Long> transactionIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        if (transactionIds == null || transactionIds.isEmpty()) {
            return result;
        }
        String dataTableName = "dt_savings_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        columnHeaders.add(ResultsetColumnHeaderData.basic("entry_details", "VARCHAR", sqlGenerator.getDialect()));
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String ids = transactionIds.stream()
                .map(e -> sqlGenerator.formatValue(headersByName.get("savings_transaction_id").getColumnType(), e))
                .collect(Collectors.joining(", "));
        String dbname = sqlGenerator.escape(dataTableName);
        String where = "savings_transaction_id in (" + ids + ")";
        String sql = "select *, structured_transaction_details::json ->> 'EntryDetails' as entry_details " + "from " + dbname + " where "
                + where;

        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return result;
        }
        for (ResultsetRowData accountDetail : accountDetails) {
            List<Object> row = accountDetail.getRow();
            Long id = (Long) row.get(0);
            result.put(id, columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), row.get(map.size())),
                    (map, map2) -> { //
                    }));
        }
        return result;
    }

    @Override
    public boolean hasTransaction(@NotNull Long accountId, @NotNull Long transactionId, @NotNull String internalCorrelationId,
            String categoryPurposeCode, @NotNull List<SavingsAccountTransactionType> types) {
        String dataTableName = "dt_savings_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String typeCodes = types.stream().map(e -> e.getValue().toString()).collect(Collectors.joining(", "));
        String dbname = sqlGenerator.escape(dataTableName);
        String noPurpose = sqlGenerator.formatValue(headersByName.get("category_purpose_code").getColumnType(), "NA");
        String where = "st.savings_account_id = ? and st.id <> ? and st.transaction_type_enum in (" + typeCodes + ") "
                + "and dt.internal_correlation_id = ? and COALESCE(dt.category_purpose_code, " + noPurpose + ") = COALESCE(?, " + noPurpose
                + ")";
        String sql = "select case when (count(dt.savings_transaction_id) > 0) then 'true' else 'false' end from " + dbname + " dt "
                + "join m_savings_account_transaction st on dt.savings_transaction_id = st.id where " + where;

        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, accountId, transactionId, internalCorrelationId,
                categoryPurposeCode);
        return Boolean.TRUE.equals(result);
    }
}
