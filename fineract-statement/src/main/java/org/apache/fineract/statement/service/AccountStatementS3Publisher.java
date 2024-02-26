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
package org.apache.fineract.statement.service;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.fineract.statement.domain.StatementPublishType.S3;
import static org.apache.fineract.statement.service.Camt053StatementGenerator.JSON_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.exception.ResourceNotFoundException;
import org.apache.fineract.portfolio.PortfolioProductType;
import org.apache.fineract.statement.data.dao.AccountStatementPublishData;
import org.apache.fineract.statement.domain.AccountStatementResult;
import org.apache.fineract.statement.domain.AccountStatementResultRepository;
import org.apache.fineract.statement.domain.StatementPublishType;
import org.apache.fineract.statement.domain.StatementType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AccountStatementS3Publisher implements AccountStatementPublisher {

    private final AccountStatementResultRepository statementResultRepository;
    private final FineractProperties properties;
    private final S3Client s3Client;

    @Override
    public boolean isSupport(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType) {
        return publishType == S3;
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Response publish(@NotNull PortfolioProductType productType, @NotNull StatementType statementType,
            @NotNull StatementPublishType publishType, List<AccountStatementPublishData> publishBatch) {
        Response.ResponseBuilder respBuilder = Response.status(NO_CONTENT);
        String bucket = properties.getReport().getExport().getS3().getBucketName();
        String filePfx = productType + "_" + statementType;
        String fileExt = ".json";
        FineractProperties.FineractStatementS3Properties s3 = properties.getStatement().getS3();
        String s3Folder = s3.getFolder();
        Integer nameLengt = s3.getLength();

        for (AccountStatementPublishData publishData : publishBatch) {
            Long resultId = publishData.getAccountStatementResultId();
            log.info("Start to publish statement result for id {}", resultId);
            AccountStatementResult result = statementResultRepository.findById(resultId)
                    .orElseThrow(() -> new ResourceNotFoundException("account.statement.result", resultId.toString()));
            String content = result.getContent();
            Map<String, String> metadata = buildMetadata(result.getMetadata());
            String resultPath = result.getResultPath();
            String folder = s3Folder;
            if (!StringUtils.isBlank(resultPath)) {
                folder = StringUtils.isBlank(folder) ? resultPath : (folder + File.separator + resultPath);
            }
            String fileName = result.getResultName();
            if (StringUtils.isBlank(fileName)) {
                fileName = filePfx + "_" + result.getGeneratedOn() + "_" + result.getResultCode().replaceAll("[^a-zA-Z0-9!\\-_.'()$]", "_")
                        + fileExt;
            }
            if (fileName.length() > nameLengt) {
                throw new IllegalArgumentException("The statement file name '" + fileName + "' must be shorter than " + nameLengt);
            }
            String filePath = StringUtils.isBlank(folder) ? fileName : (folder + File.separator + fileName);
            log.debug("Statement result is ready to publish to {} for id {}", filePath, resultId);
            s3Client.putObject(builder -> builder.bucket(bucket).key(filePath).metadata(metadata).build(), RequestBody.fromString(content));
            result.published();
            statementResultRepository.save(result);
            log.info("Statement result is published to {} for id {}", filePath, resultId);
        }
        return respBuilder.status(NO_CONTENT).build();
    }

    private static Map<String, String> buildMetadata(String metadatas) {
        Map<String, String> metadata = new HashMap<>();
        if (metadatas == null) {
            return metadata;
        }
        try {
            metadata = JSON_MAPPER.readValue(metadatas, new TypeReference<HashMap<String, List<String>>>() {}).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(",", e.getValue())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return metadata;
    }
}
