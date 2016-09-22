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
import hudson.model.Action;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.SCM;
import hudson.tasks.test.AbstractTestResultAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.stapler.export.Flavor;
import org.kohsuke.stapler.export.Model;
import org.kohsuke.stapler.export.ModelBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts a build to JSON for importing as a build activity into EventQ.
 */
public class DefaultBuildToJSON implements BuildToJSON {
    Logger logger = Logger.getLogger("hudson.plugins.collab.orchestrate");

    /** {@inheritDoc} */
    public JSONObject toJson(Object object) throws IOException {
        Writer writer = new StringWriter();
        Model model = new ModelBuilder().get(object.getClass());

        model.writeTo(object, Flavor.JSON.createDataWriter(object, writer));
        writer.close();

        return JSONObject.fromObject(writer.toString());
    }

    /**
     * Construct the payload of the JSON Message.
     *
     * @param build the build being reported
     * @return JSONObject representing the 'buildData' element in the message JSON
     * @throws IOException
     */
    public JSONObject getBuildData(AbstractBuild build) throws IOException {
        JSONObject buildData;

        buildData = new JSONObject()
                .element("remote_id", String.valueOf(build.getNumber()))
                .element("event_time", convertTime(build.getTime()))
                .element("build_url", getBuildURL(build))
                .element("status", getStatus(build))
                .element("test_results", (Object) getTestResults(build)) // only include if not null
                .element("revisions", (Object) getRevisions(build)); // only include if not null

        // Jenkins doesn't update duration until after the build is complete.
        long duration = (System.currentTimeMillis() - build.getTimeInMillis());
        long duration_s = Math.round(duration / 1000.0);

        buildData.put("duration", String.valueOf(duration_s));
        return buildData;
    }

    /**
     * Convert the SCM URL to something without credentials.
     * @return URI without the username or password
     * @param repositoryString
     */
    public URI stripUserAndPassword(String repositoryString) {
        URI repositoryURI;

        try {
            URI fullURI = new URI(repositoryString);
            repositoryURI = new URI(fullURI.getScheme(), null, fullURI.getHost(), fullURI.getPort(), fullURI.getPath(), null, null);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Unable to parse URL", e);
            repositoryURI = null;
        }

        return repositoryURI;
    }

    /**
     * Converts the standard timestamp from Java format to EventQ's format.
     *
     *
     * @param time the time to convert
     * @return the formatted time
     */
    public String convertTime(Date time) {
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
        return dateTimeFormatter.print(time.getTime());
    }

    /**
     * Convert the test results into a JSONObject
     * @param build AbstractBuild
     * @return JSONObject or null if there are no results
     */
    public JSONObject getTestResults(AbstractBuild build) {
        AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            return null;
        }

        int totalCount = testResultAction.getTotalCount();
        int passCount = totalCount - (testResultAction.getFailCount() + testResultAction.getSkipCount());

