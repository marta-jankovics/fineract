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
package org.apache.fineract.currentaccount.service.product.write.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.currentaccount.assembler.product.CurrentProductAssembler;
import org.apache.fineract.currentaccount.domain.product.CurrentProduct;
import org.apache.fineract.currentaccount.repository.product.CurrentProductRepository;
import org.apache.fineract.currentaccount.service.product.write.CurrentProductWriteService;
import org.apache.fineract.currentaccount.validator.product.CurrentProductDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class CurrentProductWriteServiceImpl implements CurrentProductWriteService {

    private final CurrentProductRepository currentProductRepository;
    private final CurrentProductDataValidator currentProductDataValidator;
    private final CurrentProductAssembler currentProductAssembler;

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult create(final JsonCommand command) {
        currentProductDataValidator.validateForCreate(command);
        final CurrentProduct product = currentProductAssembler.assemble(command);

        return new CommandProcessingResultBuilder() //
                .withResourceIdentifier(product.getId()) //
                .withEntityExternalId(product.getExternalId()) //
                .build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult update(final String productId, final JsonCommand command) {
        final CurrentProduct product = this.currentProductRepository.findById(productId)
                .orElseThrow(() -> new PlatformResourceNotFoundException("current.product",
                        "Current product with provided id: %s cannot be found", productId));
        this.currentProductDataValidator.validateForUpdate(command, product);
        final Map<String, Object> changes = currentProductAssembler.update(product, command);

        return new CommandProcessingResultBuilder() //
                .withResourceIdentifier(product.getId()) //
                .withEntityExternalId(product.getExternalId()) //
                .with(changes).build();
    }

    @Transactional(timeout = 3)
    @Override
    public CommandProcessingResult delete(final String productId) {
        if (!this.currentProductRepository.existsById(productId)) {
            throw new PlatformResourceNotFoundException("current.product", "Current product with provided id: %s cannot be found",
                    productId);
        }
        this.currentProductRepository.deleteById(productId);
        return new CommandProcessingResultBuilder() //
                .withResourceIdentifier(productId) //
                .build();
    }
}
