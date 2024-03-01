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

import static org.apache.fineract.currentaccount.enumeration.account.CurrentAccountAction.UPDATE;
import static org.apache.fineract.interoperation.domain.InteropIdentifierType.IBAN;
import static org.apache.fineract.portfolio.account.PortfolioAccountType.CURRENT;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.domain.account.AccountIdentifier;
import org.apache.fineract.currentaccount.domain.account.CurrentAccount;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.currentaccount.repository.account.CurrentAccountRepository;
import org.apache.fineract.currentaccount.repository.accountidentifiers.AccountIdentifierRepository;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.core.service.database.JdbcJavaType;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.apache.fineract.statement.data.StatementParser;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.AccountStatementRepository;
import org.apache.fineract.statement.domain.ProductStatementRepository;
import org.apache.fineract.statement.service.AccountStatementServiceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CurrentStatementServiceImpl extends AccountStatementServiceImpl implements CurrentStatementService {

    private final ReadWriteNonCoreDataService nonCoreDataService;
    private final GenericDataService genericDataService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentAccountRepository accountRepository;
    private final AccountIdentifierRepository accountIdentifierRepository;

    public CurrentStatementServiceImpl(StatementParser statementParser, ProductStatementRepository productStatementRepository,
            AccountStatementRepository statementRepository, ReadWriteNonCoreDataService nonCoreDataService,
            GenericDataService genericDataService, DatabaseSpecificSQLGenerator sqlGenerator, JdbcTemplate jdbcTemplate,
            CurrentAccountRepository currentAccountRepository, AccountIdentifierRepository accountIdentifierRepository) {
        super(statementParser, productStatementRepository, statementRepository);
        this.nonCoreDataService = nonCoreDataService;
        this.genericDataService = genericDataService;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.accountRepository = currentAccountRepository;
        this.accountIdentifierRepository = accountIdentifierRepository;
    }

    // ----- AccountStatementServiceImpl -----

    @Override
    public boolean isSupport(PortfolioProductType productType) {
        return productType == PortfolioProductType.CURRENT;
    }

    @Override
    protected String getAccountDiscriminator(@NotNull String accountId) {
        AccountIdentifier identifier = accountIdentifierRepository.getByAccountTypeAndAccountIdAndIdentifierType(CURRENT, accountId, IBAN);
        return identifier == null ? null : identifier.getSubValue();
    }

    @Override
    protected List<String> getStatementAccountIds(@NotNull String productId, @NotNull PortfolioProductType productType) {
        return accountRepository.getIdsByProductIdAndStatus(productId, CurrentAccountStatus.getEnabledStatusList(UPDATE));
    }

    @Override
    protected void postStatementCreate(@NotNull AccountStatement statement) {
        String accountId = statement.getAccountId();
        CurrentAccount currentAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("current.account", accountId));
        if (currentAccount.getStatus().isActive()) {
            statement.activate();
        }
    }

    // ----- CurrentStatementService -----

    @Override
    public HashMap<String, Object> retrieveClientDetails(@NotNull Long clientId) {
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
    @NotNull
    public List<String> getPendingTransactionIds(@NotNull String accountId, List<String> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return Collections.emptyList();
        }
        String dataTableName = "dt_current_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String ids = transactionIds.stream()
                .map(e -> sqlGenerator.formatValue(headersByName.get("current_transaction_id").getColumnType(), e))
                .collect(Collectors.joining(", "));
        String noPurpose = sqlGenerator.formatValue(headersByName.get("category_purpose_code").getColumnType(), "NA");
        String dbname = sqlGenerator.escape(dataTableName);
        String where = "dt.current_transaction_id IN (" + ids + ") and not exists (select 1 from dt_current_transaction_details dt2 "
                + "join m_current_transaction t on dt2.current_transaction_id = t.id "
                + "where dt.internal_correlation_id = dt2.internal_correlation_id "
                + "and dt.current_transaction_id <> dt2.current_transaction_id and t.account_id = "
                + sqlGenerator.formatValue(JdbcJavaType.CHAR, accountId) + " and COALESCE(dt.category_purpose_code, " + noPurpose
                + ") = COALESCE(dt2.category_purpose_code, " + noPurpose + "))";
        String sql = "select dt.current_transaction_id from " + dbname + " dt where " + where;

        columnHeaders.stream().filter(e -> "current_transaction_id".equals(e.getColumnName())).findFirst()
                .orElseThrow(() -> new PlatformDataIntegrityException("error.msg.datatable.column.missing",
                        "Column current_transaction_id does not exist.", "current_transaction_id"));
        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return Collections.emptyList();
        }
        return accountDetails.stream().map(e -> (String) e.getRow().get(0)).collect(Collectors.toList());
    }

    @Override
    @NotNull
    public Map<String, Map<String, Object>> getTransactionDetails(List<String> transactionIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (transactionIds == null || transactionIds.isEmpty()) {
            return result;
        }
        String dataTableName = "dt_current_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        columnHeaders.add(ResultsetColumnHeaderData.basic("entry_details", "VARCHAR", sqlGenerator.getDialect()));
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String ids = transactionIds.stream()
                .map(e -> sqlGenerator.formatValue(headersByName.get("current_transaction_id").getColumnType(), e))
                .collect(Collectors.joining(", "));
        String dbname = sqlGenerator.escape(dataTableName);
        String where = "current_transaction_id in (" + ids + ")";
        String sql = "select *, structured_transaction_details::json#>> '{}' as entry_details " + "from " + dbname + " where " + where;

        final List<ResultsetRowData> accountDetails = genericDataService.fillResultsetRowData(sql, columnHeaders);
        if (accountDetails == null || accountDetails.isEmpty()) {
            return result;
        }
        for (ResultsetRowData accountDetail : accountDetails) {
            List<Object> row = accountDetail.getRow();
            String id = (String) row.get(0);
            result.put(id, columnHeaders.stream().collect(HashMap::new, (map, e) -> map.put(e.getColumnName(), row.get(map.size())),
                    (map, map2) -> { //
                    }));
        }
        return result;
    }

    @Override
    public boolean hasTransaction(@NotNull String accountId, @NotNull String transactionId, @NotNull String internalCorrelationId,
            String categoryPurposeCode, @NotNull List<CurrentTransactionType> types) {
        String dataTableName = "dt_current_transaction_details";
        final List<ResultsetColumnHeaderData> columnHeaders = genericDataService.fillResultsetColumnHeaders(dataTableName);
        Map<String, ResultsetColumnHeaderData> headersByName = SearchUtil.mapHeadersToName(columnHeaders);
        String typeNames = types.stream().map(e -> sqlGenerator.formatValue(JdbcJavaType.VARCHAR, e.name()))
                .collect(Collectors.joining(", "));
        String dbname = sqlGenerator.escape(dataTableName);
        String noPurpose = sqlGenerator.formatValue(headersByName.get("category_purpose_code").getColumnType(), "NA");
        String where = "t.account_id = ? and t.id <> ? and t.transaction_type_enum in (" + typeNames + ") "
                + "and dt.internal_correlation_id = ? and COALESCE(dt.category_purpose_code, " + noPurpose + ") = COALESCE(?, " + noPurpose
                + ")";
        String sql = "select case when (count(dt.current_transaction_id) > 0) then 'true' else 'false' end from " + dbname + " dt "
                + "join m_current_transaction t on dt.current_transaction_id = t.id where " + where;

        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, accountId, transactionId, internalCorrelationId,
                categoryPurposeCode);
        return Boolean.TRUE.equals(result);
    }
}
