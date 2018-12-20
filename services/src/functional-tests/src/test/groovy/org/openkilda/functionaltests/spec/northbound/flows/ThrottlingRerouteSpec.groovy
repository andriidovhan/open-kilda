package org.openkilda.functionaltests.spec.northbound.flows

import static org.junit.Assume.assumeTrue
import static org.openkilda.testing.Constants.WAIT_OFFSET

import org.openkilda.functionaltests.BaseSpecification
import org.openkilda.functionaltests.extension.fixture.rule.CleanupSwitches
import org.openkilda.functionaltests.helpers.PathHelper
import org.openkilda.functionaltests.helpers.Wrappers
import org.openkilda.messaging.info.event.IslChangeType
import org.openkilda.messaging.info.event.PathNode
import org.openkilda.messaging.payload.flow.FlowState
import org.openkilda.model.SwitchId
import org.openkilda.northbound.dto.switches.PortDto
import org.openkilda.testing.model.topology.TopologyDefinition.Switch

import org.springframework.beans.factory.annotation.Value
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Narrative

import java.util.concurrent.TimeUnit

@Narrative("""
This test verifies that we do not perform a reroute as soon as we receive a reroute request (we talk only about
automatic reroutes here; manual reroutes are still performed instantly). Instead, system waits for 'reroute.delay'
seconds and if no new reroute requests are issued, it performs ONE reroute for each requested flowId. If ANY new reroute
is issued during 'reroute.delay' the timer is refreshed. 
System should stop refreshing the timer if 'reroute.hardtimeout' is reached and perform all the queued reroutes (unique 
for each flowId).
""")
@CleanupSwitches
class ThrottlingRerouteSpec extends BaseSpecification {

    @Value('${reroute.hardtimeout}')
    int rerouteHardTimeout

    def setupOnce() {
        //Sometimes Kilda misses a discovery packet -> doesn't issue a reroute,
        //so need to allow at least twice that time before closing the window
        assumeTrue("These tests assume a bigger time gap between \${reroute.delay} and \${discovery.interval}",
                rerouteDelay > discoveryInterval * 2 + 1)
    }

