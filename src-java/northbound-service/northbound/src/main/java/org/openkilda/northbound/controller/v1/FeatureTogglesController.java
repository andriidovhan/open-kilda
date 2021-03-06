/* Copyright 2017 Telstra Open Source
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

package org.openkilda.northbound.controller.v1;

import org.openkilda.messaging.model.system.FeatureTogglesDto;
import org.openkilda.northbound.controller.BaseController;
import org.openkilda.northbound.service.FeatureTogglesService;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for toggle existed feature in kilda without having to re-deploy code.
 */
@RestController
@RequestMapping("/v1/features")
@PropertySource("classpath:northbound.properties")
public class FeatureTogglesController extends BaseController {

    @Autowired
    private FeatureTogglesService featureTogglesService;

    @PatchMapping
    @ApiOperation(value = "Toggle kilda features", response = FeatureTogglesDto.class)
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<FeatureTogglesDto> toggleFeatures(@RequestBody FeatureTogglesDto request) {
        return featureTogglesService.toggleFeatures(request);
    }

    @GetMapping
    @ApiOperation(value = "Get states of feature toggles", response = FeatureTogglesDto.class)
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<FeatureTogglesDto> getFeatureTogglesState() {
        return featureTogglesService.getFeatureTogglesState();
    }
}
