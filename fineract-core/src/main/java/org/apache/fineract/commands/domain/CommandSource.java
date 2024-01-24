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
package org.apache.fineract.commands.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.useradministration.domain.AppUser;

import java.time.OffsetDateTime;

@Entity
@Table(name = "m_portfolio_command_source")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommandSource extends AbstractPersistableCustom<Long> {

    @Column(name = "action_name", nullable = true, length = 100)
    private String actionName;

    @Column(name = "entity_name", nullable = true, length = 100)
    private String entityName;

    @Column(name = "office_id")
    private Long officeId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "savings_account_id")
    private Long savingsId;

    @Column(name = "api_get_url", length = 100)
    private String resourceGetUrl;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "subresource_id")
    private Long subResourceId;

    @Column(name = "command_as_json", length = 1000)
    private String commandAsJson;

    @ManyToOne
    @JoinColumn(name = "maker_id", nullable = false)
    private AppUser maker;

    /*
     * Deprecated: Columns and data left untouched to help migration.
     *
     * @Column(name = "made_on_date", nullable = false) private LocalDateTime madeOnDate;
     *
     * @Column(name = "checked_on_date", nullable = true) private LocalDateTime checkedOnDate;
     */

    @Column(name = "made_on_date_utc", nullable = false)
    private OffsetDateTime madeOnDate;

    @Column(name = "checked_on_date_utc")
    private OffsetDateTime checkedOnDate;

    @ManyToOne
    @JoinColumn(name = "checker_id", nullable = true)
    private AppUser checker;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "creditbureau_id")
    private Long creditBureauId;

    @Column(name = "organisation_creditbureau_id")
    private Long organisationCreditBureauId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "idempotency_key", length = 50)
    private String idempotencyKey;

    @Column(name = "resource_external_id")
    private ExternalId resourceExternalId;

    @Column(name = "subresource_external_id")
    private ExternalId subResourceExternalId;

    @Column(name = "result")
    private String result;

    @Column(name = "result_status_code")
    private Integer resultStatusCode;

    @Column(name = "resource_identifier")
    private String resourceIdentifier;

    public static CommandSource fullEntryFrom(final CommandWrapper wrapper, final JsonCommand command, final AppUser maker,
                                              String idempotencyKey, Integer status) {
        OffsetDateTime madeOnDate = DateUtils.getAuditOffsetDateTime();
        return new CommandSource(wrapper.actionName(), wrapper.entityName(), wrapper.getOfficeId(), command.getGroupId(),
                command.getClientId(), command.getLoanId(), command.getSavingsId(), wrapper.getHref(), command.entityId(),
                command.subentityId(), command.json(), maker, madeOnDate, null, null, status, command.getProductId(),
                command.getTransactionId(), command.getCreditBureauId(), command.getOrganisationCreditBureauId(), command.getJobName(),
                idempotencyKey, null, null, null, null, command.getResourceIdentifier());
    }

    public Long getCreditBureauId() {
        return this.creditBureauId;
    }

    public void setCreditBureauId(Long creditBureauId) {
        this.creditBureauId = creditBureauId;
    }

    public Long getOrganisationCreditBureauId() {
        return this.organisationCreditBureauId;
    }

    public void setOrganisationCreditBureauId(Long organisationCreditBureauId) {
        this.organisationCreditBureauId = organisationCreditBureauId;
    }

    public String getJobName() {
        return this.jobName;
    }

    public Long getResourceId() {
        return this.resourceId;
    }

    public void setResourceId(final Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getSubResourceId() {
        return this.subResourceId;
    }

    public void setSubResourceId(final Long subResourceId) {
        this.subResourceId = subResourceId;
    }

    public String getCommandJson() {
        return this.commandAsJson;
    }

    public void setCommandJson(final String json) {
        this.commandAsJson = json;
    }

    public AppUser getMaker() {
        return maker;
    }

    public AppUser getChecker() {
        return checker;
    }

    public String getActionName() {
        return this.actionName;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getPermissionCode() {
        return this.actionName + "_" + this.entityName;
    }

    public String getResourceGetUrl() {
        return this.resourceGetUrl;
    }

    public Long getProductId() {
        return this.productId;
    }

    /**
     * @return the clientId
     */
    public Long getClientId() {
        return clientId;
    }

    /**
     * @return the groupId
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * @return the loanId
     */
    public Long getLoanId() {
        return loanId;
    }

    /**
     * @return the officeId
     */
    public Long getOfficeId() {
        return officeId;
    }

    /**
     * @return the savingsId
     */
    public Long getSavingsId() {
        return savingsId;
    }

    /**
     * @return the transactionId
     */
    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setStatus(CommandProcessingResultType status) {
        setStatus(status == null ? null : status.getValue());
    }

    public Integer getResultStatusCode() {
        return resultStatusCode;
    }

    public void setResultStatusCode(Integer resultStatusCode) {
        this.resultStatusCode = resultStatusCode;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public void markAsAwaitingApproval() {
        this.status = CommandProcessingResultType.AWAITING_APPROVAL.getValue();
    }

    public boolean isMarkedAsAwaitingApproval() {
        return this.status.equals(CommandProcessingResultType.AWAITING_APPROVAL.getValue());
    }

    public void markAsChecked(final AppUser checker) {
        this.checker = checker;
        this.checkedOnDate = DateUtils.getAuditOffsetDateTime();
        this.status = CommandProcessingResultType.PROCESSED.getValue();
    }

    public void markAsRejected(final AppUser checker) {
        this.checker = checker;
        this.checkedOnDate = DateUtils.getAuditOffsetDateTime();
        this.status = CommandProcessingResultType.REJECTED.getValue();
    }

    public void updateForAudit(final CommandProcessingResult result) {
        this.officeId = result.getOfficeId();
        this.groupId = result.getGroupId();
        this.clientId = result.getClientId();
        this.loanId = result.getLoanId();
        this.savingsId = result.getSavingsId();
        this.productId = result.getProductId();
        this.transactionId = result.getTransactionId();
        this.resourceId = result.getResourceId();
        this.resourceExternalId = result.getResourceExternalId();
        this.subResourceId = result.getSubResourceId();
        this.subResourceExternalId = result.getSubResourceExternalId();
        this.resourceIdentifier = result.getResourceIdentifier();
    }
}
