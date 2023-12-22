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

import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Created by Ergin (https://itecnote.com/tecnote/r-pagination-in-spring-data-jpa-limit-and-offset/)
 *
 * Modified by adamsaghy
 **/

@ToString
@EqualsAndHashCode
public class OffsetLimitPagedRequest implements Pageable, Serializable {

    @Serial
    private static final long serialVersionUID = -25822477129613575L;

    private final int limit;
    private final long offset;
    private final Sort sort;

    /**
     * Creates a new {@link OffsetLimitPagedRequest} with sort parameters applied.
     *
     * @param offset
     *            zero-based offset.
     * @param limit
     *            the size of the elements to be returned.
     * @param sort
     *            can be {@literal null}.
     */
    public OffsetLimitPagedRequest(long offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset index must not be less than zero!");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    /**
     * Creates a new {@link OffsetLimitPagedRequest} with sort parameters applied.
     *
     * @param offset
     *            zero-based offset.
     * @param limit
     *            the size of the elements to be returned.
     * @param direction
     *            the direction of the {@link Sort} to be specified, can be {@literal null}.
     * @param properties
     *            the properties to sort by, must not be {@literal null} or empty.
     */
    public OffsetLimitPagedRequest(long offset, int limit, Sort.Direction direction, String... properties) {
        this(offset, limit, Sort.by(direction, properties));
    }

    /**
     * Creates a new {@link OffsetLimitPagedRequest} with sort parameters applied.
     *
     * @param offset
     *            zero-based offset.
     * @param limit
     *            the size of the elements to be returned.
     */
    public OffsetLimitPagedRequest(long offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    @Override
    public boolean isPaged() {
        return Pageable.super.isPaged();
    }

    @Override
    public boolean isUnpaged() {
        return Pageable.super.isUnpaged();
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Sort getSortOr(Sort sort) {
        return Pageable.super.getSortOr(sort);
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPagedRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public OffsetLimitPagedRequest previous() {
        return hasPrevious() ? new OffsetLimitPagedRequest(getOffset() - getPageSize(), getPageSize(), getSort()) : this;
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPagedRequest(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        long offset = (long) pageNumber * getPageSize();
        return new OffsetLimitPagedRequest(offset, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }
}
