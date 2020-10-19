package org.openkilda.functionaltests.spec.flows

import static groovyx.gpars.GParsPool.withPool
import static org.junit.Assume.assumeTrue
import static org.openkilda.functionaltests.extension.tags.Tag.HARDWARE
import static org.openkilda.functionaltests.extension.tags.Tag.LOW_PRIORITY
import static org.openkilda.functionaltests.extension.tags.Tag.SMOKE_SWITCHES
import static org.openkilda.functionaltests.extension.tags.Tag.TOPOLOGY_DEPENDENT
import static org.openkilda.testing.Constants.PROTECTED_PATH_INSTALLATION_TIME
import static org.openkilda.testing.Constants.RULES_DELETION_TIME
import static org.openkilda.testing.Constants.NON_EXISTENT_FLOW_ID
import static org.openkilda.testing.Constants.NON_EXISTENT_SWITCH_ID
import static org.openkilda.testing.Constants.RULES_INSTALLATION_TIME
import static org.openkilda.testing.Constants.WAIT_OFFSET
import static org.openkilda.testing.service.floodlight.model.FloodlightConnectMode.RW

import org.openkilda.functionaltests.HealthCheckSpecification
import org.openkilda.functionaltests.extension.failfast.Tidy
import org.openkilda.functionaltests.extension.tags.IterationTag
import org.openkilda.functionaltests.extension.tags.IterationTags
import org.openkilda.functionaltests.extension.tags.Tags
import org.openkilda.functionaltests.helpers.PathHelper
import org.openkilda.functionaltests.helpers.Wrappers
import org.openkilda.messaging.error.MessageError
import org.openkilda.messaging.info.event.IslChangeType
import org.openkilda.messaging.payload.flow.FlowState
import org.openkilda.model.FlowEncapsulationType
import org.openkilda.model.SwitchId
import org.openkilda.model.cookie.Cookie
import org.openkilda.model.cookie.CookieBase.CookieType
import org.openkilda.northbound.dto.v1.flows.PingInput
import org.openkilda.northbound.dto.v2.flows.FlowLoopPayload
import org.openkilda.northbound.dto.v2.flows.FlowRequestV2
import org.openkilda.testing.model.topology.TopologyDefinition.Isl
import org.openkilda.testing.service.traffexam.TraffExamService
import org.openkilda.testing.tools.FlowTrafficExamBuilder

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Ignore
import spock.lang.Narrative
import spock.lang.See
import spock.lang.Unroll

import javax.inject.Provider

@See("https://github.com/telstra/open-kilda/tree/develop/docs/design/flow-loop")
@Narrative("""Flow loop feature designed for flow path testing. Loop provides additional flow rules on one of the 
terminating switch so any flow traffic is returned to switch-port where it was received. Such flow has 'looped=true'
flag and supports all flow operations. When the loop removed system should restore the original flow rules.
Enabling flowLoop in flow history is registered as the 'update' operation.""")
class FlowLoopSpec extends HealthCheckSpecification {

    @Autowired
    Provider<TraffExamService> traffExamProvider

    @Tidy
    @Unroll
    @IterationTags([
            @IterationTag(tags = [SMOKE_SWITCHES, TOPOLOGY_DEPENDENT], iterationNameRegex = /protected/),
            @IterationTag(tags = [HARDWARE], iterationNameRegex = /vxlan/)
    ])
    def "Able to create flowLoop for a #data.flowDescription flow"() {
        given: "An active and valid  #data.flowDescription flow"
        def allTraffGenSwIds = topology.activeTraffGens*.switchConnected*.dpId
        assumeTrue("Unable to find switches connected to traffGens", (allTraffGenSwIds.size() > 1))
        def switchPair = data.switchPair(allTraffGenSwIds)
        assumeTrue("Unable to find required switch pair in topology", switchPair != null)
        def flow = flowHelperV2.randomFlow(switchPair)
        flow.tap(data.flowTap)
        flowHelperV2.addFlow(flow)

        when: "Create flowLoop on the src switch"
        def flowLoopPayloadSrcSw = new FlowLoopPayload(switchPair.src.dpId)
        def createResponse = northboundV2.createFlowLoop(flow.flowId, flowLoopPayloadSrcSw)

        then: "Create flowLoop response contains flowId and src switchId"
        assert createResponse.flowId == flow.flowId
        assert createResponse.switchId == switchPair.src.dpId

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) {
            assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP
        }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "FlowLoop is really created on the src switch"
        northbound.getFlow(flow.flowId).loopSwitchId == switchPair.src.dpId

