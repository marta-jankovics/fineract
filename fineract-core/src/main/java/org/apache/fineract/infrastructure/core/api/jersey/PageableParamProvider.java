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
package org.apache.fineract.infrastructure.core.api.jersey;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableParamProvider implements ValueParamProvider {

    @Override
    public Function<ContainerRequest, ?> getValueProvider(Parameter parameter) {

        if (parameter.getRawType() == Pageable.class && parameter.isAnnotationPresent(Pagination.class)) {
            return new PageableFunction(parameter);
        }
        return null;
    }

    @Override
    public PriorityType getPriority() {
        // Use HIGH otherwise it might not be used
        return Priority.HIGH;
    }

    private record PageableFunction(Parameter param) implements Function<ContainerRequest, Pageable> {

        @Override
        public Pageable apply(ContainerRequest request) {
            MultivaluedMap<String, String> queryParameters = request.getUriInfo().getQueryParameters();
            List<Sort.Order> sortingOrders = new ArrayList<>();
            AtomicInteger page = new AtomicInteger(0);
            AtomicInteger size = new AtomicInteger(50);
            AtomicReference<List<String>> sort = new AtomicReference<>();
            queryParameters.forEach((key, list) -> {
                switch (key) {
                    case "page" -> page.set(Integer.parseInt(list.get(0)));
                    case "size" -> size.set(Integer.parseInt(list.get(0)));
                    case "sort" -> sort.set(list);
                    default -> {
                    }
                }
            });
            if (sort.get() != null) {
                for (String propOrder : sort.get()) {
                    String[] propOrderSplit = propOrder.split(",");
                    String property = propOrderSplit[0];
                    if (propOrderSplit.length == 1) {
                        sortingOrders.add(Sort.Order.by(property));
                    } else {
                        Sort.Direction direction = Sort.Direction.fromString(propOrderSplit[1]);
                        sortingOrders.add(new Sort.Order(direction, property));
                    }
                }
            }

            return PageRequest.of(page.get(), size.get(), Sort.by(sortingOrders));
        }
    }
}
