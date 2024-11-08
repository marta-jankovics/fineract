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
package org.apache.fineract.test.initializer.global;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.client.models.BusinessStep;
import org.apache.fineract.client.models.GetBusinessStepConfigResponse;
import org.apache.fineract.client.models.UpdateBusinessStepConfigRequest;
import org.apache.fineract.client.services.BusinessStepConfigurationApi;
import org.apache.fineract.test.helper.ErrorHelper;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@RequiredArgsConstructor
@Component
public class CobBusinessStepInitializerStep implements FineractGlobalInitializerStep {

    private static final String WORKFLOW_NAME_LOAN_CLOSE_OF_BUSINESS = "LOAN_CLOSE_OF_BUSINESS";
    private static final String BUSINESS_STEP_NAME_ACCRUAL_ACTIVITY_POSTING = "ACCRUAL_ACTIVITY_POSTING";
    private final BusinessStepConfigurationApi businessStepConfigurationApi;

    @Override
    public void initialize() throws Exception {
        // --- Adding ACCRUAL_ACTIVITY_POSTING to default COB steps ---
        Response<GetBusinessStepConfigResponse> businessStepConfigResponse = businessStepConfigurationApi
                .retrieveAllConfiguredBusinessStep(WORKFLOW_NAME_LOAN_CLOSE_OF_BUSINESS).execute();
        ErrorHelper.checkSuccessfulApiCall(businessStepConfigResponse);
        List<BusinessStep> businessSteps = businessStepConfigResponse.body().getBusinessSteps();
        businessSteps.sort(Comparator.comparingLong(BusinessStep::getOrder));
        Long lastOrder = businessSteps.get(businessSteps.size() - 1).getOrder();

        BusinessStep accrualActivityPosting = new BusinessStep().stepName(BUSINESS_STEP_NAME_ACCRUAL_ACTIVITY_POSTING).order(lastOrder + 1);
        businessSteps.add(accrualActivityPosting);

        UpdateBusinessStepConfigRequest request = new UpdateBusinessStepConfigRequest().businessSteps(businessSteps);

        Response<Void> response = businessStepConfigurationApi.updateJobBusinessStepConfig(WORKFLOW_NAME_LOAN_CLOSE_OF_BUSINESS, request)
                .execute();
        ErrorHelper.checkSuccessfulApiCall(response);
    }
}
