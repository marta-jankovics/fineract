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
package org.apache.fineract.portfolio.statement.data;

import com.google.common.base.Enums;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.portfolio.statement.domain.StatementBatchType;
import org.apache.fineract.portfolio.statement.domain.StatementPublishType;
import org.apache.fineract.portfolio.statement.domain.StatementType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class StatementParser {

    public static final String PRODUCT_STATEMENT_RESOURCE = "productStatement";
    public static final String ACCOUNT_STATEMENT_RESOURCE = "accountStatement";

    public static final String PARAM_STATEMENTS = "statements";
    public static final String PARAM_PRODUCT_ID = "productId";
    public static final String PARAM_PRODUCT_TYPE = "productType";
    public static final String PARAM_STATEMENT_CODE = "statementCode";
    public static final String PARAM_STATEMENT_TYPE = "statementType";
    public static final String PARAM_PUBLISH_TYPE = "publishType";
    public static final String PARAM_BATCH_TYPE = "batchType";
    public static final String PARAM_RECURRENCE = "recurrence";
    public static final String PARAM_ACCOUNT_ID = "accountId";
    public static final String PARAM_SEQUENCE_PREFIX = "sequencePrefix";
    public static final String PARAM_LOCALE = "locale";

    private static final Set<String> PRODUCT_STATEMENT_CREATE_PARAMETERS = Set.of(PARAM_STATEMENT_CODE, PARAM_STATEMENT_TYPE,
            PARAM_PUBLISH_TYPE, PARAM_BATCH_TYPE, PARAM_RECURRENCE, PARAM_SEQUENCE_PREFIX, PARAM_LOCALE);

    private static final Set<String> PRODUCT_STATEMENT_UPDATE_PARAMETERS = Set.of(PARAM_STATEMENT_CODE, PARAM_STATEMENT_TYPE,
            PARAM_PUBLISH_TYPE, PARAM_BATCH_TYPE, PARAM_RECURRENCE, PARAM_SEQUENCE_PREFIX, PARAM_LOCALE);

    private static final Set<String> ACCOUNT_STATEMENT_CREATE_PARAMETERS = Set.of(PARAM_STATEMENT_CODE, PARAM_RECURRENCE,
            PARAM_SEQUENCE_PREFIX, PARAM_LOCALE);

    private static final Set<String> ACCOUNT_STATEMENT_UPDATE_PARAMETERS = Set.of(PARAM_STATEMENT_CODE, PARAM_RECURRENCE,
            PARAM_SEQUENCE_PREFIX, PARAM_LOCALE);

    private final FromJsonHelper fromJsonHelper;

    @Autowired
    public StatementParser(FromJsonHelper fromJsonHelper) {
        this.fromJsonHelper = fromJsonHelper;
    }

    @NotNull
    public ProductStatementData parseProductStatementForCreate(String json, Long productId, PortfolioProductType productType) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final JsonObject element = fromJsonHelper.parse(json).getAsJsonObject();
        return parseProductStatementForCreate(element, productId, productType);
    }

    @NotNull
    public ProductStatementData parseProductStatementForCreate(JsonObject element, Long productId, PortfolioProductType productType) {
        fromJsonHelper.checkForUnsupportedParameters(element, PRODUCT_STATEMENT_CREATE_PARAMETERS);

        final DataValidatorBuilder validator = new DataValidatorBuilder(new ArrayList<>()).resource(PRODUCT_STATEMENT_RESOURCE);

        validator.parameter(PARAM_PRODUCT_ID).value(productId).notBlank();
        validator.parameter(PARAM_PRODUCT_TYPE).value(productType).notNull();

        final String code = parseStatementCode(element, validator);
        final StatementType statementType = parseStatementType(element, validator);
        final StatementPublishType publishType = parsePublishType(element, validator);
        final StatementBatchType batchType = parseBatchType(element, validator);

        final String recurrence = parseRecurrence(element, validator);
        final String prefix = parseSequencePrefix(element, validator);

        validator.throwValidationErrors();

        return new ProductStatementData(productId, productType, code, statementType, publishType, batchType, recurrence, prefix);
    }

    public ProductStatementData parseProductStatementForUpdate(String json, Long productId, PortfolioProductType productType) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final JsonElement element = fromJsonHelper.parse(json);
        return parseProductStatementForUpdate(element, productId, productType);
    }

    public ProductStatementData parseProductStatementForUpdate(JsonElement element, Long productId, PortfolioProductType productType) {
        fromJsonHelper.checkForUnsupportedParameters(element.getAsJsonObject(), PRODUCT_STATEMENT_UPDATE_PARAMETERS);

        final DataValidatorBuilder validator = new DataValidatorBuilder(new ArrayList<>()).resource(PRODUCT_STATEMENT_RESOURCE);

        String code = parseStatementCode(element, validator);
        StatementType statementType = null;
        if (fromJsonHelper.parameterExists(PARAM_STATEMENT_TYPE, element)) {
            statementType = parseStatementType(element, validator);
        }
        StatementPublishType publishType = null;
        if (fromJsonHelper.parameterExists(PARAM_PUBLISH_TYPE, element)) {
            publishType = parsePublishType(element, validator);
        }
        StatementBatchType batchType = null;
        if (fromJsonHelper.parameterExists(PARAM_BATCH_TYPE, element)) {
            batchType = parseBatchType(element, validator);
        }
        String recurrence = null;
        if (fromJsonHelper.parameterExists(PARAM_BATCH_TYPE, element)) {
            recurrence = parseRecurrence(element, validator);
        }
        String prefix = null;
        if (fromJsonHelper.parameterExists(PARAM_SEQUENCE_PREFIX, element)) {
            prefix = parseSequencePrefix(element, validator);
        }

        validator.throwValidationErrors();

        return new ProductStatementData(productId, productType, code, statementType, publishType, batchType, recurrence, prefix);
    }

    public AccountStatementData parseAccountStatementForCreate(String json, Long accountId) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final JsonObject element = fromJsonHelper.parse(json).getAsJsonObject();
        return parseAccountStatementForCreate(element, accountId);
    }

    public AccountStatementData parseAccountStatementForCreate(JsonObject element, Long accountId) {
        fromJsonHelper.checkForUnsupportedParameters(element.getAsJsonObject(), ACCOUNT_STATEMENT_CREATE_PARAMETERS);

        final DataValidatorBuilder validator = new DataValidatorBuilder(new ArrayList<>()).resource(ACCOUNT_STATEMENT_RESOURCE);

        validator.parameter(PARAM_ACCOUNT_ID).value(accountId).notBlank();

        final String code = parseStatementCode(element, validator);
        final String recurrence = parseRecurrence(element, validator);
        final String prefix = parseSequencePrefix(element, validator);

        validator.throwValidationErrors();

        return new AccountStatementData(accountId, code, recurrence, prefix);
    }

    public AccountStatementData parseAccountStatementForUpdate(String json, Long accountId) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }
        final JsonObject element = fromJsonHelper.parse(json).getAsJsonObject();
        return parseAccountStatementForUpdate(element, accountId);
    }

    public AccountStatementData parseAccountStatementForUpdate(JsonObject element, Long accountId) {
        fromJsonHelper.checkForUnsupportedParameters(element.getAsJsonObject(), ACCOUNT_STATEMENT_UPDATE_PARAMETERS);

        final DataValidatorBuilder validator = new DataValidatorBuilder(new ArrayList<>()).resource(ACCOUNT_STATEMENT_RESOURCE);

        String code = null;
        if (fromJsonHelper.parameterExists(PARAM_STATEMENT_CODE, element)) {
            code = parseStatementCode(element, validator);
        }
        String recurrence = null;
        if (fromJsonHelper.parameterExists(PARAM_BATCH_TYPE, element)) {
            recurrence = parseRecurrence(element, validator);
        }
        String prefix = null;
        if (fromJsonHelper.parameterExists(PARAM_SEQUENCE_PREFIX, element)) {
            prefix = parseSequencePrefix(element, validator);
        }

        validator.throwValidationErrors();

        return new AccountStatementData(accountId, code, recurrence, prefix);
    }

    private String parseStatementCode(JsonElement element, DataValidatorBuilder validator) {
        final String code = fromJsonHelper.extractStringNamed(PARAM_STATEMENT_CODE, element);
        validator.parameter(PARAM_STATEMENT_CODE).value(code).notBlank();
        return code;
    }

    @Nullable
    private StatementType parseStatementType(JsonElement element, DataValidatorBuilder validator) {
        StatementType statementType = null;
        final String statementTypeS = fromJsonHelper.extractStringNamed(PARAM_STATEMENT_TYPE, element);
        if (statementTypeS != null && (statementType = Enums.getIfPresent(StatementType.class, statementTypeS).orNull()) == null) {
            validator.reset().parameter(PARAM_STATEMENT_TYPE).value(statementTypeS).notBlank().failWithCode("invalid");
        }
        return statementType;
    }

    @Nullable
    private StatementPublishType parsePublishType(JsonElement element, DataValidatorBuilder validator) {
        StatementPublishType publishType = null;
        final String publishTypeS = fromJsonHelper.extractStringNamed(PARAM_PUBLISH_TYPE, element);
        if (publishTypeS != null && (publishType = Enums.getIfPresent(StatementPublishType.class, publishTypeS).orNull()) == null) {
            validator.reset().parameter(PARAM_PUBLISH_TYPE).value(publishTypeS).notBlank().failWithCode("invalid");
        }
        return publishType;
    }

    @Nullable
    private StatementBatchType parseBatchType(JsonElement element, DataValidatorBuilder validator) {
        StatementBatchType batchType = null;
        final String batchTypeS = fromJsonHelper.extractStringNamed(PARAM_BATCH_TYPE, element);
        if (batchTypeS != null && (batchType = Enums.getIfPresent(StatementBatchType.class, batchTypeS).orNull()) == null) {
            validator.reset().parameter(PARAM_BATCH_TYPE).value(batchTypeS).notBlank().failWithCode("invalid");
        }
        return batchType;
    }

    private String parseRecurrence(JsonElement element, DataValidatorBuilder validator) {
        String recurrence = fromJsonHelper.extractStringNamed(PARAM_RECURRENCE, element);
        validator.reset().parameter(PARAM_RECURRENCE).value(recurrence).ignoreIfNull().notBlank().isValidRecurringRule(recurrence);
        return recurrence;
    }

    private String parseSequencePrefix(JsonElement element, DataValidatorBuilder validator) {
        String prefix = fromJsonHelper.extractStringNamed(PARAM_SEQUENCE_PREFIX, element);
        validator.reset().parameter(PARAM_SEQUENCE_PREFIX).value(prefix).ignoreIfNull().notBlank();
        return prefix;
    }
}
