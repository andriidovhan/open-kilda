/* Copyright 2019 Telstra Open Source
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

package org.openkilda.persistence.repositories;

import org.openkilda.model.FlowCookie;

import java.util.Collection;
import java.util.Optional;

public interface FlowCookieRepository extends Repository<FlowCookie> {
    Collection<FlowCookie> findAll();

    boolean exists(long unmaskedCookie);

    Optional<FlowCookie> findByCookie(long unmaskedCookie);

    /**
     * Find the first (lowest by value) cookie value which is not assigned to any flow.
     *
     * @param lowestCookieValue the lowest value for a potential cookie.
     * @param highestCookieValue the highest value for a potential cookie.
     * @return the found cookie value
     */
    Optional<Long> findFirstUnassignedCookie(long lowestCookieValue, long highestCookieValue);
}
