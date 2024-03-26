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
package org.apache.fineract.statement.data.camt053;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContactDetailsData {

    @JsonProperty("MobileNumber")
    @JsonFormat(shape = STRING, pattern = "^\\+[0-9]{1,3}-[0-9()+\\-]{1,30}$")
    @Size(min = 1, max = 35)
    private final String mobileNumber;
    @JsonProperty("EmailAddress")
    @Size(min = 1, max = 2048)
    private final String emailAddress;
    @JsonProperty("Other")
    private final OtherContactData[] otherContact;

    public static ContactDetailsData create(String mobileNumber, String emailAddress, OtherContactData... otherContacts) {
        if (otherContacts != null
                && (otherContacts = Arrays.stream(otherContacts).filter(Objects::nonNull).toArray(OtherContactData[]::new)).length == 0) {
            otherContacts = null;
        }
        if (mobileNumber == null && emailAddress == null && otherContacts == null) {
            return null;
        }
        return new ContactDetailsData(mobileNumber, emailAddress, otherContacts);
    }
}
