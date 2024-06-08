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
package org.apache.fineract.infrastructure.core.data;

import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

/**
 * Represents the successful result of an REST API call that results in processing a command.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CommandProcessingResultBuilder {

    private Long commandId;
    private Long officeId;
    private Long groupId;
    private Long clientId;
    private Long loanId;
    private Long savingsId;
    private String resourceIdentifier;
    private Long resourceId;
    private Long subResourceId;
    private Long gsimId;
    private Long glimId;
    private String transactionId;
    private Map<String, Object> changes;
    private Map<String, Object> creditBureauReportData;
    private Long productId;
    private boolean rollbackTransaction = false;
    private ExternalId resourceExternalId;
    private ExternalId subResourceExternalId;

    public CommandProcessingResult build() {
        return CommandProcessingResult.fromDetails(this.commandId, this.officeId, this.groupId, this.clientId, this.loanId, this.savingsId,
                this.resourceIdentifier, this.resourceId, this.gsimId, this.glimId, this.creditBureauReportData, this.transactionId,
                this.changes, this.productId, this.rollbackTransaction, this.subResourceId, this.resourceExternalId,
                this.subResourceExternalId);
    }

    public static CommandProcessingResultBuilder fromResult(CommandProcessingResult result) {
        return new CommandProcessingResultBuilder(result.getCommandId(), result.getOfficeId(), result.getGroupId(), result.getClientId(),
                result.getLoanId(), result.getSavingsId(), result.getResourceIdentifier(), result.getResourceId(),
                result.getSubResourceId(), result.getGsimId(), result.getGlimId(), result.getTransactionId(), result.getChanges(),
                result.getCreditBureauReportData(), result.getProductId(),
                Optional.ofNullable(result.getRollbackTransaction()).orElse(false), result.getResourceExternalId(),
                result.getSubResourceExternalId());
    }

    public CommandProcessingResultBuilder withCommandId(final Long withCommandId) {
        this.commandId = withCommandId;
        return this;
    }

    public CommandProcessingResultBuilder with(final Map<String, Object> withChanges) {
        this.changes = withChanges;
        return this;
    }

    public CommandProcessingResultBuilder withResourceIdentifier(final String withResourceIdentifier) {
        this.resourceIdentifier = withResourceIdentifier;
        return this;
    }

    public CommandProcessingResultBuilder withEntityId(final Long withEntityId) {
        return withResourceId(withEntityId);
    }

    public CommandProcessingResultBuilder withResourceId(final Long resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public CommandProcessingResultBuilder withResource(final Object resource) {
        if (resource instanceof Long) {
            this.resourceId = (Long) resource;
        } else if (resource instanceof Integer) {
            this.resourceId = ((Integer) resource).longValue();
        } else if (resource != null) {
            this.resourceIdentifier = String.valueOf(resource);
        }
        return this;
    }

    public CommandProcessingResultBuilder withSubEntityId(final Long withSubEntityId) {
        return withSubResourceId(withSubEntityId);
    }

    public CommandProcessingResultBuilder withSubResourceId(final Long subResourceId) {
        this.subResourceId = subResourceId;
        return this;
    }

    public CommandProcessingResultBuilder withOfficeId(final Long withOfficeId) {
        this.officeId = withOfficeId;
        return this;
    }

    public CommandProcessingResultBuilder withClientId(final Long withClientId) {
        this.clientId = withClientId;
        return this;
    }

    public CommandProcessingResultBuilder withGroupId(final Long withGroupId) {
        this.groupId = withGroupId;
        return this;
    }

    public CommandProcessingResultBuilder withLoanId(final Long withLoanId) {
        this.loanId = withLoanId;
        return this;
    }

    public CommandProcessingResultBuilder withSavingsId(final Long withSavingsId) {
        this.savingsId = withSavingsId;
        return this;
    }

    public CommandProcessingResultBuilder withTransactionId(final String withTransactionId) {
        this.transactionId = withTransactionId;
        return this;
    }

    public CommandProcessingResultBuilder withProductId(final Long productId) {
        this.productId = productId;
        return this;
    }

    public CommandProcessingResultBuilder withGsimId(final Long gsimId) {
        this.gsimId = gsimId;
        return this;
    }

    public CommandProcessingResultBuilder withGlimId(final Long glimId) {
        this.glimId = glimId;
        return this;
    }

    public CommandProcessingResultBuilder withCreditReport(final Map<String, Object> withCreditReport) {
        this.creditBureauReportData = withCreditReport;
        return this;
    }

    public CommandProcessingResultBuilder setRollbackTransaction(final boolean rollbackTransaction) {
        this.rollbackTransaction |= rollbackTransaction;
        return this;
    }

    public CommandProcessingResultBuilder withEntityExternalId(final ExternalId entityExternalId) {
        return withResourceExternalId(entityExternalId);
    }

    public CommandProcessingResultBuilder withResourceExternalId(final ExternalId resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
        return this;
    }

    public CommandProcessingResultBuilder withSubEntityExternalId(final ExternalId subEntityExternalId) {
        return withSubResourceExternalId(subEntityExternalId);
    }

    public CommandProcessingResultBuilder withSubResourceExternalId(final ExternalId subResourceExternalId) {
        this.subResourceExternalId = subResourceExternalId;
        return this;
    }
}
