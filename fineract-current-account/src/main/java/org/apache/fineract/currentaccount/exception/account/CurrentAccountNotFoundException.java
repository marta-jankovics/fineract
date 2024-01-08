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
package org.apache.fineract.currentaccount.exception.account;

import java.util.UUID;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;

public class CurrentAccountNotFoundException extends AbstractPlatformResourceNotFoundException {

    public CurrentAccountNotFoundException(final UUID id) {
        super("error.msg.currentaccount.id.invalid", "Current account with identifier " + id + " does not exist", id);
    }

    public CurrentAccountNotFoundException(final ExternalId externalId) {
        super("error.msg.currentaccount.external.id.invalid",
                "Current account with external identifier " + externalId.getValue() + " does not exist");
    }

    public CurrentAccountNotFoundException(UUID id, EmptyResultDataAccessException e) {
        super("error.msg.currentaccount.id.invalid", "Current account with identifier " + id + " does not exist", id, e);
    }
}
