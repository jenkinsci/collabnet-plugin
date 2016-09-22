/*
 * Copyright 2013 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.collabnet.orchestrate;

import hudson.model.AbstractBuild;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Converts builds to EventQ API format.
 */
public class DefaultBuildToOrchestrateAPI implements BuildToOrchestrateAPI {
    Logger logger = Logger.getLogger("hudson.plugins.collab.orchestrate");

    /**
     * creates the json build data from the build object.
     */
    BuildToJSON converter;

    /**
     * Initializes the BuildToEventQAPI object.
     * @param jsonBuilder the build to JSON converter.
     */
    public DefaultBuildToOrchestrateAPI(BuildToJSON jsonBuilder) {
        converter = jsonBuilder;
    }

    /** {@inheritDoc} */
    public String toOrchestrateAPI(AbstractBuild build, String sourceKey) throws IOException {
        JSONObject response = new JSONObject()
                .element("api_version", "1")
                .element("source_association_key", sourceKey)
                .element("build_data", converter.getBuildData(build));

        return response.toString();
    }
}
