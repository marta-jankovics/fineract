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
package org.apache.fineract.portfolio.statement;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public final class StatementUtils {

    private StatementUtils() {}

    public static String ensureSize(String value, String name, int min, int max) {
        return ensureSize(value, name, min, max, true);
    }

    public static String ensureSize(String value, String name, int min, int max, boolean nullable) {
        if (Strings.isBlank(value)) {
            return nullable ? null : value;
        }
        int length = value.length();
        if (length < min) {
            log.warn("Statement parameter {} value '{}' is too short. Minimum length: {}", name, value, min);
            return StringUtils.rightPad(value, min - length);
        }
        if (length > max) {
            log.warn("Statement parameter {} value '{}' is too long. Maximum length: {}", name, value, max);
            return value.substring(0, max);
        }
        return value;
    }

    public static String[] ensureLines(String value, String name, int min, int max, int size) {
        return ensureLines(value, name, min, max, size, true);
    }

    public static String[] ensureLines(String value, String name, int min, int max, int size, boolean nullable) {
        if (Strings.isBlank(value)) {
            return nullable ? null : new String[0];
        }
        int length = value.length();
        if (length < min) {
            log.warn("Statement parameter {} value '{}' is too short. Minimum length: {}", name, value, min);
            return new String[] { StringUtils.rightPad(value, min - length) };
        }
        if (length > max) {
            List<String> lines = new ArrayList<>(size);
            value.lines().filter(e -> !Strings.isBlank(e)).forEach(e -> split(e, max, lines));
            List<String> result = lines;
            if (result.size() > size) {
                log.warn("Statement parameter {} value '{}' has too many lines. Maximum : {}", name, value, size);
                result = result.subList(0, size);
            }
            return result.toArray(new String[0]);
        }
        return new String[] { value };
    }

    public static void split(String value, int length, List<String> lines) {
        if (Strings.isBlank(value)) {
            return;
        }
        if (value.length() > length) {
            int idx = lastIndexOfWhitespace(value, length);
            lines.add(value.substring(0, idx < 0 ? length : idx));
            split(value.substring(idx < 0 ? length : idx + 1), length, lines);
        } else {
            lines.add(value);
        }
    }

    public static int indexOfWhitespace(@NotNull String value) {
        return indexOfWhitespace(value, 0);
    }

    public static int indexOfWhitespace(@NotNull String value, int start) {
        int idx = start;
        while (idx < value.length()) {
            char ch = value.charAt(idx);
            if (ch == ' ' || ch == '\t' || Character.isWhitespace(ch)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    public static int lastIndexOfWhitespace(@NotNull String value) {
        return lastIndexOfWhitespace(value, value.length());
    }

    public static int lastIndexOfWhitespace(@NotNull String value, int end) {
        int idx = end - 1;
        while (idx > 0) {
            char ch = value.charAt(idx);
            if (ch == ' ' || ch == '\t' || Character.isWhitespace(ch)) {
                return idx;
            }
            idx--;
        }
        return -1;
    }
}
