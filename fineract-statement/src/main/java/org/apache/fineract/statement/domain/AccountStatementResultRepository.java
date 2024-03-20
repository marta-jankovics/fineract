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
package org.apache.fineract.statement.domain;

import java.util.Collection;
import java.util.List;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementPublishData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountStatementResultRepository
        extends JpaRepository<AccountStatementResult, Long>, JpaSpecificationExecutor<AccountStatementResult> {

    @Query("select case when (count(st) > 0) then 'true' else 'false' end from AccountStatement st where st.statementResult.id = :resultId and st.id not in :statementIds")
    boolean hasAccountReference(@Param("resultId") Long resultId, @Param("statementIds") Collection<Long> statementIds);

    @Query("select new org.apache.fineract.statement.data.dao.AccountStatementPublishData(asr.id, asr.resultCode, asr.productType, asr.statementType, asr.publishType) "
            + "from AccountStatementResult asr where asr.productType = :productType and asr.resultStatus in :statuses")
    List<AccountStatementPublishData> getStatementsDataToPublish(@Param("productType") PortfolioProductType productType,
            @Param("statuses") List<StatementResultStatus> statuses);
}