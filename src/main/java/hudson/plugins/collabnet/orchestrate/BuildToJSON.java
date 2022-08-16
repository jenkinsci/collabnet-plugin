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

import hudson.model.Run;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Converts a Model object to JSON for importing build activity information into TeamForge.
 */
public interface BuildToJSON {

    /**
     * Converts a build to a JSON string.
     *
     * @param object the model object to convert
     * @return Jenkins' representation of the build as JSON
     * @throws IOException if an error occurs converting it to JSON
     */
    public JSONObject toJson(Object object) throws IOException;

    /**
     * Construct the payload of the JSON Message.
     *
     * @param build the build being reported
     * @return JSONObject representing the 'buildData' element in the message JSON
     * @throws IOException
     */
    public JSONObject getBuildData(Run build) throws IOException;

    /**
     * Construct the payload of the JSON Message.
     *
     * @param build the build being reported
     * @param status the status to use
     * @return JSONObject representing the 'buildData' element in the message JSON
     * @throws IOException
     */
    public JSONObject getBuildData(Run run, String status, boolean excludeCommitInfo) throws IOException;     

    /**
     * Convert the SCM URL to something without credentials.
     * @return URI without the username or password
     * @param repositoryString
     */
    URI stripUserAndPassword(String repositoryString);

    /**
     * Convert the test results into a JSONObject
     * @param build AbstractBuild
     * @return JSONObject or null if there are no results
     */
    JSONObject getTestResults(Run build);

    /**
     * Build a status JSON object from the given build
     *
     * @param build AbstractBuild
     * @return JSONObject containing the type and name
     */
    JSONObject getStatus(Run build);

    /**
     * Because Jenkins is a pain in the ass to mock...
     *
     *
     * @param run Run we're grabbing the URI for
     * @return URI for the build HTML page
     * @throws java.net.URISyntaxException
     */
    URI getBuildURI(Run run) throws URISyntaxException;

    /**
     * Generate the JSON for the revision information.
     *
     * Contains the revision number, the repository type and URL
     * @param build
     * @return
     * @throws IOException
     */
    JSONArray getRevisions(Run build) throws IOException;

    /**
     * Generate a partial JSON for the revision information.
     *
     * Contains the repository type and URL, but not the revision number
     * @param build
     * @return
     * @throws IOException
     */
    JSONObject getRepositoryInfo(Run build) throws IOException;

    /**
     * Converts the standard timestamp from Java format to TeamForge's format.
     *
     *
     * @param time the time to convert
     * @return the formatted time
     */
    String convertTime(Date time);
}
