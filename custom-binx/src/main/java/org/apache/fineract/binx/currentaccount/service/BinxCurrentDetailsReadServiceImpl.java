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
package org.apache.fineract.binx.currentaccount.service;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.core.service.database.JdbcJavaType;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.portfolio.search.service.SearchUtil;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
public class BinxCurrentDetailsReadServiceImpl implements BinxCurrentDetailsReadService {

    private final ReadWriteNonCoreDataService nonCoreDataService;
    private final GenericDataService genericDataService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;

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
    @NotNull
    public List<Map<String, Object>> getTransactionMetadataDetails(@NotNull Long officeId) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        String dataTableName = "dt_current_transaction_metadata_config";
        GenericResultsetData metadataDetails = nonCoreDataService.retrieveDatatableGenericResultSet(dataTableName, officeId, null, null);
        if (metadataDetails == null || metadataDetails.getData().isEmpty()) {
            return result;
        }
        List<ResultsetColumnHeaderData> columnHeaders = metadataDetails.getColumnHeaders();
        metadataDetails.getData().forEach(m -> result.add(columnHeaders.stream().collect(HashMap::new,
                (map, e) -> map.put(e.getColumnName(), m.getRow().get(map.size())), (map, map2) -> { //
                })));
        return result;
    }
}
