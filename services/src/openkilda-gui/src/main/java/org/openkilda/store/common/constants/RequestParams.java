/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.store.common.constants;

import lombok.Getter;

@Getter
public enum RequestParams {

    USER_NAME("<user-name>","In place of username."),
    PASSWORD("<password>","In place of password."),
    ACCESS_TOKEN("<access-token>","In place of access token."),
    LINK_ID("<link-id>","In place of link id."),
    STATUS("<status>","In place of status."),
    CONTRACT_ID("<contract-id>","In place of contract id.");
    
    private String value;
    
    private String description;
    
    /**
     * Instantiates a new request params.
     *
     * @param value the value
     * @param description the description
     */
    private RequestParams(final String value, final String description) {
        this.value = value;
        this.description = description;
    }
}