        and: "FlowLoop rules are created on the src switch"
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            Map<Long, Long> flowLoopsCounter = getFlowLoopRules(switchPair.src.dpId)
                    .collectEntries { [(it.cookie): it.packetCount] }
            assert flowLoopsCounter.size() == 2
            assert flowLoopsCounter.values().every { it == 0 }
        }

        and: "FlowLoop rules are not created on the dst switch"
        getFlowLoopRules(switchPair.dst.dpId).empty

        and: "The src switch is valid"
        northbound.validateSwitch(switchPair.src.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])

        and: "Flow is pingable"
        with(northbound.pingFlow(flow.flowId, new PingInput())) {
            forward.pingSuccess
            reverse.pingSuccess
        }

        when: "Send traffic via flow"
        def traffExam = traffExamProvider.get()
        def exam = new FlowTrafficExamBuilder(topology, traffExam)
                .buildBidirectionalExam(flowHelperV2.toV1(flow), 1000, 5)

        then: "Flow doesn't allow traffic, because it is grubbed by flowLoop rules"
        withPool {
            [exam.forward, exam.reverse].eachParallel { direction ->
                def resources = traffExam.startExam(direction)
                direction.setResources(resources)
                assert !traffExam.waitExam(direction).hasTraffic()
            }
        }

        and: "Counter on the flowLoop rules are increased"
        getFlowLoopRules(switchPair.src.dpId)*.packetCount.every { it > 0 }

        when: "Delete flowLoop"
        def deleteResponse = northboundV2.deleteFlowLoop(flow.flowId)

        then: "The delete flowLoop response contains the flowId"
        deleteResponse.flowId == flow.flowId

        and: "FlowLoop is really deleted"
        !northboundV2.getFlow(flow.flowId).loopSwitchId

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) {
            assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP
        }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "FlowLoop rules are deleted from the src switch"
        Wrappers.wait(RULES_DELETION_TIME) {
            assert getFlowLoopRules(switchPair.src.dpId).empty
        }

        and: "The src switch is valid"
        northbound.validateSwitch(switchPair.src.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        def switchIsValid = true

        and: "Flow allows traffic"
        withPool {
            [exam.forward, exam.reverse].eachParallel { direction ->
                def resources = traffExam.startExam(direction)
                direction.setResources(resources)
                assert traffExam.waitExam(direction).hasTraffic()
            }
        }

        cleanup: "Delete the flow"
        flow && flowHelperV2.deleteFlow(flow.flowId)
        !switchIsValid && [switchPair.src.dpId, switchPair.dst.dpId].each {
            northbound.synchronizeSwitch(it, true)
        }

        where:
        data << [[
                         flowDescription: "pinned",
                         switchPair     : { List<SwitchId> switchIds ->
                             getSwPairConnectedToTraffGenForSimpleFlow(switchIds)
                         },
                         flowTap        : { FlowRequestV2 fl -> fl.pinned = true }
                 ],
                 [
                         flowDescription: "default",
                         switchPair     : { List<SwitchId> switchIds ->
                             getSwPairConnectedToTraffGenForSimpleFlow(switchIds)
                         },
                         flowTap        : { FlowRequestV2 fl ->
                             fl.source.vlanId = 0
                             fl.destination.vlanId = 0
                         }
                 ],
                 [
                         flowDescription: "protected",
                         switchPair     : { List<SwitchId> switchIds ->
                             getSwPairConnectedToTraffGenForProtectedFlow(switchIds)
                         },
                         flowTap        : { FlowRequestV2 fl -> fl.allocateProtectedPath = true }
                 ],
                 [
                         flowDescription: "vxlan",
                         switchPair     : { List<SwitchId> switchIds ->
                             getSwPairConnectedToTraffGenForVxlanFlow(switchIds)
                         },
                         flowTap        : { FlowRequestV2 fl -> fl.encapsulationType = FlowEncapsulationType.VXLAN }
                 ],
                 [
                         flowDescription: "qinq",
                         switchPair     : { List<SwitchId> switchIds ->
                             getSwPairConnectedToTraffGenForQinQ(switchIds)
                         },
                         flowTap        : { FlowRequestV2 fl ->
                             fl.source.vlanId = 10
                             fl.source.innerVlanId = 100
                             fl.destination.vlanId = 20
                             fl.destination.innerVlanId = 200
                         }
                 ]
        ]
    }

    @Tidy
    @Tags(LOW_PRIORITY)
    def "Able to delete a flow with created flowLoop on it"() {
        given: "A active multi switch flow"
        def switchPair = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)

        when: "Create flowLoop on the dst switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.dst.dpId))

        then: "FlowLoop is created on the dst switch"
        def flowLoopOnSwitch = northboundV2.getFlowLoop(switchPair.dst.dpId)
        flowLoopOnSwitch.size() == 1
        with(flowLoopOnSwitch[0]) {
            it.flowId == flow.flowId
            it.switchId == switchPair.dst.dpId
        }

        and: "FlowLoop rules are created"
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            assert getFlowLoopRules(switchPair.dst.dpId).size() == 2
        }

        and: "Flow history contains info about flowLoop"
        def flowHistory = northbound.getFlowHistory(flow.flowId).last()
        //bug
