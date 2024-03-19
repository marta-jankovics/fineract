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
import java.util.Optional;
import org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData;
import org.apache.fineract.currentaccount.domain.transaction.CurrentTransaction;
import org.apache.fineract.currentaccount.enumeration.account.CurrentAccountStatus;
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

    String TRANSACTION_DATA_SELECT = "SELECT new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, "
            + "t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, cp.currency.code, cp.currency.digitsAfterDecimal, cp.currency.inMultiplesOf, "
            + "curr.name, curr.displaySymbol, pt.id, pt.name, pt.description, pt.isCashPayment, pt.codeName) "
            + "FROM CurrentTransaction t JOIN CurrentAccount ca on ca.id = t.accountId "
            + "JOIN CurrentProduct cp on cp.id = ca.productId JOIN ApplicationCurrency curr on curr.code = cp.currency.code "
            + "LEFT JOIN PaymentType pt on pt.id = t.paymentTypeId ";

    @Query("SELECT t.id FROM CurrentTransaction t WHERE t.externalId = :externalId")
    String getIdByExternalId(@Param("externalId") ExternalId externalId);

    @Query(TRANSACTION_DATA_SELECT + "WHERE t.accountId = :accountId AND t.id = :transactionId")
    CurrentTransactionData getTransactionDataById(@Param("accountId") String accountId, @Param("transactionId") String transactionId);

    @Query(TRANSACTION_DATA_SELECT + "WHERE t.accountId = :accountId AND t.externalId = :externalId")
    CurrentTransactionData getTransactionDataByExternalId(@Param("accountId") String accountId, @Param("externalId") ExternalId externalId);

    boolean existsByTransactionTypeAndReferenceId(CurrentTransactionType currentTransactionType, String referenceId);

    Optional<CurrentTransaction> findByIdAndAccountId(String id, String accountId);

    @Query(TRANSACTION_DATA_SELECT + "WHERE t.accountId = :accountId")
    Page<CurrentTransactionData> getTransactionsDataPage(@Param("accountId") String accountId, Pageable pageable);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime")
    List<CurrentTransaction> getTransactionsFrom(@Param("accountId") String accountId, @Param("fromDateTime") OffsetDateTime fromDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFromSorted(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate <= :tillDateTime ORDER By t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsTillSorted(@Param("accountId") String accountId,
            @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.createdDate > :fromDateTime AND t.createdDate <= :tillDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsFromAndTillSorted(@Param("accountId") String accountId,
            @Param("fromDateTime") OffsetDateTime fromDateTime, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId")
    List<CurrentTransaction> getTransactions(@Param("accountId") String accountId);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId order by t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsSorted(@Param("accountId") String accountId);

    @Query("SELECT t FROM CurrentTransaction t WHERE t.accountId = :accountId AND t.submittedOnDate > :fromDate AND t.createdDate <= :tillDateTime ORDER BY t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsSubmittedFromAndTillSorted(@Param("accountId") String accountId,
            @Param("fromDate") LocalDate fromDate, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.submittedOnDate <= :toDate and t.transactionType in :types")
    List<CurrentTransaction> getTransactionsSubmittedTo(@Param("accountId") String accountId, @Param("toDate") LocalDate toDate,
            @Param("types") List<CurrentTransactionType> types);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.submittedOnDate > :fromDate and t.submittedOnDate <= :toDate "
            + "and t.transactionType in :types")
    List<CurrentTransaction> getTransactionsSubmittedFromTo(@Param("accountId") String accountId, @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate, @Param("types") List<CurrentTransactionType> types);

    @Query("select new org.apache.fineract.currentaccount.data.transaction.CurrentTransactionData(t.id, t.accountId, t.externalId, t.transactionType, "
            + "t.transactionDate, t.submittedOnDate, t.amount, t.createdDate, pt.id, pt.name) "
            + "from CurrentTransaction t left join PaymentType pt on pt.id = t.paymentTypeId "
            + "where t.accountId = :accountId and t.submittedOnDate >= :fromDate and t.submittedOnDate <= :toDate "
            + "and t.transactionType in :types order by t.submittedOnDate, t.createdDate, t.id")
    List<CurrentTransactionData> getTransactionsDataForStatement(@Param("accountId") String accountId,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("types") List<CurrentTransactionType> types);

    @Query("select t.accountId, t.id from CurrentTransaction t join CurrentAccount ca on t.accountId = ca.id "
            + "where t.sequenceNo is null and t.createdDate <= :tillDateTime and ca.status in :statuses")
    List<String[]> getTransactionIdsForMetadata(@Param("tillDateTime") OffsetDateTime tillDateTime,
            @Param("statuses") List<CurrentAccountStatus> statuses);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.id in :transactionIds order by t.submittedOnDate, t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsForMetadata(@Param("accountId") String accountId,
            @Param("transactionIds") List<String> transactionIds);

    @Query("select t from CurrentTransaction t where t.accountId = :accountId and t.id in :transactionIds and t.createdDate <= :tillDateTime "
            + "order by t.submittedOnDate, t.createdDate, t.id")
    List<CurrentTransaction> getTransactionsForMetadataTill(@Param("accountId") String accountId,
            @Param("transactionIds") List<String> transactionIds, @Param("tillDateTime") OffsetDateTime tillDateTime);

    @Query("select max(t.sequenceNo) from CurrentTransaction t where t.accountId = :accountId and t.submittedOnDate = :date")
    Integer getMaxSequenceNo(@Param("accountId") String accountId, @Param("date") LocalDate date);
}
