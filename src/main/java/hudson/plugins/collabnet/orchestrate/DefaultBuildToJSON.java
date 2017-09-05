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
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.SCM;
import hudson.tasks.test.AbstractTestResultAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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
import java.util.List;
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
    public JSONObject getBuildData(Run run) throws IOException {
        return getBuildData(run, null, false);
    }

    /**
     * Construct the payload of the JSON Message.
     *
     * @param build the build being reported
     * @param eventqStatus explicit status to use
     * @param excludeCommitInfo whether to exclude commit info
     * @return JSONObject representing the 'buildData' element in the message JSON
     * @throws IOException
     */
    public JSONObject getBuildData(Run run, String eventqStatus, boolean excludeCommitInfo) throws IOException {
        Run useRun = run;
        if (!(useRun instanceof AbstractBuild)) {
            Run rawBuild = getRawBuild(useRun);
            if (rawBuild != null) {
                useRun = rawBuild;
            }
        }
        JSONObject buildData;
        buildData = new JSONObject()
                .element("remote_id", String.valueOf(useRun.getNumber()))
                .element("event_time", convertTime(useRun.getTime()))
                .element("build_url", getBuildURL(useRun))
                .element("status",  getStatus(useRun, eventqStatus))
                .element("test_results", (Object) getTestResults(useRun)) // only include if not null
                ;
        if (!excludeCommitInfo) {         
            buildData = buildData.element("revisions", (Object) getRevisions(useRun)); // only include if not null
        }

        // Jenkins doesn't update duration until after the build is complete.
        long duration = (System.currentTimeMillis() - useRun.getTimeInMillis());
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
    public JSONObject getTestResults(Run run) {
        AbstractTestResultAction testResultAction = run.getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            return null;
        }

        int totalCount = testResultAction.getTotalCount();
        int passCount = totalCount - (testResultAction.getFailCount() + testResultAction.getSkipCount());

        return new JSONObject()
                .element("passed_count", passCount)
                .element("failed_count", testResultAction.getFailCount())
                .element("ignored_count", testResultAction.getSkipCount())
                .element("url", getTestResultsURL(run, testResultAction));
    }

    /**
     * Build a status JSON object from the given build
     *
     * @param run Run
     * @return JSONObject containing the type and name
     */
    public JSONObject getStatus(Run run) {
        Result result = run.getResult();
        String eventQStatus = null;
        if (result != null) {
            if (result.isBetterOrEqualTo(Result.SUCCESS)) {
                eventQStatus = "success";
            } else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
                eventQStatus = "unstable";
            } else if (result == Result.ABORTED) {
                eventQStatus = "aborted";
            } else {
                eventQStatus = "fail";
            }
        }
        return getStatus(eventQStatus);
    }

    /**
     * Determines the EventQ status and builds a status JSON object.
     *
     * @param Run run
     * @param statusNameOrType string
     * @return JSONObject containing the type and name
     */
    private JSONObject getStatus(Run run, String statusNameOrType) {
        return (statusNameOrType != null && statusNameOrType.trim().length() > 0) ?
                getStatus(statusNameOrType) : getStatus(run);
    }

    /**
     * Build a status JSON object from the given EventQ status name or type
     *
     * @param statusNameOrType string
     * @return JSONObject containing the type and name
     */
    private JSONObject getStatus(String statusNameOrType) {
        JSONObject status = new JSONObject();
        if (statusNameOrType != null && statusNameOrType.trim().length() > 0) {
            if (statusNameOrType.toLowerCase().startsWith("success")) {
                status.put("type", "SUCCESS");
                status.put("name", "Successful");
            } else if (statusNameOrType.toLowerCase().startsWith("unstable")) {
                status.put("type", "UNSTABLE");
                status.put("name", "Unstable");
            } else if (statusNameOrType.toLowerCase().startsWith("aborted")) {
                status.put("type", "ABORTED");
                status.put("name", "Aborted");
            } else {
                status.put("type", "FAILURE");
                status.put("name", "Failed");
            }
        }
        if (!status.has("type")) {
            // default is 'Successful'
            status.put("type", "SUCCESS");
            status.put("name", "Successful");
        }

        return status;
    }

    /**
     * Because Jenkins is a pain in the ass to mock...
     *
     *
     * @param run Run we're grabbing the URI for
     * @return URI for the build HTML page
     * @throws java.net.URISyntaxException
     */
    public URI getBuildURI(Run run) throws URISyntaxException {
        return new URI(run.getAbsoluteUrl());
    }

    /**
     * Generate the JSON for the revision information.
     *
     * Contains the revision number, the repository type and URL
     * @param build
     * @return
     * @throws IOException
     */
    public JSONArray getRevisions(Run run) throws IOException {
        JSONObject repositoryInfo = getRepositoryInfo(run);

        if (repositoryInfo == null) {
            return null;
        }

        JSONArray revisions = new JSONArray();
        for (ChangeLogSet.Entry entry : findChangeSet(run)) {
            JSONObject revision = new JSONObject();
            revision.putAll(repositoryInfo);
            revision.put("revision", ((Entry) entry).getCommitId());
            revisions.add(revision);
        }
        return revisions;
    }

    public JSONObject getRepositoryInfo(Run run) throws IOException {
        SCM scmServer = getSCM(run);
        if (scmServer == null) {
            return null;
        }
        String serverType = scmServer.getType();

        String repositoryType;
        URI repositoryURI;

        if ("hudson.scm.SubversionSCM".equals(serverType)) {
            repositoryType = "svn";
            repositoryURI = getSVNRepository(run);
        } else if ("hudson.plugins.git.GitSCM".equals(serverType)) {
            repositoryType = "git";
            repositoryURI = getGitRepository(run, scmServer);
        } else {
            logger.warning("Unknown repository type " + serverType);
            return null;
        }

        return new JSONObject()
                .element("repository_type", repositoryType)
                .element("repository_url", repositoryURI.toString());
    }

    protected URI getSVNRepository(Run run) throws IOException {
        // SVN plugin buries the repository in revisions->module
        JSONObject changeSet = toJson(getChangeSet(run));
        JSONObject firstRevision = changeSet.getJSONArray("revisions").getJSONObject(0);

        return stripUserAndPassword(firstRevision.getString("module"));
    }

    @SuppressWarnings("rawtypes")
    protected URI getGitRepository(Run run, SCM scm) throws IOException {
        // build data isn't an action type that you can retrieve using getAction(class)
        Action buildDataAction = getActionByClassName(run, "hudson.plugins.git.util.BuildData");

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
            JSONObject scmInfo = toJson(scm);
            JSONArray userRemoteConfigs = scmInfo.getJSONArray("userRemoteConfigs");
            repositoryString = userRemoteConfigs.getJSONObject(0).getString("url");
        }

        return stripUserAndPassword(repositoryString);
    }

    protected Action getActionByClassName(Run run, String actionName) {
        for (Action action : run.getAllActions()) {
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
    private String getBuildURL(Run run) {
        String buildURL = null;

        try {
            URI buildURI = getBuildURI(run);
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
    private String getTestResultsURL(Run run, AbstractTestResultAction testResultAction) {
        String resultsURL = null;

        try {
            URI uri = getBuildURI(run);
            URI testURI = uri.resolve(testResultAction.getUrlName());
            resultsURL = testURI.toString();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Unable to parse URL", e);
        }
        return resultsURL;
    }

    private ChangeLogSet<? extends ChangeLogSet.Entry> findChangeSet(Run run) {
        ChangeLogSet<? extends ChangeLogSet.Entry> cs = getChangeSet(run);
        if (run != null && cs.isEmptySet()) {
            return findChangeSet(run.getPreviousBuild());
        }
        return cs;
    }

    private ChangeLogSet<? extends ChangeLogSet.Entry> getChangeSet(Run run) {
        if (run == null) {
            return ChangeLogSet.createEmpty(run);
        }
        if (run instanceof AbstractBuild) {
            return ((AbstractBuild) run).getChangeSet();
        }
        if (run instanceof WorkflowRun) {
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = ((WorkflowRun) run).getChangeSets();
            if (!changeSets.isEmpty()) {
                // It is possible that a workflow run can have multiple nodes (i.e. workspaces),
                // use the last changeset assuming it is the one relevant to the pipeline step
                return changeSets.get(changeSets.size() - 1);
            }
        }
        try { // to support WorkflowRun prior to workflow-job 2.12
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = 
                    (List) run.getClass().getMethod("getChangeSets").invoke(run);
            if (changeSets != null && !changeSets.isEmpty()) {
                for (ChangeLogSet<? extends ChangeLogSet.Entry> clEntry : changeSets) {
                    if (!clEntry.isEmptySet()) {
                        return clEntry;
                    }
                }
            }
        } catch (Exception e) { // something weird like ExternalRun
            // ignore
        }
        return ChangeLogSet.createEmpty(run);
    }

    private SCM getSCM(Run build) {
        SCM scmServer = null;
        if (build.getParent() instanceof AbstractProject) {
            scmServer = ((AbstractProject)build.getParent()).getScm();
        }
        else if (build.getParent() instanceof WorkflowJob) {
                WorkflowJob parentWJ = (WorkflowJob) build.getParent();
                scmServer = parentWJ.getTypicalSCM();
        }
        if (scmServer == null) {
            logger.warning("Failed to get repository info for " + build.getId());
        }
        return scmServer;
    }

    private Run getRawBuild(Run build) {
        Run rawBuild = null;
        try {
            rawBuild = Run.fromExternalizableId(build.getExternalizableId());
        }
        catch(Exception e) {
            logger.warning("Failed to get raw build for " + (build == null ? "???" : build.getNumber()));
            // ignore
        }
        return rawBuild;
    }
}
