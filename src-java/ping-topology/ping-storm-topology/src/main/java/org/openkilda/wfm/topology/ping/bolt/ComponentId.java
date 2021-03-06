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

package org.openkilda.wfm.topology.ping.bolt;

public enum ComponentId {
    MONOTONIC_TICK("monotonic.tick"),
    TICK_DEDUPLICATOR("deduplicator.tick"),

    INPUT("input"),
    INPUT_DECODER("input.decoder"),
    INPUT_ROUTER("input.router"),

    FLOW_FETCHER("flow_fetcher"),
    PING_PRODUCER("ping_producer"),
    PING_ROUTER("ping.router"),
    PERIODIC_PING_SHAPING("shaping.periodic"),
    BLACKLIST("blacklist"),
    TIMEOUT_MANAGER("timeout_manager"),
    RESULT_DISPATCHER("result.dispatcher"),
    PERIODIC_RESULT_MANAGER("result_manager.periodic"),
    ON_DEMAND_RESULT_MANAGER("result_manager.manual"),
    GROUP_COLLECTOR("group_collector"),
    STATS_PRODUCER("stats_producer"),
    FAIL_REPORTER("fail_reporter"),

    SPEAKER_ENCODER("speaker.encoder"),
    SPEAKER_OUTPUT("speaker.output"),

    FLOW_STATUS_ENCODER("flow_status.encoder"),
    FLOW_STATUS_OUTPUT("flow_status.output"),

    OTSDB_ENCODER("otsdb.encoder"),
    OTSDB_OUTPUT("otsdb.output"),

    NORTHBOUND_ENCODER("northbound.encoder"),
    NORTHBOUND_OUTPUT("northbound.output");

    private final String value;

    ComponentId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
