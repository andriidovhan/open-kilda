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

package org.openkilda.wfm.share.zk;

import org.openkilda.bluegreen.State;
import org.openkilda.bluegreen.ZooKeeperWriter;
import org.openkilda.wfm.AbstractBolt;

import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class ZooKeeperBolt extends AbstractBolt {
    public static final String FIELD_ID_STATE = "lifecycle.state";

    public static final String FIELD_ID_CONTEXT = AbstractBolt.FIELD_ID_CONTEXT;
    private String id;
    private String serviceName;
    private int apiVersion;
    private String connectionString;
    private ZooKeeperWriter zooKeeperWriter;


    public ZooKeeperBolt(String id, String serviceName, int apiVersion, String connectionString) {
        this.id = id;
        this.serviceName = serviceName;
        this.apiVersion = apiVersion;
        this.connectionString = connectionString;
    }

    @Override
    protected void handleInput(Tuple input) throws Exception {
        if (input.getValueByField(FIELD_ID_STATE) != null) {
            zooKeeperWriter.setState(State.SHUTDOWN);
        }
    }

    @Override
    protected void init() {
        try {
            this.zooKeeperWriter = ZooKeeperWriter.builder().id(id).serviceName(serviceName).apiVersion(apiVersion)
                .connectionString(connectionString).build();
        } catch (IOException | InterruptedException | KeeperException e) {
            log.error("Failed to init ZooKeeper with connection string: %s, received: %s ", connectionString,
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
