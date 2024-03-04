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
package org.apache.fineract.currentaccount.api.account;

import java.util.List;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.note.data.NoteData;

public interface CurrentAccountNotesApi {

    NoteData retrieveNoteByIdentifier(String identifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifier(String idType, String identifier, Long noteId);

    NoteData retrieveNoteByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier, Long noteId);

    List<NoteData> retrieveNotesByIdentifier(String identifier);

    List<NoteData> retrieveNotesByIdTypeIdentifier(String idType, String identifier);

    List<NoteData> retrieveNotesByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier);

    CommandProcessingResult createNoteByIdentifier(String identifier, String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifier(String idType, String identifier, String requestJson);

    CommandProcessingResult createNoteByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier,
            String requestJson);

    CommandProcessingResult updateNoteByIdentifier(String identifier, Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifier(String idType, String identifier, Long noteId, String requestJson);

    CommandProcessingResult updateNoteByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier, Long noteId,
            String requestJson);

    CommandProcessingResult deleteNoteByIdentifier(String identifier, Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifier(String idType, String identifier, Long noteId);

    CommandProcessingResult deleteNoteByIdTypeIdentifierSubIdentifier(String idType, String identifier, String subIdentifier, Long noteId);

}
