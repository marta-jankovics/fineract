/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
package org.apache.fineract.infrastructure.eclipselink;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

/**
 * Default String field value to JDBC data type converter.
 */
public class EmptyStringIfNullConverter implements Converter {

    /**
     * Creates an instance of default UUID field value to JDBC data type converter.
     */
    public EmptyStringIfNullConverter() {
    }

    /**
     * Converts String field value to non-null String.
     * @param value source Object field value
     * @param session current database session
     * @return target String to be stored as JDBC VARCHAR
     */
    @Override
    public Object convertObjectValueToDataValue(Object value, Session session) {
        return value == null ? "" : value;
    }

    /**
     * Converts String from JDBC VARCHAR parameter to String field value.
     * @param jdbcValue source String from JDBC VARCHAR
     * @param session current database session
     * @return target UUID field value
     */
    @Override
    public Object convertDataValueToObjectValue(Object jdbcValue, Session session) {
        return ((String) jdbcValue).isEmpty() ? null : jdbcValue;
    }

    /**
     * UUID values and String are immutable.
     * @return value of {@code false}
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Initialize mapping for JDBC data type.
     * @param mapping field database mapping
     * @param session current database session
     */
    @Override
    public void initialize(DatabaseMapping mapping, Session session) {
    }
}