    @Ignore("Should be reworked due to https://github.com/telstra/open-kilda/issues/1729")
    def "Reroute is not performed while new reroutes are being issued (alt path available)"() {
        def blinkingPeriod = rerouteDelay
        assumeTrue("Configured reroute timeouts are not acceptable for this test. " +
                "Make a bigger gap between \${reroute.hardtimeout} and \${reroute.delay}",
                blinkingPeriod + rerouteDelay + 1 < rerouteHardTimeout)

        given: "A flow with alternate paths available"
        def switches = topology.getActiveSwitches()
        List<List<PathNode>> allPaths = []
        def (Switch srcSwitch, Switch dstSwitch) = [switches, switches].combinations()
                .findAll { src, dst -> src != dst }.unique { it.sort() }.find { Switch src, Switch dst ->
            allPaths = database.getPaths(src.dpId, dst.dpId)*.path
            allPaths.size() > 1
        } ?: assumeTrue("No suiting switches found", false)
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flowHelper.addFlow(flow)
        def currentPath = PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "Make the current path less preferable than alternatives"
        def alternativePaths = allPaths.findAll { it != currentPath }
        alternativePaths.each { pathHelper.makePathMorePreferable(it, currentPath) }

        when: "One of the ISLs blinks for some time"
        def isl = pathHelper.getInvolvedIsls(currentPath).first()
        def endTime = System.currentTimeSeconds() + blinkingPeriod
        while (System.currentTimeSeconds() < endTime) {
            blinkPort(isl.dstSwitch.dpId, isl.dstPort)
        }

        then: "The flow remains on the same path"
        currentPath == PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "Still on the same path right before the timeout should run out"
        TimeUnit.SECONDS.sleep(rerouteDelay - discoveryInterval - 1)
        currentPath == PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "The flow reroutes (changes path) after window timeout"
        Wrappers.wait(WAIT_OFFSET + discoveryInterval + 1) {
            assert currentPath != PathHelper.convert(northbound.getFlowPath(flow.id))
        }
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.id).status == FlowState.UP }
        //TODO(rtretiak): Check logs that only 1 reroute has been performed

        and: "Do cleanup"
        flowHelper.deleteFlow(flow.id)
    }

    @Ignore("Should be reworked due to https://github.com/telstra/open-kilda/issues/1729")
    def "Reroute is not performed while new reroutes are being issued (no alt path)"() {
        def blinkingPeriod = rerouteDelay
        assumeTrue("Configured reroute timeouts are not acceptable for this test. " +
                "Make a bigger gap between \${reroute.hardtimeout} and \${reroute.delay}",
                blinkingPeriod + rerouteDelay + 1 < rerouteHardTimeout)

        given: "A flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.getActiveSwitches()[0..1]
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flowHelper.addFlow(flow)
        def allPaths = database.getPaths(srcSwitch.dpId, dstSwitch.dpId)*.path
        def currentPath = PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "Ports that lead to alternative paths are brought down to deny alternative paths"
        def altPaths = allPaths.findAll { it != currentPath }
        List<PortDto> broughtDownPorts = altPaths.collect { path ->
            northbound.portDown(path.first().switchId, path.first().portNo)
        }

        when: "One of the flow's ISLs blinks for some time (issuing reroutes)"
        def isl = pathHelper.getInvolvedIsls(currentPath).first()
        def endTime = System.currentTimeSeconds() + blinkingPeriod
        while (System.currentTimeSeconds() < endTime) {
            blinkPort(isl.dstSwitch.dpId, isl.dstPort)
        }

        and: "Ends up in FAILED state"
        northbound.portDown(isl.dstSwitch.dpId, isl.dstPort)
        Wrappers.wait(WAIT_OFFSET) { assert islUtils.getIslInfo(isl).get().state == IslChangeType.FAILED }

        then: "The flow is not rerouted and remains UP"
        northbound.getFlowStatus(flow.id).status == FlowState.UP
        currentPath == PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "Still UP and on the same path right before the timeout should run out"
        TimeUnit.SECONDS.sleep(rerouteDelay - discoveryInterval - 1)
        currentPath == PathHelper.convert(northbound.getFlowPath(flow.id))
        northbound.getFlowStatus(flow.id).status == FlowState.UP

        and: "The flow tries to reroute and goes DOWN after window timeout"
        Wrappers.wait(WAIT_OFFSET + discoveryInterval + 1) {
            assert northbound.getFlowStatus(flow.id).status == FlowState.DOWN
        }
        //TODO(rtretiak): Check logs that only 1 reroute has been performed

        and: "Do cleanup"
        flowHelper.deleteFlow(flow.id)
        northbound.portUp(isl.dstSwitch.dpId, isl.dstPort)
        broughtDownPorts.each { northbound.portUp(new SwitchId(it.switchId), it.portNumber) }
        Wrappers.wait(WAIT_OFFSET) { northbound.getAllLinks().each { assert it.state != IslChangeType.FAILED } }
    }

    @Ignore
    @Issue("https://github.com/telstra/open-kilda/issues/1636")
    def "Reroute timer is refreshed even if another flow reroute is issued"() {
        def blinkingPeriod = rerouteDelay
        assumeTrue("Configured reroute timeouts are not acceptable for this test. " +
                "Make a bigger gap between \${reroute.hardtimeout} and \${reroute.delay}",
                blinkingPeriod + rerouteDelay + 1 < rerouteHardTimeout)

        given: "2 flows with alternate paths available"
        def switches = topology.getActiveSwitches()
        List<List<PathNode>> allPaths1 = []
        def (Switch srcSwitch1, Switch dstSwitch1) = [switches, switches].combinations()
                .findAll { src, dst -> src != dst }.unique { it.sort() }.find { Switch src, Switch dst ->
            allPaths1 = database.getPaths(src.dpId, dst.dpId)*.path
            allPaths1.size() > 1
        } ?: assumeTrue("No suiting switches found", false)
        def flow1 = flowHelper.randomFlow(srcSwitch1, dstSwitch1)
        def restSwitches = switches.findAll { it != srcSwitch1 } //do not want the same flow, excluding used srcSwitch
        assumeTrue("Not enough switches in the topology", restSwitches.size() > 1)
        def (Switch srcSwitch2, Switch dstSwitch2) = restSwitches[0..1]
        def flow2 = flowHelper.randomFlow(srcSwitch2, dstSwitch2)
        def (currentPath1, currentPath2) = [flow1, flow2].collect { flow ->
            flowHelper.addFlow(flow)
            PathHelper.convert(northbound.getFlowPath(flow.id))
        }
        def flow1Isls = pathHelper.getInvolvedIsls(currentPath1)
        def flow2Isls = pathHelper.getInvolvedIsls(currentPath2)

        and: "Make the current path for the flow1 less preferable than alternatives"
        //Current implementation cannot guarantee that by making the same for second
        // flow both flows will choose new path. So will only track path change for flow1
        allPaths1.findAll { it != currentPath1 }.each { pathHelper.makePathMorePreferable(it, currentPath1) }

        when: "Unique ISL for the flow1 blinks twice, initiating 2 reroutes of the flow1"
        def isl1 = flow1Isls.find { !flow2Isls.contains(it) && !flow2Isls.contains(islUtils.reverseIsl(it)) }
        2.times { blinkPort(isl1.dstSwitch.dpId, isl1.dstPort) }

        and: "Right before timeout ends the flow2 ISL blinks twice"
        TimeUnit.SECONDS.sleep(rerouteDelay - discoveryInterval - 2)
        def isl2 = flow2Isls.find { !flow1Isls.contains(it) && !flow1Isls.contains(islUtils.reverseIsl(it)) }
        2.times { blinkPort(isl2.dstSwitch.dpId, isl2.dstPort) }

        then: "The flow1 is still on its path right before the updated timeout runs out"
        TimeUnit.SECONDS.sleep(rerouteDelay - discoveryInterval - 2)
        currentPath1 == PathHelper.convert(northbound.getFlowPath(flow1.id))

        and: "The flow1 reroutes (changes path) after window timeout"
        Wrappers.wait(WAIT_OFFSET + discoveryInterval) {
            assert currentPath1 != PathHelper.convert(northbound.getFlowPath(flow1.id))
        }
        Wrappers.wait(WAIT_OFFSET) {
            [flow1, flow2].each { assert northbound.getFlowStatus(it.id).status == FlowState.UP }
        }
        //TODO(rtretiak): Check logs that 1 reroute is also issued for flow2
        //TODO(rtretiak): Check logs that only 1 reroute for each flow has been performed

        and: "Do cleanup"
        [flow1, flow2].each { flowHelper.deleteFlow(it.id) }
    }

    @Ignore("Should be reworked due to https://github.com/telstra/open-kilda/issues/1729")
    def "Reroute is performed after hard timeout even if new reroutes are still being issued"() {
        given: "A flow with alternate paths available"
        def switches = topology.getActiveSwitches()
        List<List<PathNode>> allPaths = []
        def (Switch srcSwitch, Switch dstSwitch) = [switches, switches].combinations()
                .findAll { src, dst -> src != dst }.unique { it.sort() }.find { Switch src, Switch dst ->
            allPaths = database.getPaths(src.dpId, dst.dpId)*.path
            allPaths.size() > 1
        } ?: assumeTrue("No suiting switches found", false)
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flowHelper.addFlow(flow)
        def currentPath = PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "Make the current path less preferable than alternatives"
        def alternativePaths = allPaths.findAll { it != currentPath }
        alternativePaths.each { pathHelper.makePathMorePreferable(it, currentPath) }

        when: "One of the ISLs begins to blink"
        def isl = pathHelper.getInvolvedIsls(currentPath).first()
        def hardTimeoutTime = System.currentTimeSeconds() + rerouteHardTimeout
        def stopBlinkingTime = hardTimeoutTime + 5
        def blinkingThread = new Thread({
            while (System.currentTimeSeconds() < stopBlinkingTime) {
                blinkPort(isl.dstSwitch.dpId, isl.dstPort)
            }
        })
        blinkingThread.start()

        then: "The flow is still not rerouted right before hard timeout should end"
        TimeUnit.SECONDS.sleep(hardTimeoutTime - System.currentTimeSeconds() - discoveryInterval)
        currentPath == PathHelper.convert(northbound.getFlowPath(flow.id))

        and: "The flow rerouted after hard timeout despite ISL is still blinking"
        Wrappers.wait(hardTimeoutTime - System.currentTimeSeconds() + WAIT_OFFSET) {
            assert currentPath != PathHelper.convert(northbound.getFlowPath(flow.id))
        }
        blinkingThread.alive
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.id).status == FlowState.UP }
        //TODO(rtretiak): Check logs that only 1 reroute has been performed

        and: "Do cleanup"
        flowHelper.deleteFlow(flow.id)

        cleanup: "Wait for blinking thread to finish"
        blinkingThread && blinkingThread.join()
    }

    def "Flow can be safely deleted while it is in the reroute window waiting for reroute"() {
        given: "A flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flowHelper.addFlow(flow)
        def path = PathHelper.convert(northbound.getFlowPath(flow.id))

        when: "Init a flow reroute by blinking a port"
        def islToBreak = pathHelper.getInvolvedIsls(pathHelper.convert(northbound.getFlowPath(flow.id))).first()
        blinkPort(islToBreak.dstSwitch.dpId, islToBreak.dstPort)

        and: "Immediately remove the flow before reroute delay runs out and flow is actually rerouted"
        flowHelper.deleteFlow(flow.id)

        and: "Refresh the reroute by blinking the port again"
        blinkPort(islToBreak.dstSwitch.dpId, islToBreak.dstPort)

        and: "Wait until reroute delay runs out"
        TimeUnit.SECONDS.sleep(rerouteDelay + 1)

        then: "The flow is not present in NB"
        northbound.getAllFlows().every { it.id != flow.id }

        and: "The flow is not present in Database"
        database.countFlows() == 0

        and: "Related switches has no excess rules"
        pathHelper.getInvolvedSwitches(path).each {
            verifySwitchRules(it.dpId)
        }
    }

    def "Auto-reroute on the flow which is already deleted should not revive the flow"() {
        given: "A flow"
        def (Switch srcSwitch, Switch dstSwitch) = topology.activeSwitches
        def flow = flowHelper.randomFlow(srcSwitch, dstSwitch)
        flowHelper.addFlow(flow)
        def path = PathHelper.convert(northbound.getFlowPath(flow.id))

        when: "Remove the flow"
        flowHelper.deleteFlow(flow.id)

        and: "Immediately break the path to init all flows on that path to reroute"
        def islToBreak = pathHelper.getInvolvedIsls(path).first()
        northbound.portDown(islToBreak.srcSwitch.dpId, islToBreak.srcPort)

        then: "The flow is not present in the system after reroute timeout"
        TimeUnit.SECONDS.sleep(rerouteDelay + WAIT_OFFSET)
        !northbound.getAllFlows().find { it.id == flow.id }
        northbound.getAllLinks().every { it.availableBandwidth == it.speed }

        and: "No rule discrepancies observed"
        topology.activeSwitches.each {
            verifySwitchRules(it.dpId)
        }

        and: "Bring port back up"
        northbound.portUp(islToBreak.srcSwitch.dpId, islToBreak.srcPort)
        Wrappers.wait(WAIT_OFFSET) { northbound.getAllLinks().each { assert it.state != IslChangeType.FAILED } }
    }

    def cleanup() {
        northbound.deleteLinkProps(northbound.getAllLinkProps())
        database.resetCosts()
        Wrappers.wait(WAIT_OFFSET) { northbound.getAllLinks().each { assert it.availableBandwidth == it.speed } }
    }

    def blinkPort(SwitchId sw, int port) {
        northbound.portDown(sw, port)
        northbound.portUp(sw, port)
        //give Kilda time to send and receive a discovery packet, so that ISL is rediscovered and reroute is reissued
        sleep(discoveryInterval * 1000)
    }
}
