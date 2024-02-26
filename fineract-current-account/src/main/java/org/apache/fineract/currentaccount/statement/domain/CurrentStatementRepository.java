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
package org.apache.fineract.currentaccount.statement.domain;

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
import org.apache.fineract.statement.data.dao.AccountStatementGenerationData;
import org.apache.fineract.statement.domain.AccountStatement;
import org.apache.fineract.statement.domain.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentStatementRepository extends JpaRepository<AccountStatement, Long> {

    @Query("select new org.apache.fineract.statement.data.dao.AccountStatementGenerationData(ast.id, ast.accountId, ast.nextStatementDate, "
            + "ca.productId, cast(ca.clientId as varchar), ps.statementCode, ps.statementType, ps.publishType, ps.batchType) "
            + "from AccountStatement ast, ProductStatement ps, CurrentAccount ca where ast.productStatement.id = ps.id and ast.accountId = ca.id "
            + "and ast.nextStatementDate < :transactionDate and ast.statementStatus in :statuses and ca.status in :currentStatuses")
    List<AccountStatementGenerationData> getStatementsToGenerate(@Param("transactionDate") LocalDate transactionDate,
            @Param("statuses") List<StatementStatus> statuses, @Param("currentStatuses") List<CurrentAccountStatus> currentStatuses);
}
