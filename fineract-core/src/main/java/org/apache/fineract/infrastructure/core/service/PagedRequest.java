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
package org.apache.fineract.infrastructure.core.service;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class PagedRequest<T> {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 10000;

    private T request;

    private int page;
    private int size = DEFAULT_PAGE_SIZE;

    private final List<SortOrder> sorts = new ArrayList<>();

    public static Pageable createFrom(Pageable pageable) {
        int page = ObjectUtils.defaultIfNull(pageable.getPageNumber(), 0);
        int size = Integer.min(ObjectUtils.defaultIfNull(pageable.getPageSize(), DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);
        return PageRequest.of(page, size, pageable.getSort());
    }

    public Optional<T> getRequest() {
        return Optional.ofNullable(request);
    }

    public PageRequest toPageable() {
        if (isEmpty(sorts)) {
            return PageRequest.of(page, size);
        } else {
            List<Sort.Order> orders = sorts.stream().map(SortOrder::toOrder).toList();
            return PageRequest.of(page, size, Sort.by(orders));
        }
    }

    @Data
    @SuppressWarnings({ "unused" })
    private static final class SortOrder {

        private Sort.Direction direction;
        private String property;

        private Sort.Order toOrder() {
            return new Sort.Order(direction, property);
        }
    }
}
