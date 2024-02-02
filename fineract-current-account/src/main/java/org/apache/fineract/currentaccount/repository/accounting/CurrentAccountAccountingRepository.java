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
package org.apache.fineract.currentaccount.repository.accounting;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.currentaccount.domain.accounting.GLAccountingHistory;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentAccountAccountingRepository extends JpaRepository<GLAccountingHistory, String> {

    @Query("SELECT ca.id FROM CurrentAccount ca, CurrentProduct cp, GLAccountingHistory glah, (SELECT ct.accountId, MAX(ct.createdDate) as createdDate "
            + "FROM CurrentTransaction ct WHERE ct.createdDate <= :tillDateTime GROUP BY ct.accountId) lct, CurrentTransaction fct "
            + "WHERE fct.id = glah.calculatedTillTransactionId AND ca.id = glah.accountId AND lct.accountId = ca.id AND lct.createdDate > fct.createdDate AND ca.productId = cp.id AND cp.accountingType = org.apache.fineract.accounting.common.AccountingRuleType.CASH_BASED")
    List<String> getAccountIdsWhereAccountingIsBehind(@Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT ca.id FROM CurrentAccount ca, CurrentProduct cp WHERE ca.id NOT IN (SELECT glah.accountId FROM GLAccountingHistory glah) AND ca.productId = cp.id AND cp.accountingType = org.apache.fineract.accounting.common.AccountingRuleType.CASH_BASED")
    List<String> getAccountIdsWhereAccountingNotCalculated();

    Optional<GLAccountingHistory> findByAccountTypeAndAccountId(PortfolioAccountType accountType, String accountId);
}
