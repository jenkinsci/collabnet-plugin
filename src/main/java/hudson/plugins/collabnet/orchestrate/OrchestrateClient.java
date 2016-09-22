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

import java.io.IOException;

/**
 * Communicates with the Orchestrate server.
 */
public interface OrchestrateClient {

    /**
     * Posts the build information to the given Orchestrate server.
     *
     * @param serverUrl The Orchestrate server to post to.
     * @param serverUsername The Orchestrate server auth username. When null, no credentials are submitted.
     * @param serverPassword The Orchestrate server auth password
     * @param buildInformation the build data to post.
     */
    void postBuild(String serverUrl, String serverUsername, String serverPassword, String buildInformation) throws IOException;

}
