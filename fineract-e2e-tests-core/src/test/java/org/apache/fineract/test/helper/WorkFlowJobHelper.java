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
package org.apache.fineract.test.helper;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.client.models.BusinessStep;
import org.apache.fineract.client.models.GetBusinessStepConfigResponse;
import org.apache.fineract.client.services.BusinessStepConfigurationApi;
import org.apache.fineract.test.support.TestContext;
import org.apache.fineract.test.support.TestContextKey;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@RequiredArgsConstructor
@Component
public class WorkFlowJobHelper {

    private static final String WORKFLOW_NAME_LOAN_CLOSE_OF_BUSINESS = "LOAN_CLOSE_OF_BUSINESS";

    private final BusinessStepConfigurationApi businessStepConfigurationApi;

    public void saveOriginalCOBWorkflowJobBusinessStepList() throws IOException {
        Response<GetBusinessStepConfigResponse> businessStepConfigResponse = businessStepConfigurationApi
                .retrieveAllConfiguredBusinessStep(WORKFLOW_NAME_LOAN_CLOSE_OF_BUSINESS).execute();
        ErrorHelper.checkSuccessfulApiCall(businessStepConfigResponse);
        List<BusinessStep> businessSteps = businessStepConfigResponse.body().getBusinessSteps();
        businessSteps.sort(Comparator.comparingLong(BusinessStep::getOrder));
        TestContext.GLOBAL.set(TestContextKey.ORIGINAL_COB_WORKFLOW_JOB_BUSINESS_STEP_LIST, businessSteps);
    }
}
