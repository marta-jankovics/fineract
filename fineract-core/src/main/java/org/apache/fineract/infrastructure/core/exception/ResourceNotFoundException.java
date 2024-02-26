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
package org.apache.fineract.infrastructure.core.exception;

import org.springframework.beans.BeansException;

public class ResourceNotFoundException extends AbstractPlatformResourceNotFoundException {

    public ResourceNotFoundException(String resource, String defaultUserMessage, Object... defaultUserMessageArgs) {
        super(String.format("error.msg.%s.not.found", resource), String.format(defaultUserMessage, defaultUserMessageArgs),
                defaultUserMessageArgs);
    }

    public ResourceNotFoundException(String resource, String identifier) {
        this(resource, "Resource '%s' with id '%s' cannot be found.", resource, identifier);
    }

    public ResourceNotFoundException(BeansException e) {
        this("resource", "Resource does not exist.", e);
    }
}
