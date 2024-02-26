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
package org.apache.fineract.currentaccount.data.product;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.apache.fineract.currentaccount.data.accounting.GLAccountDetailsData;
import org.apache.fineract.infrastructure.core.data.GLStringEnumOptionData;
import org.apache.fineract.infrastructure.core.data.StringEnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.statement.data.dto.ProductStatementTemplateData;

@Data
public class CurrentProductTemplateResponseData implements Serializable {

    private final List<CurrencyData> currencyOptions;
    private final List<StringEnumOptionData> accountingTypeOptions;
    private final Map<String, List<GLStringEnumOptionData>> accountingOptions;
    private final Map<String, List<GLAccountDetailsData>> accountingMappingOptions;
    private final List<PaymentTypeData> paymentTypeOptions;
    private final String accountTypeForPaymentTypeMapping;
    private final ProductStatementTemplateData statement;
}
