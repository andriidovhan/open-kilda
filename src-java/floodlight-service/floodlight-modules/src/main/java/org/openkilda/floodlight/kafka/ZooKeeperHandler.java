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

package org.openkilda.floodlight.kafka;

import org.openkilda.bluegreen.LifeCycleObserver;
import org.openkilda.bluegreen.LifecycleEvent;
import org.openkilda.bluegreen.Signal;
import org.openkilda.bluegreen.ZooKeeperWatchDog;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ZooKeeperHandler implements Runnable, LifeCycleObserver {
    private final AtomicBoolean active;

    private ZooKeeperWatchDog watchDog;
    private LifecycleEvent event;

    public ZooKeeperHandler(AtomicBoolean active, String id, String serviceName, int apiVersion,
                            String connectionString) {
        this.active = active;

        try {
            this.watchDog = ZooKeeperWatchDog.builder().id(id).serviceName(serviceName).apiVersion(apiVersion)
                    .connectionString(connectionString).build();
            watchDog.subscribe(this);
        } catch (IOException e) {
            log.error("Failed to init ZooKeeper with connection string: {}, received: {}",
                    connectionString, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(LifecycleEvent lifecycleEvent) {
        this.event = lifecycleEvent;
        if (event != null) {
            this.active.set(!event.getSignal().equals(Signal.SHUTDOWN));
        }
    }
}
