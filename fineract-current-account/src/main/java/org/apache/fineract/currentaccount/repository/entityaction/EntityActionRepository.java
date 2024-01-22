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
package org.apache.fineract.currentaccount.repository.entityaction;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.apache.fineract.currentaccount.domain.account.EntityAction;
import org.apache.fineract.currentaccount.enumeration.account.EntityActionType;
import org.apache.fineract.currentaccount.enumeration.account.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityActionRepository extends JpaRepository<EntityAction, Long> {

    @Query("SELECT ea.actionDate FROM EntityAction ea WHERE ea.entityType = :entityType AND ea.entityId = :entityId AND ea.actionType = :actionType")
    Optional<LocalDate> getActionDateByEntityTypeAndEntityIdAndActionType(@Param("entityType") EntityType entityType,
            @Param("entityId") UUID entityId, @Param("actionType") EntityActionType actionType);
}
