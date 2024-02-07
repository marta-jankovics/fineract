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
package org.apache.fineract.currentaccount.handler.account.impl;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_ACCOUNT_ENTITY_NAME;
import static org.apache.fineract.infrastructure.configuration.api.ApiConstants.ACTION_WITHDRAWAL;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.domain.CommandActionContext;
import org.apache.fineract.currentaccount.handler.account.CurrentAccountWithdrawalCommandHandler;
import org.apache.fineract.currentaccount.service.transaction.write.CurrentTransactionWriteService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = CURRENT_ACCOUNT_ENTITY_NAME, action = ACTION_WITHDRAWAL)
@RequiredArgsConstructor
public class CurrentAccountWithdrawalCommandHandlerImpl implements CurrentAccountWithdrawalCommandHandler {

    private final CurrentTransactionWriteService writePlatformService;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {
        ThreadLocalContextUtil.setCommandContext(CommandActionContext.create(CURRENT_ACCOUNT_ENTITY_NAME, ACTION_WITHDRAWAL));
        return this.writePlatformService.withdrawal(command.getResourceIdentifier(), command, false);
    }
}
