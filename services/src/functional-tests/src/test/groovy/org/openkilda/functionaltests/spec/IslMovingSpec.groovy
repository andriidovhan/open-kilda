package org.openkilda.functionaltests.spec

import org.openkilda.functionaltests.BaseSpecification
import org.openkilda.functionaltests.helpers.Wrappers
import org.openkilda.testing.model.topology.TopologyDefinition
import org.openkilda.testing.service.aswitch.ASwitchService
import org.openkilda.testing.service.aswitch.model.ASwitchFlow
import org.openkilda.testing.service.northbound.NorthboundService
import org.openkilda.testing.tools.IslUtils
import org.springframework.beans.factory.annotation.Autowired

import static org.junit.Assume.assumeNotNull
import static org.openkilda.messaging.info.event.IslChangeType.*

class IslMovingSpec extends BaseSpecification {
    @Autowired
    TopologyDefinition topology
    @Autowired
    IslUtils islUtils
    @Autowired
    NorthboundService northbound
    @Autowired
    ASwitchService aswitchService

    def "ISL status changes to MOVED when replugging"() {
        given: "A connected a-switch link"
        def isl = topology.islsForActiveSwitches.find {
            it.getAswitch()?.inPort && it.getAswitch()?.outPort
        }
        assert isl

        and: "A non-connected a-switch link"
        def notConnectedIsl = topology.notConnectedIsls.first()

        when: "Replug one end of connected link to the not connected one"
        def newIsl = islUtils.replug(isl, false, notConnectedIsl, true)

        then: "Replugged ISL status changes to MOVED"
        islUtils.waitForIslStatus([isl, islUtils.reverseIsl(isl)], MOVED)

        and: "New ISL becomes Discovered"
        islUtils.waitForIslStatus([newIsl, islUtils.reverseIsl(newIsl)], DISCOVERED)

        when: "Replug the link back where it was"
        islUtils.replug(newIsl, true, isl, false)

        then: "Original ISL becomes Discovered again"
        islUtils.waitForIslStatus([isl, islUtils.reverseIsl(isl)], DISCOVERED)

        and: "Replugged ISL status changes to MOVED"
        islUtils.waitForIslStatus([newIsl, islUtils.reverseIsl(newIsl)], MOVED)
    }

    def "New ISL is not getting discovered when replugging into a self-loop (same port)"() {
        given: "A connected a-switch link"
        def isl = topology.islsForActiveSwitches.find {
            it.getAswitch()?.inPort && it.getAswitch()?.outPort
        }
        assert isl

        when: "Replug one end of link into 'itself'"
        def loopedIsl = islUtils.replug(isl, false, isl, true)

        then: "Replugged ISL status changes to FAILED"
        islUtils.waitForIslStatus([isl, islUtils.reverseIsl(isl)], FAILED)

        and: "The potential self-loop ISL is not present in list of isls"
        def allLinks = northbound.getAllLinks()
        !islUtils.getIslInfo(allLinks, loopedIsl).present
        !islUtils.getIslInfo(allLinks, islUtils.reverseIsl(loopedIsl)).present

        when: "Replug the link back where it was"
        islUtils.replug(loopedIsl, true, isl, false)

        then: "Original ISL becomes Discovered again"
        islUtils.waitForIslStatus([isl, islUtils.reverseIsl(isl)], DISCOVERED)
    }

    def "New ISL is not getting discovered when adding new self-loop ISL (different port)"() {
        given: "2 a-switch links on a single switch"
        def isls = topology.isls.findAll { it.aswitch && it.srcSwitch?.active }
                .inject([:].withDefault { [] }) { r, link ->
            link.srcSwitch && r[link.srcSwitch] << link
            link.dstSwitch && r[link.dstSwitch] << islUtils.reverseIsl(link)
            r //map where key: switch, value: list of a-switch isls related to this switch
        }.find { k, v ->
            v.findAll { !it.dstSwitch }.size() > 1 //contains at least 2 not connected asw link
        }?.value as List<TopologyDefinition.Isl>
        assumeNotNull("Not able to find required switch with enough free A-switch ISLs", isls)
        def islToPlug = isls[0]
        def islToPlugInto = isls[1]

        when: "Plug an ISL between two ports on the same switch"
        def expectedIsl = islUtils.replug(islToPlug, true, islToPlugInto, true)

        then: "The potential self-loop ISL is not present in list of isls (wait for 5 sec)"
        !Wrappers.wait(5) {
            def allLinks = northbound.getAllLinks()
            islUtils.getIslInfo(allLinks, expectedIsl).present ||
                    islUtils.getIslInfo(allLinks, islUtils.reverseIsl(expectedIsl)).present
        }

        and: "Unplug the link how it was before"
        aswitchService.removeFlows(Arrays.asList(
                new ASwitchFlow(expectedIsl.aswitch.getInPort(), expectedIsl.aswitch.getOutPort()),
                new ASwitchFlow(expectedIsl.aswitch.getOutPort(), expectedIsl.aswitch.getInPort())));
    }
}