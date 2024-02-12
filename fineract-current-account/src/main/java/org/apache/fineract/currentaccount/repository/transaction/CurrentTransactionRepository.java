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
package org.apache.fineract.currentaccount.repository.transaction;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.transaction.CurrentTransactionType;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentTransactionRepository extends JpaRepository<CurrentTransaction, String> {

    CurrentTransaction getByExternalId(ExternalId externalId);

    String getIdByExternalId(ExternalId externalId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, "
            + "t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, "
            + "curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) "
            + "FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt "
            + "WHERE t.accountId = :accountId AND t.id = :transactionId AND ca.id = t.accountId AND ca.productId = cp.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    CurrentTransactionData getTransactionDataById(@Param("accountId") String accountId, @Param("transactionId") String transactionId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, "
            + "t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, "
            + "curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) "
            + "FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt "
            + "WHERE t.accountId = :accountId AND t.externalId = :externalId AND ca.id = t.accountId AND ca.productId = cp.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    CurrentTransactionData getTransactionDataByExternalId(@Param("accountId") String accountId, @Param("externalId") ExternalId externalId);

    @Query("SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, "
            + "t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, "
            + "curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) "
            + "FROM CurrentTransaction t, ApplicationCurrency curr, CurrentAccount ca, CurrentProduct cp, PaymentType pt "
            + "WHERE t.accountId = :accountId AND ca.id = t.accountId AND ca.productId = cp.id AND curr.code = cp.currency.code AND pt.id = t.paymentTypeId")
    Page<CurrentTransactionData> getTransactionsDataPage(@Param("accountId") String accountId, Pageable pageable);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime")
    List<CurrentTransaction> getTransactionsFrom(@Param("accountId") String accountId, @Param("fromDateTime") OffsetDateTime fromDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFromSorted(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate <= :tillDateTime")
    List<CurrentTransaction> getTransactionsTill(@Param("accountId") String accountId, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate <= :tillDateTime ORDER By t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsTillSorted(@Param("accountId") String accountId,
            @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime AND t.createdDate <= :tillDateTime")
    List<CurrentTransaction> getTransactionsFromAndTill(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime AND t.createdDate <= :tillDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFromAndTillSorted(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.submittedOnDate <= :toDate and t.transactionType in :types")
    List<CurrentTransaction> getTransactionsSubmittedTo(@Param("accountId") String accountId, @Param("toDate") LocalDate toDate,
            @Param("types") List<CurrentTransactionType> types);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.submittedOnDate > :fromDate and t.submittedOnDate <= :toDate "
            + "and t.transactionType in :types")
    List<CurrentTransaction> getTransactionsSubmittedFromTo(@Param("accountId") String accountId, @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate, @Param("types") List<CurrentTransactionType> types);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId")
    List<CurrentTransaction> getTransactions(@Param("accountId") String accountId);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId order by t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsSorted(@Param("accountId") String accountId);
}