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
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ZooKeeperWatchDog extends ZooKeeperClient implements WatchDog, DataCallback {

    private static final String SIGNAL = "signal";

    private String signalPath;
    private Signal signal;

    private Set<LifeCycleObserver> observers = new HashSet<>();

    @Builder
    public ZooKeeperWatchDog(String id, String serviceName, int apiVersion, String connectionString,
                             int sessionTimeout, Signal signal) throws IOException {
        super(id, serviceName, apiVersion, connectionString, sessionTimeout);

        signalPath = getPaths(serviceName, id, SIGNAL);
        if (signal == null) {
            signal = Signal.NONE;
        }
        this.signal = signal;
        initWatch();
    }

    @Override
    void validateNodes() throws KeeperException, InterruptedException {
        super.validateNodes();
        if (zookeeper.exists(getPaths(serviceName, id, SIGNAL), false) == null) {
            zookeeper.create(getPaths(serviceName, id, SIGNAL), signal.toString().getBytes(), Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }
    }


    private void checkSignal() throws KeeperException, InterruptedException {
        zookeeper.getData(signalPath, this, this, null);
    }

    @Override
    void initWatch() {
        try {
            validateNodes();
            checkSignal();
        } catch (KeeperException e) {
            log.error(e.getMessage());
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }


    @Override
    public void subscribe(LifeCycleObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unsubscribe(LifeCycleObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println(event);
        log.info("Received event: %s", event);
        try {
            if (!refreshConnection(event.getState()) && signalPath.equals(event.getPath())) {
                checkSignal();
            }
        } catch (IOException e) {
            log.error("Failed to read zk event: %s", e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            log.error("Zk event interrupted: %s", e.getMessage());
            e.printStackTrace();
        } catch (KeeperException e) {
            log.error("Zk keeper error: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        log.debug("Received result on path");
        if (signalPath.equals(path) && data != null && data.length > 0) {
            String signalString = new String(data);
            try {
                signal = Signal.valueOf(signalString);
                notifyObservers();
            } catch (Exception e) {
                log.error("Received unknown signal: %s", signalString);
            }
        }
    }

    protected void notifyObservers() {
        LifecycleEvent event = LifecycleEvent.builder().signal(signal).build();
        for (LifeCycleObserver observer : observers) {
            observer.handle(event);
        }
    }

}



