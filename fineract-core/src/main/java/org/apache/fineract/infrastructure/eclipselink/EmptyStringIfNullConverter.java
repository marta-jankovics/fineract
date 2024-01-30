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
    public EmptyStringIfNullConverter() {}

    /**
     * Converts String field value to non-null String.
     *
     * @param value
     *            source Object field value
     * @param session
     *            current database session
     * @return target String to be stored as JDBC VARCHAR
     */
    @Override
    public Object convertObjectValueToDataValue(Object value, Session session) {
        return value == null ? "" : value;
    }

    /**
     * Converts String from JDBC VARCHAR parameter to String field value.
     *
     * @param jdbcValue
     *            source String from JDBC VARCHAR
     * @param session
     *            current database session
     * @return target UUID field value
     */
    @Override
    public Object convertDataValueToObjectValue(Object jdbcValue, Session session) {
        return ((String) jdbcValue).isEmpty() ? null : jdbcValue;
    }

    /**
     * UUID values and String are immutable.
     *
     * @return value of {@code false}
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * Initialize mapping for JDBC data type.
     *
     * @param mapping
     *            field database mapping
     * @param session
     *            current database session
     */
    @Override
    public void initialize(DatabaseMapping mapping, Session session) {}
}
