package org.openkilda.functionaltests.extension.fixture

import org.openkilda.functionaltests.extension.spring.ContextAwareGlobalExtension
import org.openkilda.messaging.info.event.IslChangeType
import org.openkilda.model.cookie.Cookie
import org.openkilda.testing.Constants
import org.openkilda.testing.model.topology.TopologyDefinition
import org.openkilda.testing.service.floodlight.FloodlightsHelper
import org.openkilda.testing.service.northbound.NorthboundService
import org.openkilda.testing.service.northbound.NorthboundServiceV2
import org.openkilda.testing.tools.SoftAssertions

import groovy.util.logging.Slf4j
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

/**
 * Performs certain checks after every spec, tries to verify that environment is left clean.
 * This extension is meant to help to ensure that your tests have 'good' cleanups. Note that is does not guarantee a
 * fully clean env and is just another reassurance. Analyzing all aspects of the clean environment is very
 * difficult and developer should still take full responsibility for cleanup-ing all the changed resources.
 * This is turned off by default during CI builds and its main purpose is to be used during local debug. Can be switched
 * on/off by setting `cleanup.verifier` property
 */
@Slf4j
class CleanupVerifierExtension extends ContextAwareGlobalExtension {
    @Value('${cleanup.verifier}')
    boolean enabled

    @Autowired
    NorthboundService northbound
    @Autowired
    NorthboundServiceV2 northboundV2
    @Autowired
    TopologyDefinition topology
    @Autowired
    FloodlightsHelper flHelper

    @Override
    void delayedVisitSpec(SpecInfo spec) {
        if (!enabled) {
            return
        }
        spec.features.each {
            it.addInterceptor(new IMethodInterceptor() {
                @Override
                void intercept(IMethodInvocation invocation) throws Throwable {
                    invocation.proceed()
                    log.info("Running cleanup verifier for '$invocation.feature.name'")
                    assert northboundV2.getAllFlows().empty
                    northbound.getAllSwitches().each {
                        def validation = northbound.validateSwitch(it.switchId)
                        validation.verifyRuleSectionsAreEmpty()
                        validation.verifyMeterSectionsAreEmpty()
                        if (it.ofVersion == "OF_13") {
                            assert northbound.getSwitchRules(it.switchId).flowEntries.find { it.cookie == Cookie.DROP_VERIFICATION_LOOP_RULE_COOKIE }
                        }
                    }
                    def regionVerifications = new SoftAssertions()
                    flHelper.fls.forEach { fl ->
                        def expectedSwitchIds = topology.activeSwitches.findAll { fl.region in it.regions }*.dpId
                        if (!expectedSwitchIds.empty) {
                            regionVerifications.checkSucceeds {
                                assert fl.floodlightService.switches*.switchId.sort() == expectedSwitchIds.sort()
                            }
                        }
                    }
                    regionVerifications.verify()
                    northbound.getAllLinks().each {
                        assert it.state == IslChangeType.DISCOVERED
                        assert it.cost == Constants.DEFAULT_COST || it.cost == 0
                        assert it.availableBandwidth == it.maxBandwidth
                    }

                }
            })
        }
    }
}
