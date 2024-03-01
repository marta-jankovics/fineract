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
package org.apache.fineract.portfolio.note.service;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.note.domain.NoteType;

@RequiredArgsConstructor
public class NoteReadPlatformServiceImpl implements NoteReadPlatformService {

    private final NoteRepository noteRepository;

    @Override
    public NoteData retrieveNote(final Long noteId, final String resourceId, final NoteType noteType) {
        return switch (noteType) {
            case CLIENT -> noteRepository.getNoteDataByClientId(Long.valueOf(resourceId), noteId);
            case LOAN -> noteRepository.getNoteDataByLoanId(Long.valueOf(resourceId), noteId);
            case LOAN_TRANSACTION -> noteRepository.getNoteDataByLoanTransactionId(Long.valueOf(resourceId), noteId);
            case SAVING_ACCOUNT -> noteRepository.getNoteDataBySavingsAccountId(Long.valueOf(resourceId), noteId);
            case SAVINGS_TRANSACTION -> noteRepository.getNoteDataBySavingsTransactionId(Long.valueOf(resourceId), noteId);
            case GROUP -> noteRepository.getNoteDataByGroupId(Long.valueOf(resourceId), noteId);
            case SHARE_ACCOUNT -> noteRepository.getNoteDataByShareAccountId(Long.valueOf(resourceId), noteId);
            case CURRENT_ACCOUNT, CURRENT_TRANSACTION ->
                noteRepository.getNoteDataByNoteTypeIdAndEntityIdentifier(noteType.getValue(), resourceId, noteId);
        };
    }

    @Override
    public Collection<NoteData> retrieveNotesByResource(@NotNull String resourceId, @NotNull NoteType noteType) {
        return switch (noteType) {
            case CLIENT -> noteRepository.getNotesDataByClientId(Long.valueOf(resourceId));
            case LOAN -> noteRepository.getNotesDataByLoanId(Long.valueOf(resourceId));
            case LOAN_TRANSACTION -> noteRepository.getNotesDataByLoanTransactionId(Long.valueOf(resourceId));
            case SAVING_ACCOUNT -> noteRepository.getNotesDataBySavingsAccountId(Long.valueOf(resourceId));
            case SAVINGS_TRANSACTION -> noteRepository.getNotesDataBySavingsTransactionId(Long.valueOf(resourceId));
            case GROUP -> noteRepository.getNotesDataByGroupId(Long.valueOf(resourceId));
            case SHARE_ACCOUNT -> noteRepository.getNotesDataByShareAccountId(Long.valueOf(resourceId));
            case CURRENT_ACCOUNT, CURRENT_TRANSACTION ->
                noteRepository.getNotesDataByNoteTypeIdAndEntityIdentifier(noteType.getValue(), resourceId);
        };
    }
}