//        !flowHistory.dumps.find { it.type == "stateBefore" }.loopSwitchId
//        flowHistory.dumps.find { it.type == "stateAfter" }.loopSwitchId


        when: "Delete the flow"
        flowHelperV2.deleteFlow(flow.flowId)
        def flowIsDeleted = true

        then: "FlowLoop rules are deleted from the dst switch"
        Wrappers.wait(RULES_DELETION_TIME) {
            assert getFlowLoopRules(switchPair.dst.dpId).empty
        }

        and: "The dst switch is valid"
        northbound.validateSwitch(switchPair.src.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        def testIsCompleted = true

        cleanup: "Delete the flow"
        flow && !flowIsDeleted && flowHelperV2.deleteFlow(flow.flowId)
        !testIsCompleted && [switchPair.src.dpId, switchPair.dst.dpId].each {
            northbound.synchronizeSwitch(it, true)
        }
    }

    @Ignore("flowLoop for singleSwitchFlow is not implemented")
    @Tidy
    @Tags(LOW_PRIORITY)
    def "Able to create flowLoop for a singleSwitch flow"() {
        given: "An active singleSwitch flow"
        def sw = topology.activeSwitches.first()
        def flow = flowHelperV2.singleSwitchFlow(sw)
        flowHelperV2.addFlow(flow)

        when: "Create flowLoop on the sw switch"
        def createResponse = northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(sw.dpId))

        then: "Create flowLoop response contains flowId and src switchId"
        assert createResponse.flowId == flow.flowId
        assert createResponse.switchId == sw.dpId

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "FlowLoop is really created on the switch"
        def flowLoopOnSwitch = northboundV2.getFlowLoop(flow.flowId)
        flowLoopOnSwitch.size() == 1
        with(flowLoopOnSwitch[0]) {
            it.flowId == flow.flowId
            it.switchId == sw.dpId
        }

        and: "FlowLoop rules are created"
        //bug
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            assert getFlowLoopRules(sw.dpId).size() == 2
        }

        and: "The switch is valid"
        //there is a bug, flow_loop is in excess
        northbound.validateSwitch(sw.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])

        when: "Delete flowLoop"
        northboundV2.deleteFlowLoop(flow.flowId)

        then: "FlowLoop is deleted"
        !northboundV2.getFlow(flow.flowId).loopSwitchId

        and: "FlowLoop rules are deleted from the switch"
        assert getFlowLoopRules(sw.dpId).empty

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "The switch is valid"
        northbound.validateSwitch(sw.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        def testIsCompleted = true

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
        !testIsCompleted && northbound.synchronizeSwitch(sw.dpId, true)
    }

    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop twice on the same flow"() {
        given: "An active multi switch flow with created flowLoop on the src switch"
        def switchPair = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.src.dpId))
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP }

        when: "Try to create flowLoop on the dst switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.dst.dpId))

        then: "Human readable error is returned"
        def exc = thrown(HttpClientErrorException)
        exc.statusCode == HttpStatus.BAD_REQUEST
        with(exc.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Can't change loop switch"
        }

        and: "FlowLoop is still present for the src switch"
        northboundV2.getFlow(flow.flowId).loopSwitchId == switchPair.src.dpId

        and: "No extra rules are created on the src/dst switches"
        //bug with rule
        getFlowLoopRules(switchPair.src.dpId).size() == 2
        getFlowLoopRules(switchPair.dst.dpId).empty

        and: "The src/dst switches are valid"
        //there is probably a bug, flow_loop is in excess
        [switchPair.src, switchPair.dst].each {
            northbound.validateSwitch(it.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        }

        when: "Delete the flow with created flowLoop"
        northboundV2.deleteFlow(flow.flowId)
        def flowIsDeleted = true

        then: "FlowLoop rules are deleted"
        Wrappers.wait(RULES_DELETION_TIME) {
            assert getFlowLoopRules(switchPair.src.dpId).empty
        }
        def testIsCompleted = true

        cleanup:
        !flowIsDeleted && flowHelperV2.deleteFlow(flow.flowId)
        if (!testIsCompleted) {
            [switchPair.src, switchPair.dst].each { northbound.synchronizeSwitch(it.dpId, true) }
        }
    }

    @Ignore("flowLoop for singleSwitchFlow is not implemented")
    @Tidy
    def "Able to create flowLoop for a singleSwitchSinglePort flow"() {
        given: "An active singleSwitchSinglePort flow"
        def sw = topology.activeSwitches.first()
        def flow = flowHelperV2.singleSwitchSinglePortFlow(sw)
        flowHelperV2.addFlow(flow)

        when: "Create flowLoop on the sw switch"
        def createResponse = northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(sw.dpId))

        then: "Create flowLoop response contains flowId and src switchId"
        assert createResponse.flowId == flow.flowId
        assert createResponse.switchId == sw.dpId

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "FlowLoop is really created on the switch"
        northbound.getFlow(flow.flowId).loopSwitchId == sw.dpId

        and: "FlowLoop rules are created"
        //bug
