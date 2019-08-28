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

package org.openkilda.wfm.topology.switchmanager.bolt.speaker;

import org.openkilda.floodlight.api.FlowSegmentSchemaEntry;
import org.openkilda.floodlight.api.request.DefaultFlowsSchemaRequest;
import org.openkilda.floodlight.api.request.FlowSegmentBlankGenericResolver;
import org.openkilda.floodlight.api.request.MetersDumpRequest;
import org.openkilda.floodlight.api.request.SpeakerRequest;
import org.openkilda.floodlight.api.request.TableDumpRequest;
import org.openkilda.floodlight.api.response.SpeakerDefaultFlowsSchemaResponse;
import org.openkilda.floodlight.api.response.SpeakerFlowSegmentSchemaResponse;
import org.openkilda.floodlight.api.response.SpeakerMetersDumpResponse;
import org.openkilda.floodlight.api.response.SpeakerResponse;
import org.openkilda.floodlight.api.response.SpeakerTableDumpResponse;
import org.openkilda.floodlight.flow.response.FlowErrorResponse;
import org.openkilda.messaging.MessageContext;
import org.openkilda.model.SwitchId;
import org.openkilda.wfm.topology.switchmanager.model.SpeakerSwitchSchema;
import org.openkilda.wfm.topology.switchmanager.model.SwitchDefaultFlowsSchema;
import org.openkilda.wfm.topology.switchmanager.model.SwitchOfMeterDump;
import org.openkilda.wfm.topology.switchmanager.model.SwitchOfTableDump;
import org.openkilda.wfm.topology.switchmanager.model.ValidateFlowSegmentEntry;
import org.openkilda.wfm.topology.switchmanager.service.SpeakerWorkerCarrier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SchemaFetchHandler extends WorkerHandler {
    private final SpeakerWorkerCarrier carrier;
    private final MessageContext context;

    private final SwitchId switchId;

    private final Map<UUID, FlowSegmentBlankGenericResolver> requestBlanks = new HashMap<>();
    private final Map<UUID, ValidateFlowSegmentEntry> ofSchema = new HashMap<>();

    private final Map<Integer, UUID> tableRequests = new HashMap<>();
    private final Map<Integer, SwitchOfTableDump> tableDumps = new HashMap<>();

    private final Map<RequestType, UUID> otherRequests = new EnumMap<>(RequestType.class);
    private final Map<RequestType, ResponseMapper> otherResponses = new EnumMap<>(RequestType.class);

    public SchemaFetchHandler(
            SpeakerWorkerCarrier carrier, SwitchId switchId, List<FlowSegmentBlankGenericResolver> schemaRequests) {
        this.carrier = carrier;
        this.context = (new MessageContext(carrier.getCommandContext().getCorrelationId()))
                .fork("schema").fork(switchId.toString());

        this.switchId = switchId;

        for (FlowSegmentBlankGenericResolver entry : schemaRequests) {
            carrier.sendSpeakerCommand(entry.makeSchemaRequest());
            requestBlanks.put(entry.getCommandId(), entry);
        }

        // force table 0 dump (to get current system/default OF flows)
        requestOfTableDump(0);
    }

    @Override
    public void speakerResponse(SpeakerResponse response) {
        if (response instanceof SpeakerFlowSegmentSchemaResponse) {
            handleSpeakerResponse((SpeakerFlowSegmentSchemaResponse) response);
        } else if (response instanceof SpeakerTableDumpResponse) {
            handleSpeakerResponse((SpeakerTableDumpResponse) response);
        } else if (response instanceof SpeakerDefaultFlowsSchemaResponse) {
            handleSpeakerResponse((SpeakerDefaultFlowsSchemaResponse) response);
        } else if (response instanceof SpeakerMetersDumpResponse) {
            handleSpeakerResponse((SpeakerMetersDumpResponse) response);
        } else if (response instanceof FlowErrorResponse) {
            handleSpeakerResponse((FlowErrorResponse) response);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Reject %s speaker response (unexpected/unsupported response type)",
                    response.getClass().getName()));
        }
    }

    @Override
    public void timeout() {
        carrier.sendHubValidationError(null);
    }

    @Override
    public boolean isCompleted() {
        if (requestBlanks.size() != ofSchema.size()) {
            return false;
        }
        if (tableRequests.size() != tableDumps.size()) {
            return false;
        }
        return otherRequests.size() == otherResponses.size();
    }

    private void handleSpeakerResponse(SpeakerFlowSegmentSchemaResponse schemaResponse) {
        FlowSegmentBlankGenericResolver blank = requestBlanks.get(schemaResponse.getCommandId());
        if (blank == null) {
            log.warn(
                    "Receive unwanted flow segment schema response - sw:{} commandID: {}",
                    switchId, schemaResponse.getCommandId());
            return;
        }

        handleFlowSegmentSchema(blank, schemaResponse.getSchema());
    }

    private void handleSpeakerResponse(SpeakerTableDumpResponse response) {
        UUID request = tableRequests.get(response.getTableId());
        if (! request.equals(response.getCommandId())) {
            log.error(
                    "Receive unwanted OF table dump response - sw:{} commandID:{}", switchId, response.getCommandId());
            return;
        }

        SwitchOfTableDump tableDump = new SwitchOfTableDump(switchId, response.getTableId(), response.getEntries());
        tableDumps.put(response.getTableId(), tableDump);

        makeCompleteAttempt();
    }

    private void handleSpeakerResponse(SpeakerDefaultFlowsSchemaResponse response) {
        if (Objects.equals(otherRequests.get(RequestType.DEFAULT_SCHEMA), response.getCommandId())) {
            log.error(
                    "Receive unwanted default flows schema dump response - sw:{} commandID:{}",
                    response.getSwitchId(), response.getCommandId());
            return;
        }

        SwitchDefaultFlowsSchema defaulFlowsSchema = new SwitchDefaultFlowsSchema(
                response.getSwitchId(), response.getEntries());
        otherResponses.put(RequestType.DEFAULT_SCHEMA, new DefaultSchemaMapper(defaulFlowsSchema));

        makeCompleteAttempt();
    }

    private void handleSpeakerResponse(SpeakerMetersDumpResponse response) {
        if (Objects.equals(otherRequests.get(RequestType.METERS), response.getCommandId())) {
            log.error(
                    "Receive unwanted OF meters dump response - sw:{} commandID:{}",
                    response.getSwitchId(), response.getCommandId());
            return;
        }

        SwitchOfMeterDump meterDump = new SwitchOfMeterDump(response.getSwitchId(), response.getEntries());
        otherResponses.put(RequestType.METERS, new MeterMapper(meterDump));

        makeCompleteAttempt();
    }

    private void handleSpeakerResponse(FlowErrorResponse error) {
        carrier.sendHubValidationError(error);  // terminate point
    }

    private void handleFlowSegmentSchema(FlowSegmentBlankGenericResolver blank, org.openkilda.floodlight.api.FlowSegmentSchema schema) {
        if (! switchId.equals(schema.getDatapath())) {
            carrier.sendHubValidationWorkerError(String.format(
                    "Receive invalid flow segment - segment address switch %s but handler request switch %s",
                    schema.getDatapath(), switchId));  // terminate point
            return;
        }

        ofSchema.put(blank.getCommandId(), new ValidateFlowSegmentEntry(blank, schema));

        for (FlowSegmentSchemaEntry entry : schema.getEntries()) {
            requestOfTableDump((int) entry.getTableId());
            if (entry.getMeterId() != null) {
                requestOther(RequestType.METERS);
            }
        }

        makeCompleteAttempt();
    }

    private void makeCompleteAttempt() {
        if (! isCompleted()) {
            return;
        }

        SpeakerSwitchSchema.SpeakerSwitchSchemaBuilder schemaBuilder = SpeakerSwitchSchema.builder()
                .flowSegments(ImmutableList.copyOf(ofSchema.values()))
                .tables(ImmutableMap.copyOf(tableDumps));
        for (ResponseMapper mapper : otherResponses.values()) {
            mapper.apply(schemaBuilder);
        }

        carrier.sendHubSwitchSchema(schemaBuilder.build());  // terminate point
    }

    private void requestOfTableDump(Integer tableId) {
        if (tableRequests.containsKey(tableId)) {
            return;
        }

        TableDumpRequest dumpRequest = new TableDumpRequest(
                context.fork(String.valueOf(tableId)), switchId, UUID.randomUUID(), tableId);
        carrier.sendSpeakerCommand(dumpRequest);

        tableRequests.put(tableId, dumpRequest.getCommandId());
    }

    private void requestOther(RequestType type) {
        if (otherRequests.containsKey(type)) {
            return;
        }

        SpeakerRequest request;
        switch (type) {
            case METERS:
                request = new MetersDumpRequest(context.fork("meters"), switchId, UUID.randomUUID());
                break;
            case DEFAULT_SCHEMA:
                request = new DefaultFlowsSchemaRequest(context.fork("default-flows"), switchId, UUID.randomUUID());
                break;
            default:
                throw new UnsupportedOperationException(String.format(
                        "There is no mapping for %s.%s", type.getClass().getName(), type));
        }

        carrier.sendSpeakerCommand(request);

        otherRequests.put(type, request.getCommandId());
    }

    private abstract static class ResponseMapper {
        abstract void apply(SpeakerSwitchSchema.SpeakerSwitchSchemaBuilder builder);
    }

    @AllArgsConstructor
    private static class MeterMapper extends ResponseMapper {
        private final SwitchOfMeterDump payload;

        @Override
        void apply(SpeakerSwitchSchema.SpeakerSwitchSchemaBuilder builder) {
            builder.meters(payload);
        }
    }

    @AllArgsConstructor
    private static class DefaultSchemaMapper extends ResponseMapper {
        private final SwitchDefaultFlowsSchema schema;

        @Override
        void apply(SpeakerSwitchSchema.SpeakerSwitchSchemaBuilder builder) {
            builder.defaultFlowsSchema(schema);
        }
    }

    private enum RequestType {
        DEFAULT_SCHEMA,
        METERS
    }
}
