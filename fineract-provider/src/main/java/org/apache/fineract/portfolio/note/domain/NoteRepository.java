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
package org.apache.fineract.portfolio.note.domain;

import java.util.List;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {

    List<Note> findByLoanId(Long id);

    List<Note> findByClient(Client id);

    List<Note> findByGroup(Group group);

    Note findByLoanAndId(Loan loanId, Long id);

    Note findByClientAndId(Client client, Long id);

    Note findByGroupAndId(Group group, Long id);

    Note findByLoanTransactionAndId(LoanTransaction loanTransaction, Long id);

    List<Note> findBySavingsAccount(SavingsAccount savingAccount);

    Note findBySavingsAccountAndId(SavingsAccount savingAccount, Long id);

    @Query("select note from Note note where note.savingsTransaction.id = :savingsTransactionId")
    List<Note> findBySavingsTransactionId(@Param("savingsTransactionId") Long savingsTransactionId);

    Note getByNoteTypeIdAndEntityIdentifierAndId(Integer noteTypeId, String entityIdentifier, Long id);

    List<Note> getByNoteTypeIdAndEntityIdentifier(Integer noteTypeId, String entityIdentifier);

    String NOTE_DATA_SELECT = "select new org.apache.fineract.portfolio.note.data.NoteData(n.id, n.client.id, n.group.id, n.loan.id, "
            + "n.loanTransaction.id, n.savingsAccount.id, n.savingsTransaction.id, n.shareAccount.id, n.entityIdentifier, n.noteTypeId, "
            + "n.note, n.createdBy, cb.username, n.createdDate, n.lastModifiedBy, mb.username, n.lastModifiedDate) "
            + "from Note n left join AppUser cb on n.createdBy = cb.id left join AppUser mb on n.lastModifiedBy = mb.id ";
    String NOTE_DATA_ORDER = " order by n.createdDate DESC";
    String NOTE_DATA_ID_WHERE = " and n.id = :id ";

    @Query(NOTE_DATA_SELECT + "where n.client.id = :clientId and n.noteTypeId = 100" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByClientId(@Param("clientId") Long clientId);

    @Query(NOTE_DATA_SELECT + "where n.group.id = :groupId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByGroupId(@Param("groupId") Long groupId);

    @Query(NOTE_DATA_SELECT + "where n.loan.id = :loanId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByLoanId(@Param("loanId") Long loanId);

    @Query(NOTE_DATA_SELECT + "where n.loanTransaction.id = :loanTransactionId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByLoanTransactionId(@Param("loanTransactionId") Long loanTransactionId);

    @Query(NOTE_DATA_SELECT + "where n.savingsAccount.id = :savingsAccountId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataBySavingsAccountId(@Param("savingsAccountId") Long savingsAccountId);

    @Query(NOTE_DATA_SELECT + "where n.savingsTransaction.id = :savingsTransactionId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataBySavingsTransactionId(@Param("savingsTransactionId") Long savingsTransactionId);

    @Query(NOTE_DATA_SELECT + "where n.shareAccount.id = :shareAccountId" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByShareAccountId(@Param("shareAccountId") Long shareAccountId);

    @Query(NOTE_DATA_SELECT + "where n.noteTypeId = :noteTypeId and n.entityIdentifier = :entityIdentifier" + NOTE_DATA_ORDER)
    List<NoteData> getNotesDataByNoteTypeIdAndEntityIdentifier(@Param("noteTypeId") Integer noteTypeId,
            @Param("entityIdentifier") String entityIdentifier);

    @Query(NOTE_DATA_SELECT + "where n.client.id = :clientId and n.noteTypeId = 100" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataByClientId(@Param("clientId") Long clientId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.group.id = :groupId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataByGroupId(@Param("groupId") Long groupId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.loan.id = :loanId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataByLoanId(@Param("loanId") Long loanId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.loanTransaction.id = :loanTransactionId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataByLoanTransactionId(@Param("loanTransactionId") Long loanTransactionId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.savingsAccount.id = :savingsAccountId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataBySavingsAccountId(@Param("savingsAccountId") Long savingsAccountId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.savingsTransaction.id = :savingsTransactionId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataBySavingsTransactionId(@Param("savingsTransactionId") Long savingsTransactionId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.shareAccount.id = :shareAccountId" + NOTE_DATA_ID_WHERE + NOTE_DATA_ORDER)
    NoteData getNoteDataByShareAccountId(@Param("shareAccountId") Long shareAccountId, @Param("id") Long id);

    @Query(NOTE_DATA_SELECT + "where n.noteTypeId = :noteTypeId and n.entityIdentifier = :entityIdentifier" + NOTE_DATA_ID_WHERE
            + NOTE_DATA_ORDER)
    NoteData getNoteDataByNoteTypeIdAndEntityIdentifier(@Param("noteTypeId") Integer noteTypeId,
            @Param("entityIdentifier") String entityIdentifier, @Param("id") Long id);
}
