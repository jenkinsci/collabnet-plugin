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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Converts a Model object to JSON for importing build activity information into EventQ.
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
    public JSONObject getBuildData(AbstractBuild build) throws IOException;

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
    JSONObject getTestResults(AbstractBuild build);

    /**
     * Build a status JSON object from the given build
     *
     * @param build AbstractBuild
     * @return JSONObject containing the type and name
     */
    JSONObject getStatus(AbstractBuild build);

    /**
     * Because Jenkins is a pain in the ass to mock...
     *
     *
     * @param build AbstractBuild we're grabbing the URI for
     * @return URI for the build HTML page
     * @throws java.net.URISyntaxException
     */
    URI getBuildURI(AbstractBuild build) throws URISyntaxException;

    /**
     * Generate the JSON for the revision information.
     *
     * Contains the revision number, the repository type and URL
     * @param build
     * @return
     * @throws IOException
     */
    JSONArray getRevisions(AbstractBuild build) throws IOException;

    /**
     * Generate a partial JSON for the revision information.
     *
     * Contains the repository type and URL, but not the revision number
     * @param build
     * @return
     * @throws IOException
     */
    JSONObject getRepositoryInfo(AbstractBuild build) throws IOException;

    /**
     * Converts the standard timestamp from Java format to EventQ's format.
     *
     *
     * @param time the time to convert
     * @return the formatted time
     */
    String convertTime(Date time);
}
