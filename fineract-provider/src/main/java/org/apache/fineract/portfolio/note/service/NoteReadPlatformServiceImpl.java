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
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.note.data.NoteData;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.note.domain.NoteType;
import org.apache.fineract.portfolio.note.exception.NoteNotFoundException;

@RequiredArgsConstructor
public class NoteReadPlatformServiceImpl implements NoteReadPlatformService {

    private final NoteRepository noteRepository;

    @Override
    public NoteData retrieveNote(final Long noteId, final String resourceId, final NoteType noteType) {
        Optional<NoteData> note = switch (noteType) {
            case CLIENT -> noteRepository.findNoteDataByClientId(Long.valueOf(resourceId), noteId);
            case LOAN -> noteRepository.findNoteDataByLoanId(Long.valueOf(resourceId), noteId);
            case LOAN_TRANSACTION -> noteRepository.findNoteDataByLoanTransactionId(Long.valueOf(resourceId), noteId);
            case SAVING_ACCOUNT -> noteRepository.findNoteDataBySavingsAccountId(Long.valueOf(resourceId), noteId);
            case SAVINGS_TRANSACTION -> noteRepository.findNoteDataBySavingsTransactionId(Long.valueOf(resourceId), noteId);
            case GROUP -> noteRepository.findNoteDataByGroupId(Long.valueOf(resourceId), noteId);
            case SHARE_ACCOUNT -> noteRepository.findNoteDataByShareAccountId(Long.valueOf(resourceId), noteId);
        };
        return note.orElseThrow(() -> new NoteNotFoundException(noteId, resourceId, noteType.name().toLowerCase()));
    }

    @Override
    public List<NoteData> retrieveNotesByResource(@NotNull String resourceId, @NotNull NoteType noteType) {
        return switch (noteType) {
            case CLIENT -> noteRepository.getNotesDataByClientId(Long.valueOf(resourceId));
            case LOAN -> noteRepository.getNotesDataByLoanId(Long.valueOf(resourceId));
            case LOAN_TRANSACTION -> noteRepository.getNotesDataByLoanTransactionId(Long.valueOf(resourceId));
            case SAVING_ACCOUNT -> noteRepository.getNotesDataBySavingsAccountId(Long.valueOf(resourceId));
            case SAVINGS_TRANSACTION -> noteRepository.getNotesDataBySavingsTransactionId(Long.valueOf(resourceId));
            case GROUP -> noteRepository.getNotesDataByGroupId(Long.valueOf(resourceId));
            case SHARE_ACCOUNT -> noteRepository.getNotesDataByShareAccountId(Long.valueOf(resourceId));
        };
    }
}
