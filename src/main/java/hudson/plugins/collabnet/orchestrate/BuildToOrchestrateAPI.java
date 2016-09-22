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

import java.io.IOException;

/**
 * Defines how to convert build data to EventQ API format.
 */
public interface BuildToOrchestrateAPI {

    /**
     * Converts the given build to the format used by the EventQ API.
     *
     * @param sourceKey the key to use to identify this server to the EventQ API.
     * @return the JSON formatted data
     * @throws IOException if an error occurs converting the build to a JSON object
     */
    public String toOrchestrateAPI(AbstractBuild build, String sourceKey) throws IOException;

}