        return new JSONObject()
                .element("passed_count", passCount)
                .element("failed_count", testResultAction.getFailCount())
                .element("ignored_count", testResultAction.getSkipCount())
                .element("url", getTestResultsURL(build, testResultAction));
    }

    /**
     * Build a status JSON object from the given build
     *
     * @param build AbstractBuild
     * @return JSONObject containing the type and name
     */
    public JSONObject getStatus(AbstractBuild build) {
        JSONObject status = new JSONObject();

        Result result = build.getResult();
        if (result.isBetterOrEqualTo(Result.SUCCESS)) {
            status.put("type", "SUCCESS");
            status.put("name", "Successful");
        } else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
            status.put("type", "UNSTABLE");
            status.put("name", "Unstable");
        } else if (result == Result.ABORTED) {
            status.put("type", "ABORTED");
            status.put("name", "Aborted");
        } else {
            status.put("type", "FAILURE");
            status.put("name", "Failed");
        }

        return status;
    }

    /**
     * Because Jenkins is a pain in the ass to mock...
     *
     *
     * @param build AbstractBuild we're grabbing the URI for
     * @return URI for the build HTML page
     * @throws java.net.URISyntaxException
     */
    public URI getBuildURI(AbstractBuild build) throws URISyntaxException {
        return new URI(build.getAbsoluteUrl());
    }


    /**
     * Generate the JSON for the revision information.
     *
     * Contains the revision number, the repository type and URL
     * @param build
     * @return
     * @throws IOException
     */
    public JSONArray getRevisions(AbstractBuild build) throws IOException {
        JSONObject repositoryInfo = getRepositoryInfo(build);

        if (repositoryInfo == null) {
            return null;
        }

        JSONArray revisions = new JSONArray();

        for (ChangeLogSet.Entry entry : findChangeSet(build)) {
            JSONObject revision = new JSONObject();
            revision.putAll(repositoryInfo);
            revision.put("revision", ((Entry) entry).getCommitId());

            revisions.add(revision);
        }

        return revisions;
    }

    public JSONObject getRepositoryInfo(AbstractBuild build) throws IOException {
        SCM scmServer = build.getProject().getScm();
        String serverType = scmServer.getType();

        String repositoryType;
        URI repositoryURI;

        if ("hudson.scm.SubversionSCM".equals(serverType)) {
            repositoryType = "svn";
            repositoryURI = getSVNRepository(build);
        } else if ("hudson.plugins.git.GitSCM".equals(serverType)) {
            repositoryType = "git";
            repositoryURI = getGitRepository(build);
        } else {
            logger.warning("Unknown repository type " + serverType);
            return null;
        }

        return new JSONObject()
                .element("repository_type", repositoryType)
                .element("repository_url", repositoryURI.toString());
    }

    protected URI getSVNRepository(AbstractBuild build) throws IOException {
        // SVN plugin buries the repository in revisions->module
        JSONObject changeSet = toJson(build.getChangeSet());
        JSONObject firstRevision = changeSet.getJSONArray("revisions").getJSONObject(0);

        return stripUserAndPassword(firstRevision.getString("module"));
    }

    protected URI getGitRepository(AbstractBuild build) throws IOException {
        // build data isn't an action type that you can retrieve using getAction(class)
        Action buildDataAction = getActionByClassName(build, "hudson.plugins.git.util.BuildData");

        if (buildDataAction == null) {
            logger.warning("Git plugin not found");
            return null;
        }

        // Sometime between 1.485 and 1.500 the Repository URL moved from userRemoteConfigs (which is now essentially empty)
        // to remoteUrls.  Check for both for now, but the 'else' block can go away eventually
        JSONObject actionObject = toJson(buildDataAction);
        JSONArray remoteUrls = actionObject.getJSONArray("remoteUrls");

        String repositoryString;

        if (remoteUrls != null && remoteUrls.size() > 0) {
            repositoryString = remoteUrls.getString(0);
        } else {

            logger.fine("Falling back to old repository detection.");

            SCM scm = build.getProject().getScm();
            JSONObject scmInfo = toJson(scm);
            JSONArray userRemoteConfigs = scmInfo.getJSONArray("userRemoteConfigs");
            repositoryString = userRemoteConfigs.getJSONObject(0).getString("url");
        }

        return stripUserAndPassword(repositoryString);
    }

    protected Action getActionByClassName(AbstractBuild build, String actionName) {
        for (Action action : build.getActions()) {
            if (actionName.equals(action.getClass().getName())) {
                return action;
            }
        }

        return null;
    }

    /**
     * Get the URL for the Jenkin's build summary as a string
     * @param build
     * @return
     */
    private String getBuildURL(AbstractBuild build) {
        String buildURL = null;

        try {
            URI buildURI = getBuildURI(build);
            buildURL = buildURI.toString();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Failed to parse build URL", e);
        }
        return buildURL;
    }

    /**
     * Get the URL for the Jenkin's test summary as a string
     * @param build
     * @param testResultAction
     * @return
     */
    private String getTestResultsURL(AbstractBuild build, AbstractTestResultAction testResultAction) {
        String resultsURL = null;

        try {
            URI uri = getBuildURI(build);
            URI testURI = uri.resolve(testResultAction.getUrlName());
            resultsURL = testURI.toString();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Unable to parse URL", e);
        }
        return resultsURL;
    }

    private ChangeLogSet<? extends ChangeLogSet.Entry> findChangeSet(AbstractBuild build) {
        AbstractBuild prior = build;
        while (prior != null && prior.getChangeSet().isEmptySet()) {
            prior = (AbstractBuild) prior.getPreviousBuild();
        }

        AbstractBuild theBuild = (prior == null) ? build : prior;
        return theBuild.getChangeSet();
    }
}
