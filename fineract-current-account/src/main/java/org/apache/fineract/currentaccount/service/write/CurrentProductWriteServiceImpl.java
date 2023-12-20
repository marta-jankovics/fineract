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
package org.apache.fineract.currentaccount.service.write;

import static org.apache.fineract.currentaccount.api.CurrentAccountApiConstants.CURRENT_PRODUCT_RESOURCE_NAME;

import jakarta.persistence.PersistenceException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingWritePlatformService;
import org.apache.fineract.currentaccount.assembler.CurrentProductAssembler;
import org.apache.fineract.currentaccount.domain.CurrentProduct;
import org.apache.fineract.currentaccount.exception.CurrentProductNotFoundException;
import org.apache.fineract.currentaccount.repository.CurrentProductRepository;
import org.apache.fineract.currentaccount.validator.CurrentProductDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentProductWriteServiceImpl implements CurrentProductWriteService {

    private final PlatformSecurityContext context;
    private final CurrentProductRepository currentProductRepository;
    private final CurrentProductDataValidator currentProductDataValidator;
    private final CurrentProductAssembler currentProductAssembler;
    private final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService;

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {
        try {
            currentProductDataValidator.validateForCreate(command.json());

            final CurrentProduct product = currentProductAssembler.assemble(command);

            currentProductRepository.saveAndFlush(product);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e.getMostSpecificCause(), e);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            handleDataIntegrityIssues(command, ExceptionUtils.getRootCause(dve.getCause()), dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final Long productId, final JsonCommand command) {
        try {
            this.context.authenticatedUser();
            final CurrentProduct product = this.currentProductRepository.findById(productId)
                    .orElseThrow(() -> new CurrentProductNotFoundException(productId));

            this.currentProductDataValidator.validateForUpdate(command.json(), product);
            final Map<String, Object> changes = currentProductAssembler.update(product, command);

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .with(changes).build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e.getMostSpecificCause(), e);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            handleDataIntegrityIssues(command, ExceptionUtils.getRootCause(dve.getCause()), dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long productId) {

        this.context.authenticatedUser();
        if (!this.currentProductRepository.existsById(productId)) {
            throw new CurrentProductNotFoundException(productId);
        }
        this.currentProductRepository.deleteById(productId);
        return new CommandProcessingResultBuilder() //
                .withEntityId(productId) //
                .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dae) {
        String msgCode = "error.msg." + CURRENT_PRODUCT_RESOURCE_NAME;
        String msg = "Unknown data integrity issue with current product.";
        String param = null;
        Object[] msgArgs;
        Throwable checkEx = realCause == null ? dae : realCause;
        if (checkEx.getMessage().contains("m_current_product_name_key")) {
            final String name = command.stringValueOfParameterNamed("name");
            msgCode += ".duplicate.name";
            msg = "Current product with name `" + name + "` already exists";
            param = "name";
            msgArgs = new Object[] { name, dae };
        } else if (checkEx.getMessage().contains("m_current_product_short_name_key")) {
            final String shortName = command.stringValueOfParameterNamed("shortName");
            msgCode += ".duplicate.short.name";
            msg = "Current product with short name `" + shortName + "` already exists";
            param = "shortName";
            msgArgs = new Object[] { shortName, dae };
        } else {
            msgCode += ".unknown.data.integrity.issue";
            msgArgs = new Object[] { dae };
        }
        log.error("Error occurred.", dae);
        throw ErrorHandler.getMappable(dae, msgCode, msg, param, msgArgs);
    }
}
