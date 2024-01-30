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
package org.apache.fineract.currentaccount.api.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public final class CurrentAccountCommonApiResourceSwagger {

    private CurrentAccountCommonApiResourceSwagger() {}

    @Schema(description = "PostCommandResponse")
    public static final class PostCommandResponse {

        @Schema(example = "1")
        public Long clientId;
        @Schema(example = "7GGBmEwPEf6WgTchzDHnX")
        public Long resourceIdentifier;
        @Schema(example = "95174ff9-1a75-4d72-a413-6f9b1cb988b7")
        public String resourceExternalId;
        @Schema(example = "")
        public Map<String, Object> changes;

        private PostCommandResponse() {}
    }
}
