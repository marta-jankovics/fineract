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
package org.apache.fineract.infrastructure.dataqueries.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.PersistenceException;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.domain.EntityDatatableChecks;
import org.apache.fineract.infrastructure.dataqueries.domain.EntityDatatableChecksRepository;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableEntryRequiredException;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.exception.EntityDatatableCheckAlreadyExistsException;
import org.apache.fineract.infrastructure.dataqueries.exception.EntityDatatableCheckNotSupportedException;
import org.apache.fineract.infrastructure.dataqueries.exception.EntityDatatableChecksNotFoundException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class EntityDatatableChecksWritePlatformServiceImpl implements EntityDatatableChecksWritePlatformService {

    private final PlatformSecurityContext context;
    private final EntityDatatableChecksDataValidator fromApiJsonDeserializer;
    private final EntityDatatableChecksRepository entityDatatableChecksRepository;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final SavingsProductReadPlatformService savingsProductReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationDomainService configurationDomainService;

    @Transactional
    @Override
    public CommandProcessingResult createCheck(final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            // check if the datatable is linked to the entity

            String datatableName = command.stringValueOfParameterNamed("datatableName");
            DatatableData datatableData = this.readWriteNonCoreDataService.retrieveDatatable(datatableName);

            if (datatableData == null) {
                throw new DatatableNotFoundException(datatableName);
            }

            final String entity = command.stringValueOfParameterNamed("entity");
            final String foreignKeyColumnName = EntityTables.getForeignKeyColumnNameOnDatatable(entity);
            final boolean columnExist = datatableData.hasColumn(foreignKeyColumnName);

            log.debug("{} has column {} ? {}", datatableData.getRegisteredTableName(), foreignKeyColumnName, columnExist);

            if (!columnExist) {
                throw new EntityDatatableCheckNotSupportedException(datatableData.getRegisteredTableName(), entity);
            }

            final Long productId = command.longValueOfParameterNamed("productId");
            final Integer status = command.integerValueOfParameterNamed("status");

            List<EntityDatatableChecks> entityDatatableCheck;
            if (productId == null) {
                entityDatatableCheck = this.entityDatatableChecksRepository.findByEntityStatusAndDatatableIdAndNoProduct(entity, status,
                        datatableName);
                if (!entityDatatableCheck.isEmpty()) {
                    throw new EntityDatatableCheckAlreadyExistsException(entity, status, datatableName);
                }
            } else {
                EntityTables entityTable = EntityTables.fromEntityName(entity);
                if (entityTable == EntityTables.LOAN) {
                    // if invalid loan product id, throws exception
                    this.loanProductReadPlatformService.retrieveLoanProduct(productId);
                } else if (entityTable == EntityTables.SAVINGS) {
                    // if invalid savings product id, throws exception
                    this.savingsProductReadPlatformService.retrieveOne(productId);
                } else {
                    throw new EntityDatatableCheckNotSupportedException(entity, productId);
                }
                entityDatatableCheck = this.entityDatatableChecksRepository.findByEntityStatusAndDatatableIdAndProductId(entity, status,
                        datatableName, productId);
                if (!entityDatatableCheck.isEmpty()) {
                    throw new EntityDatatableCheckAlreadyExistsException(entity, status, datatableName, productId);
                }
            }

            final EntityDatatableChecks check = EntityDatatableChecks.fromJson(command);
            this.entityDatatableChecksRepository.saveAndFlush(check);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withResourceId(check.getId()) //
                    .build();
        } catch (final DataAccessException e) {
            handleReportDataIntegrityIssues(command, e.getMostSpecificCause(), e);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            handleReportDataIntegrityIssues(command, ExceptionUtils.getRootCause(dve.getCause()), dve);
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public void runTheCheck(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId,
            String entitySubtype) {
        Integer statusId = status.getValue();
        String entityName = entity.getName();
        List<EntityDatatableChecks> datatableChecks;
        if (entitySubtype == null) {
            datatableChecks = entityDatatableChecksRepository.findByEntityAndStatus(entityName, statusId);
        } else {
            datatableChecks = entityDatatableChecksRepository.findByEntityAndStatusAndSubtype(entityName, statusId,
                    entitySubtype.toUpperCase());
        }

        if (datatableChecks != null) {
            for (EntityDatatableChecks datatableCheck : datatableChecks) {
                final String datatable = datatableCheck.getDatatableName();
                final Long countEntries = readWriteNonCoreDataService.countDatatableEntries(datatable, entityId, entity);

                log.debug("The are {} entries in the table {}", countEntries, datatable);
                if (countEntries.intValue() == 0) {
                    throw new DatatableEntryRequiredException(datatable);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void runTheCheckForProduct(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId,
            @NotNull Long productId) {
        Integer statusId = status.getValue();
        String entityName = entity.getName();

        List<EntityDatatableChecks> datatableChecks = entityDatatableChecksRepository.findByEntityStatusAndProduct(entityName, statusId,
                productId);
        if (datatableChecks == null || datatableChecks.isEmpty()) {
            datatableChecks = entityDatatableChecksRepository.findByEntityStatusAndNoProduct(entityName, statusId);
        }
        if (datatableChecks != null) {
            for (EntityDatatableChecks datatableCheck : datatableChecks) {
                final String datatable = datatableCheck.getDatatableName();
                final Long countEntries = readWriteNonCoreDataService.countDatatableEntries(datatable, entityId, entity);

                log.debug("The are {} entries in the table {}", countEntries, datatable);
                if (countEntries.intValue() == 0) {
                    throw new DatatableEntryRequiredException(datatable);
                }
            }
        }
    }

    @Transactional
    @Override
    public boolean saveDatatables(@NotNull StatusEnum status, @NotNull EntityTables entity, @NotNull Serializable entityId, Long productId,
            JsonArray elements) {
        final AppUser user = this.context.authenticatedUser();
        boolean isMakerCheckerEnabled = false;
        if (elements != null) {
            for (JsonElement element : elements) {
                final String datatable = this.fromApiJsonHelper.extractStringNamed("registeredTableName", element);
                final JsonObject datatableData = this.fromApiJsonHelper.extractJsonObjectNamed("data", element);

                if (datatable == null || datatableData == null) {
                    final ApiParameterError error = ApiParameterError.generalError(
                            "registeredTableName.and.data.parameters.must.be.present.in.each.list.items.in.datatables",
                            "registeredTableName and data parameters must be present in each list items in datatables");
                    List<ApiParameterError> errors = new ArrayList<>();
                    errors.add(error);
                    throw new PlatformApiDataValidationException(errors);
                }
                final String taskPermissionName = "CREATE_" + datatable;
                user.validateHasPermissionTo(taskPermissionName);
                if (this.configurationDomainService.isMakerCheckerEnabledForTask(taskPermissionName)) {
                    isMakerCheckerEnabled = true;
                }
                try {
                    this.readWriteNonCoreDataService.createNewDatatableEntry(datatable, entityId, datatableData.toString());
                } catch (PlatformApiDataValidationException e) {
                    for (ApiParameterError error : e.getErrors()) {
                        error.setParameterName("datatables." + datatable + "." + error.getParameterName());
                    }
                    throw e;
                }
            }
        }
        return isMakerCheckerEnabled;
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCheck(final Long entityDatatableCheckId) {
        final EntityDatatableChecks check = this.entityDatatableChecksRepository.findById(entityDatatableCheckId)
                .orElseThrow(() -> new EntityDatatableChecksNotFoundException(entityDatatableCheckId));

        this.entityDatatableChecksRepository.delete(check);

        return new CommandProcessingResultBuilder() //
                .withResourceId(entityDatatableCheckId) //
                .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleReportDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dae) {
        String msgCode = "error.msg.entityDatatableCheck";
        Throwable checkEx = realCause == null ? dae : realCause;
        String msg = "Unknown data integrity issue with resource: " + checkEx.getMessage();
        String param = null;
        Object[] msgArgs;
        if (checkEx.getMessage().contains("FOREIGN KEY (x_registered_table_name)")) {
            final String datatableName = command.stringValueOfParameterNamed("datatableName");
            msgCode += ".foreign.key.constraint";
            msg = "Datatable with name '" + datatableName + "' does not exist";
            param = "datatableName";
            msgArgs = new Object[] { datatableName, dae };
        } else if (checkEx.getMessage().contains("unique_entity_check")) {
            final String datatableName = command.stringValueOfParameterNamed("datatableName");
            final Integer status = command.integerValueOfParameterNamed("status");
            final String entity = command.stringValueOfParameterNamed("entity");
            final Long productId = command.longValueOfParameterNamed("productId");
            throw new EntityDatatableCheckAlreadyExistsException(entity, status, datatableName, productId);
        } else {
            msgCode += ".unknown.data.integrity.issue";
            msgArgs = new Object[] { dae };
        }
        log.error("Error occured.", dae);
        throw ErrorHandler.getMappable(dae, msgCode, msg, param, msgArgs);
    }
}