//    Wrappers.wait(RULES_INSTALLATION_TIME) {
//        assert northbound.getSwitchRules(sw.dpId).flowEntries.findAll {
//            new Cookie(it.cookie).getType() == CookieType.FLOW_LOOP
//        }.size() == 2
//    }

        and: "The switch is valid"
        //there is a bug, flow_loop is in excess
//      northbound.validateSwitch(sw.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])

        when: "Delete the flow with created flowLoop"
        flowHelperV2.deleteFlow(flow.flowId)
        def flowIsDeleted = true

        then: "FlowLoop rules are deleted from the switch"
        assert northbound.getSwitchRules(sw.dpId).flowEntries.findAll {
            new Cookie(it.cookie).getType() == CookieType.FLOW_LOOP
        }.empty

        and: "The switch is valid"
        northbound.validateSwitch(sw.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        def testIsCompleted = true

        cleanup: "Delete the flow"
        !flowIsDeleted && flowHelperV2.deleteFlow(flow.flowId)
        !testIsCompleted && northbound.synchronizeSwitch(sw.dpId, true)
    }

    @Tidy
    def "System is able to reroute a flow when flowLoop is created on it"() {
        given: "A multi switch flow with one alternative path at least"
        def switchPair = topologyHelper.getAllNeighboringSwitchPairs().find {
            it.paths.unique(false) {
                a, b -> a.intersect(b) == [] ? 1 : 0
            }.size() >= 2
        }
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)
        def flowPath = PathHelper.convert(northbound.getFlowPath(flow.flowId))

        and: "FlowLoop is created on the dst switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.dst.dpId))

        when: "Fail a flow ISL (bring switch port down)"
        def allFlowPaths = switchPair.paths
        def flowIsls = pathHelper.getInvolvedIsls(flowPath)
        Set<Isl> altFlowIsls = allFlowPaths.findAll { it != flowPath }.collectMany { pathHelper.getInvolvedIsls(it) }
        def islToFail = flowIsls.find { !(it in altFlowIsls) }
        antiflap.portDown(islToFail.srcSwitch.dpId, islToFail.srcPort)

        then: "The flow was rerouted"
        Wrappers.wait(rerouteDelay + WAIT_OFFSET) {
            assert northboundV2.getFlowStatus(flow.flowId).status == FlowState.UP
            assert PathHelper.convert(northbound.getFlowPath(flow.flowId)) != flowPath
        }

        and: "Flow is UP and valid"
        Wrappers.wait(WAIT_OFFSET) { assert northbound.getFlowStatus(flow.flowId).status == FlowState.UP }
        northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }

        and: "FlowLoop is still present on the dst switch"
        northboundV2.getFlow(flow.flowId).loopSwitchId == switchPair.dst.dpId

        and: "FlowLoop rules ar still present on the dst switch"
        getFlowLoopRules(switchPair.dst.dpId).size() == 2

        and: "The src switch is valid"
        //there is a bug, flow_loop is in excess
        northbound.validateSwitch(switchPair.src.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])

        cleanup: "Revive the ISL back (bring switch port up) and delete the flow"
        flow && flowHelperV2.deleteFlow(flow.flowId)
        islToFail && antiflap.portUp(islToFail.srcSwitch.dpId, islToFail.srcPort)
        Wrappers.wait(discoveryInterval + WAIT_OFFSET) {
            assert northbound.getActiveLinks().size() == topology.islsForActiveSwitches.size() * 2
        }
        database.resetCosts()
    }

    @Ignore("wait fix for sycnSwitch missing flowLoop rules")
    @Tidy
    def "System is able to detect and sync missing flowLoop rules"() {
        given: "An active flow with created flowLoop on the src switch"
        def switchPair = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.src.dpId))
        def flowLoopRules
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            flowLoopRules = getFlowLoopRules(switchPair.src.dpId)*.cookie
            assert flowLoopRules.size() == 2
        }

        when: "Delete flowLoop rules"
        flowLoopRules.each { northbound.deleteSwitchRules(switchPair.src.dpId, it) }

        then: "System detects missing flowLoop rules"
        northbound.validateSwitch(switchPair.src.dpId).rules.missing.sort() == flowLoopRules.sort()

        when: "Sync the src switch"
        def syncResponse = northbound.synchronizeSwitch(switchPair.src.dpId, true)

        then: "Sync response contains flowLoop rules into the installed section"
        syncResponse.rules.installed.sort() == flowLoopRules.sort()

        then: "FlowLoop rules are synced"
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            assert getFlowLoopRules(switchPair.src.dpId).size() == 2
        }
        northbound.validateSwitch(switchPair.src.dpId).rules.missing.empty
        def testIsCompleted = true

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
        !testIsCompleted && northbound.synchronizeSwitch(switchPair.src.dpId, true)

    }

    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop when a switch is deactivated"() {
        given: "An active flow"
        def switchPair = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)

        and: "Deactivated the src switch"
        def blockData = switchHelper.knockoutSwitch(switchPair.src, RW)

        when: "Try to create flowLoop on th src switch(deactivated)"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.src.dpId))

        then: "Human readable error is returned"
        def e = thrown(HttpClientErrorException)
        e.statusCode == HttpStatus.BAD_REQUEST
        with(e.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Source switch $switchPair.src.dpId is not connected to the controller"
        }

        and: "FlowLoop is not created"
        !northbound.getFlow(flow.flowId).loopSwitchId

        when: "Try to create flowLoop on th dst switch(activated)"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.dst.dpId))

        then: "Human readable error is returned" //system can't update the flow when it is down
        def exc = thrown(HttpClientErrorException)
        exc.statusCode == HttpStatus.BAD_REQUEST
        with(exc.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Source switch $switchPair.src.dpId is not connected to the controller"
        }

        then: "FlowLoop is not created"
        !northbound.getFlow(flow.flowId).loopSwitchId

        and: "FlowLoop rules are not created on the dst switch"
        getFlowLoopRules(switchPair.dst.dpId).empty

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
        switchHelper.reviveSwitch(switchPair.src, blockData, true)
    }

    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop on the src switch when it is already created on the dst switch"() {
        given: "An active flow with created flowLoop on the src switch"
        def switchPair = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.src.dpId))

        when: "Try to create flowLoop on the dst switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.dst.dpId))

        then: "FlowLoop is not created on the dst switch"
        def exc2 = thrown(HttpClientErrorException)
        exc2.statusCode == HttpStatus.BAD_REQUEST
        with(exc2.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Can't change loop switch"
        }

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
    }

    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop on a transit switch"() {
        given: "An active multi switch flow with transit switch"
        def switchPair = topologyHelper.getNotNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(switchPair)
        flowHelperV2.addFlow(flow)
        def transitSwId = PathHelper.convert(northbound.getFlowPath(flow.flowId))[1].switchId

        when: "Try to create flowLoop on the transit switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(transitSwId))

        then: "Human readable error is returned" //system can't update the flow when it is down
        def exc = thrown(HttpClientErrorException)
        exc.statusCode == HttpStatus.BAD_REQUEST
        with(exc.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Loop switch is not terminating in flow path"
        }

        then: "FlowLoop is not created"
        !northbound.getFlow(flow.flowId).loopSwitchId

        and: "FlowLoop rules are not created on the dst switch"
        getFlowLoopRules(transitSwId).empty

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
    }

    @Ignore("flowLoop for singleSwitchFlow is not implemented")
    @Tidy
    @Tags(LOW_PRIORITY)
    def "Systems allows to get all flowLoops that goes through a switch"() {
        given: "Two active switches"
        def switchPair = topologyHelper.getNeighboringSwitchPair()

        and: "A simple multi switch flow"
        def simpleFlow = flowHelperV2.randomFlow(switchPair)
        simpleFlow.allocateProtectedPath = true
        flowHelperV2.addFlow(simpleFlow)

        and: "A single switch flow on the first active switch"
        def allowedPorts = topology.getAllowedPortsForSwitch(switchPair.src).findAll {
            it != simpleFlow.source.portNumber
        }
        def r = new Random()
        def singleFlow = flowHelperV2.singleSwitchFlow(switchPair.src)
        singleFlow.source.portNumber = allowedPorts[r.nextInt(allowedPorts.size())]
        singleFlow.destination.portNumber = allowedPorts[r.nextInt(allowedPorts.size())]
        flowHelperV2.addFlow(singleFlow)

        when: "Get all flowLoops from the src switch"
        then: "There is no flowLoop because it is not created yet"
        northboundV2.getFlowLoop(switchPair.src.dpId).empty

        when: "Create flowLoop for both flows on the same(src) switch"
        northboundV2.createFlowLoop(simpleFlow.flowId, new FlowLoopPayload(switchPair.src.dpId))
        northboundV2.createFlowLoop(singleFlow.flowId, new FlowLoopPayload(switchPair.src.dpId))

        and: "Get all flowLoops from the src switch"
        def flowLoopSrcSw = northboundV2.getFlowLoop(switchPair.src.dpId)

        then: "The created flowLoops are returned in the response list from the src switch"
        flowLoopSrcSw.size() == 2
        flowLoopSrcSw*.flowId.sort() == [simpleFlow, singleFlow]*.flowId.sort()
        flowLoopSrcSw*.switchId.unique() == [switchPair.src.dpId]

        when: "Get all flowLoops from the dst switch"
        def flowLoopDstSw = northboundV2.getFlowLoop(switchPair.dst.dpId)

        then: "Only simpleFlow is in the response list from the dst switch"
        flowLoopDstSw.size() == 1
        flowLoopDstSw[0].flowId == simpleFlow.flowId
        flowLoopDstSw[0].switchId == switchPair.src.dpId

        cleanup: "Delete the flows"
        [simpleFlow, singleFlow].each { flowHelperV2.deleteFlow(it.flowId) }
    }

    @Tidy
    def "System is able to autoSwapPath for a protected flow when flowLoop is created on it"() {
        given: "Two active switches with three diverse paths at least"
        def allTraffGenSwIds = topology.activeTraffGens*.switchConnected*.dpId
        assumeTrue("Unable to find switches connected to traffGens", (allTraffGenSwIds.size() > 1))

        def switchPair = topologyHelper.getAllNotNeighboringSwitchPairs().find {
            [it.dst, it.src].every { it.dpId in allTraffGenSwIds } && it.paths.unique(false) {
                a, b -> a.intersect(b) == [] ? 1 : 0
            }.size() >= 3
        } ?: assumeTrue("No suiting switches found", false)


        and: "A protected unmetered flow with flowLoop on the src switch"
        def flow = flowHelperV2.randomFlow(switchPair).tap {
            maximumBandwidth = 0
            ignoreBandwidth = true
            allocateProtectedPath = true
        }
        flowHelperV2.addFlow(flow)
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(switchPair.src.dpId))

        when: "Break ISL on the main path (bring port down) to init auto swap"
        def flowPathInfo = northbound.getFlowPath(flow.flowId)
        def currentPath = pathHelper.convert(flowPathInfo)
        def currentProtectedPath = pathHelper.convert(flowPathInfo.protectedPath)
        def islToBreak = pathHelper.getInvolvedIsls(currentPath)[0]
        antiflap.portDown(islToBreak.srcSwitch.dpId, islToBreak.srcPort)
        def portIsDown = true

        then: "Flow is switched to protected path"
        Wrappers.wait(PROTECTED_PATH_INSTALLATION_TIME) {
            assert northboundV2.getFlowStatus(flow.flowId).status == FlowState.UP
            assert northbound.validateFlow(flow.flowId).each { direction -> assert direction.asExpected }
            assert pathHelper.convert(northbound.getFlowPath(flow.flowId)) == currentProtectedPath
        }

        and: "FlowLoop is still present on the src switch"
        northbound.getFlow(flow.flowId).loopSwitchId == switchPair.src.dpId

        and: "The src switch is valid"
        //there is a bug, flow_loop is in excess
        //    northbound.validateSwitch(switchPair.src.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])

        and: "FlowLoop rules are still exist on the src switch"
        Wrappers.wait(RULES_INSTALLATION_TIME) {
            Map<Long, Long> flowLoopsCounter = getFlowLoopRules(switchPair.src.dpId)
                    .collectEntries { [(it.cookie): it.packetCount] }
            assert flowLoopsCounter.size() == 2
            assert flowLoopsCounter.values().every { it == 0 }
        }

        when: "Send traffic via flow"
        def traffExam = traffExamProvider.get()
        def exam = new FlowTrafficExamBuilder(topology, traffExam)
                .buildBidirectionalExam(flowHelperV2.toV1(flow), 1000, 5)

        then: "Flow doesn't allow traffic, because it is grubbed by flowLoop rules"
        withPool {
            [exam.forward, exam.reverse].eachParallel { direction ->
                def resources = traffExam.startExam(direction)
                direction.setResources(resources)
                assert !traffExam.waitExam(direction).hasTraffic()
            }
        }

        and: "Counter on the flowLoop rules are increased"
        getFlowLoopRules(switchPair.src.dpId)*.packetCount.every { it > 0 }

        cleanup: "Revert system to original state"
        flow && flowHelperV2.deleteFlow(flow.flowId)
        if (!portIsDown) {
            antiflap.portUp(islToBreak.srcSwitch.dpId, islToBreak.srcPort)
            Wrappers.wait(WAIT_OFFSET + discoveryInterval) {
                assert islUtils.getIslInfo(islToBreak).get().state == IslChangeType.DISCOVERED
            }
        }
        database.resetCosts()
    }

    @Ignore("no validation")
    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop for a non existent flow"() {
        when: "Try to create flowLoop on the transit switch"
        def sw = topology.activeSwitches.first()
        northboundV2.createFlowLoop(NON_EXISTENT_FLOW_ID, new FlowLoopPayload(sw.dpId))

        then: "Human readable error is returned"
        def exc = thrown(HttpClientErrorException)
        exc.statusCode == HttpStatus.NOT_FOUND
        // no error message
        with(exc.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Loop switch is not terminating in flow path"
        }

        and: "FlowLoop rules are not created on the switch"
        getFlowLoopRules(sw.dpId).empty

        and: "The switch is valid"
        northbound.validateSwitch(sw.dpId).verifyRuleSectionsAreEmpty(["missing", "excess", "misconfigured"])
        def switchIsValid = true

        cleanup:
        !switchIsValid && northbound.synchronizeSwitch(sw.dpId, true)
    }

    @Ignore("no validation")
    @Tidy
    @Tags(LOW_PRIORITY)
    def "Unable to create flowLoop on a non existent switch"() {
        given: "An active multi switch flow"
        def swP = topologyHelper.getNeighboringSwitchPair()
        def flow = flowHelperV2.randomFlow(swP)
        flowHelperV2.addFlow(flow)

        when: "Try to create flowLoop on a non existent switch"
        northboundV2.createFlowLoop(flow.flowId, new FlowLoopPayload(NON_EXISTENT_SWITCH_ID))

        then: "Human readable error is returned"
        def exc = thrown(HttpClientErrorException)
        exc.statusCode == HttpStatus.NOT_FOUND
        // no error message
        with(exc.responseBodyAsString.to(MessageError)) {
            errorMessage == "Could not update flow"
            errorDescription == "Loop switch is not terminating in flow path"
        }

        and: "FlowLoop rules are not created for the flow"
        !northboundV2.getFlow(flow.flowId).loopSwitchId

        cleanup:
        flow && flowHelperV2.deleteFlow(flow.flowId)
    }

    def "getSwPairConnectedToTraffGenForSimpleFlow"(List<SwitchId> switchIds) {
        getTopologyHelper().getAllNotNeighboringSwitchPairs().collectMany { [it, it.reversed] }.find { swP ->
            [swP.dst, swP.src].every { it.dpId in switchIds }
        }
    }

    def "getSwPairConnectedToTraffGenForProtectedFlow"(List<SwitchId> switchIds) {
        getTopologyHelper().getAllNeighboringSwitchPairs().find {
            [it.dst, it.src].every { it.dpId in switchIds } && it.paths.unique(false) {
                a, b -> a.intersect(b) == [] ? 1 : 0
            }.size() >= 2
        }
    }

    def "getSwPairConnectedToTraffGenForVxlanFlow"(List<SwitchId> switchIds) {
        getTopologyHelper().getAllNeighboringSwitchPairs().find {
            [it.dst, it.src].every {
                it.dpId in switchIds &&
                        getNorthbound().getSwitchProperties(it.dpId).supportedTransitEncapsulation
                                .contains(FlowEncapsulationType.VXLAN)
            }
        }
    }

    def "getSwPairConnectedToTraffGenForQinQ"(List<SwitchId> switchIds) {
        return getTopologyHelper().getAllNotNeighboringSwitchPairs().find { swP ->
            [swP.dst, swP.src].every { it.dpId in switchIds } && swP.paths.findAll { path ->
                pathHelper.getInvolvedSwitches(path).every { getNorthbound().getSwitchProperties(it.dpId).multiTable }
            }
        }
    }

    def getFlowLoopRules(SwitchId switchId) {
        northbound.getSwitchRules(switchId).flowEntries.findAll {
            def hexCookie = Long.toHexString(it.cookie)
            hexCookie.startsWith("20080000") || hexCookie.startsWith("40080000")
        }
    }
}
