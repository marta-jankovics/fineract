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
package org.apache.fineract.currentaccount.api.transaction;

import java.util.List;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.note.data.NoteData;

public interface CurrentTransactionNotesApi {

    NoteData retrieveNoteByIdentifier(String accountIdentifier, String transactionIdentifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdentifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier, String accountSubIdentifier,
            String transactionIdentifier, Long noteId);

    NoteData retrieveNoteByIdentifier(String accountIdentifier, String transactionIdType, String transactionIdentifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdType,
            String transactionIdentifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier, String accountSubIdentifier,
            String transactionIdType, String transactionIdentifier, Long noteId);

    List<NoteData> retrieveNotesByIdentifier(String accountIdentifier, String transactionIdentifier);

    List<NoteData> retrieveNotesByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdentifier);

    List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier, String accountSubIdentifier,
            String transactionIdentifier);

    List<NoteData> retrieveNotesByIdentifier(String accountIdentifier, String transactionIdType, String transactionIdentifier);

    List<NoteData> retrieveNotesByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdType,
            String transactionIdentifier);

    List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier, String accountSubIdentifier,
            String transactionIdType, String transactionIdentifier);

    CommandProcessingResult createNoteByIdentifier(String accountIdentifier, String transactionIdentifier, String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdentifier,
            String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String transactionIdentifier, String requestJson);

    CommandProcessingResult createNoteByIdentifier(String accountIdentifier, String transactionIdType, String transactionIdentifier,
            String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdType,
            String transactionIdentifier, String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String transactionIdType, String transactionIdentifier, String requestJson);

    CommandProcessingResult updateNoteByIdentifier(String accountIdentifier, String transactionIdentifier, Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdentifier,
            Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String transactionIdentifier, String accountSubIdentifier, Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdentifier(String accountIdentifier, String transactionIdType, String transactionIdentifier,
            Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdType,
            String transactionIdentifier, Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String transactionIdType, String transactionIdentifier, Long noteId, String requestJson);

    CommandProcessingResult deleteNoteByIdentifier(String accountIdentifier, String transactionIdentifier, Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdentifier,
            Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String transactionIdentifier, Long noteId);

    CommandProcessingResult deleteNoteByIdentifier(String accountIdentifier, String transactionIdType, String transactionIdentifier,
            Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifier(String accountIdType, String accountIdentifier, String transactionIdType,
            String transactionIdentifier, Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(String accountIdType, String accountIdentifier,
            String accountSubIdentifier, String transactionIdType, String transactionIdentifier, Long noteId);

}
