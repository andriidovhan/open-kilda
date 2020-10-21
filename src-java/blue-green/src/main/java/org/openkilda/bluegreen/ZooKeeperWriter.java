/* Copyright 2020 Telstra Open Source
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

package org.openkilda.bluegreen;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;

@Slf4j
public class ZooKeeperWriter extends ZooKeeperClient {
    private static final String STATE = "state";
    private String statePath;
    private State state;


    @Builder
    public ZooKeeperWriter(String id, String serviceName, int apiVersion, String connectionString,
                           int sessionTimeout, State state) throws IOException, KeeperException, InterruptedException {
        super(id, serviceName, apiVersion, connectionString, sessionTimeout);
        if (state == null) {
            state = State.STARTING;
        }
        this.state = state;
        statePath = getPaths(serviceName, id, STATE);

        validateNodes();
    }

    /**
     * Sets state for service.
     */
    public void setState(@NonNull State state) {
        try {
            zookeeper.setData(statePath, state.toString().getBytes(), -1);
            this.state = state;
        } catch (KeeperException e) {
            log.error("Zk keeper error: %s", e.getMessage());
            throw new IllegalStateException("failed to update state");
        } catch (InterruptedException e) {
            log.error("Zk event interrupted: %s", e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("failed to update state");
        }
    }


    @Override
    void validateNodes() throws KeeperException, InterruptedException {
        super.validateNodes();
        if (zookeeper.exists(getPaths(serviceName, id, STATE), false) == null) {
            zookeeper.create(getPaths(serviceName, id, STATE), state.toString().getBytes(), Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        log.info("Received event: %s", event);
        try {
            refreshConnection(event.getState());
        } catch (IOException e) {
            log.error("Failed to read zk event: %s", e.getMessage());
            e.printStackTrace();
        }
    }


}
