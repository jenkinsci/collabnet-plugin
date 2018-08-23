package hudson.plugins.collabnet.orchestrate;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.util.Helper;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;
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
 * Converts a build to JSON for importing as a build activity into WEBR.
 */
public class BuildEvent {
    static Logger logger = Logger.getLogger("hudson.plugins.collab.orchestrate");

    public static JSONObject constructJson(Run build, TaskListener listener, String status, boolean excludeCommitInfo) throws
            IOException {
        JSONObject response = getBuildData(build, listener, status, excludeCommitInfo);
        return response;
    }

    public static JSONObject toJson(Object object) throws IOException {
        Writer writer = new StringWriter();
        Model model = new ModelBuilder().get(object.getClass());
        model.writeTo(object, Flavor.JSON.createDataWriter(object, writer));
        writer.close();
        return JSONObject.fromObject(writer.toString());
    }

    public static JSONObject getBuildData(Run run, TaskListener listener, String status, boolean excludeCommitInfo)
            throws
            IOException {
        Run useRun = run;
        if (!(useRun instanceof AbstractBuild)) {
            Run rawBuild = getRawBuild(useRun);
            if (rawBuild != null) {
                useRun = rawBuild;
            }
        }
        JSONObject buildData;
        buildData = new JSONObject()
                .element("buildId", String.valueOf(useRun.getNumber()))
                .element("eventTime", convertTime(useRun.getTime()))
                .element("buildUrl", getBuildURL(useRun))
                .element("status",  getStatus(useRun, status))
                .element("createdBy",updateStartedBy(run))
                .element("testResult", (Object) getTestResults(useRun)) // only include if not null
        ;
        if (!excludeCommitInfo) {
            buildData = buildData.element("repository", (Object) getRevisions(useRun, listener)); // only include if not null
        }

        // Jenkins doesn't update duration until after the build is complete.
        long duration = (System.currentTimeMillis() - useRun.getTimeInMillis());
        long duration_s = Math.round(duration / 1000.0);

        buildData.put("duration", String.valueOf(duration_s));
        return buildData;
    }

    private static String updateStartedBy(Run run) {
        //TODO: Need to handle this better
        Cause.UpstreamCause upstream = (Cause.UpstreamCause) run.getCause(Cause.UpstreamCause.class);
        if (upstream != null) {
            return "upstream project " + upstream.getUpstreamProject()
                    + " build number "+ upstream.getUpstreamBuild();
        }
        SCMTrigger.SCMTriggerCause scmTrigger = (SCMTrigger.SCMTriggerCause) run.getCause(SCMTrigger
                .SCMTriggerCause.class);
        if(scmTrigger!=null){
            String description = scmTrigger.getShortDescription();
            return description.replaceFirst("((.+?(?:y)))","").trim();
        }
        Cause.UserIdCause user = (Cause.UserIdCause) run.getCause(Cause.UserIdCause.class);
        if(user!=null) {
            return user.getUserName();
        }
        return null;
    }

    public static URI stripUserAndPassword(String repositoryString) {
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

    public static String convertTime(Date time) {
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
        return dateTimeFormatter.print(time.getTime());
    }

    public static JSONObject getTestResults(Run run) {
        AbstractTestResultAction testResultAction = run.getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            return null;
        }

        int totalCount = testResultAction.getTotalCount();
        int passCount = totalCount - (testResultAction.getFailCount() + testResultAction.getSkipCount());

        return new JSONObject()
                .element("passedCount", passCount)
                .element("failedCount", testResultAction.getFailCount())
                .element("ignoredCount", testResultAction.getSkipCount())
                .element("url", getTestResultsURL(run, testResultAction));
    }

    public static String getStatus(Run run) {
        Result result = run.getResult();
        String status = null;
        if (result != null) {
            if (result.isBetterOrEqualTo(Result.SUCCESS)) {
                status = "Successful";
            } else if (result.isBetterOrEqualTo(Result.UNSTABLE)) {
                status = "Unstable";
            } else if (result == Result.ABORTED) {
                status = "Aborted";
            } else {
                status = "Failed";
            }
        }
        return status;
    }

    /**
     * Determines the EventQ status and builds a status JSON object.
     *
     * @param Run run
     * @param statusName string
     * @return JSONObject containing the type and name
     */
    private static String getStatus(Run run, String statusName) {
        return (statusName != null && statusName.trim().length() > 0) ?
                getStatus(statusName) : getStatus(run);
    }

    /**
     * Build a status JSON object from the given EventQ status name or type
     *
     * @param statusName string
     * @return JSONObject containing the type and name
     */
    private static String getStatus(String statusName) {
        String status = "Successful";
        if (statusName != null && statusName.trim().length() > 0) {
            if (statusName.toLowerCase().startsWith("success")) {
                return status;
            } else if (statusName.toLowerCase().startsWith("unstable")) {
                status = "Unstable";
            } else if (statusName.toLowerCase().startsWith("aborted")) {
                status = "Aborted";
            } else {
                status = "Failed";
            }
        }
        return status;
    }


    public static URI getBuildURI(Run run) throws URISyntaxException {
        return new URI(run.getAbsoluteUrl());
    }

    public static JSONObject getRevisions(Run run, TaskListener listener) throws IOException {
        JSONObject repositoryInfo = getRepositoryInfo(run, listener);

        if (repositoryInfo == null) {
            Helper.markUnstable(run, listener.getLogger(), "SCM not configured.",
                    "hudson.plugins.collab.orchestrate.BuildEvent");
            return null;
        }

        JSONArray revisions = new JSONArray();
        for (ChangeLogSet.Entry entry : findChangeSet(run)) {
            revisions.add(entry.getCommitId());
        }
        repositoryInfo.element("revisions", revisions);
        if(revisions.size() == 0) {
            Helper.markUnstable(run, listener.getLogger(),
                    "SCM revisions not available, commit your changes and try again.",
                    "hudson.plugins.collab.orchestrate.BuildEvent");
        }
        return repositoryInfo;
    }

    public static JSONObject getRepositoryInfo(Run run, TaskListener listener) throws IOException {
        SCM scmServer = getSCM(run);
        if (scmServer == null) {
            Helper.markUnstable(run, listener.getLogger(), "SCM info not available",
                    "hudson.plugins.collab.orchestrate.BuildEvent");
            return null;
        }
        String serverType = scmServer.getType();

        String repositoryType;
        URI repositoryURI;

        if ("hudson.scm.SubversionSCM".equals(serverType)) {
            repositoryType = "svn";
            repositoryURI = getSVNRepository(run, listener);
        } else if ("hudson.plugins.git.GitSCM".equals(serverType)) {
            repositoryType = "git";
            repositoryURI = getGitRepository(run, scmServer);
        } else {
            logger.warning("Unknown repository type " + serverType);
            return null;
        }

        return new JSONObject()
                .element("type", repositoryType)
                .element("url", repositoryURI.toString());
    }

    protected static URI getSVNRepository(Run run, TaskListener listener) throws IOException {
        // SVN plugin buries the repository in revisions->module
        JSONObject changeSet = toJson(getChangeSet(run));
        if(changeSet.getJSONArray("revisions") != null){
            JSONObject firstRevision = changeSet.getJSONArray("revisions").getJSONObject(0);
            return stripUserAndPassword(firstRevision.getString("module"));
        } else {
            try {
                return stripUserAndPassword(run.getEnvironment().get("SVN_URL"));
            } catch (Exception e) {
                Helper.markUnstable(run, listener.getLogger(), "SCM info not available",
                        "hudson.plugins.collab.orchestrate.BuildEvent");
                return null;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    protected static URI getGitRepository(Run run, SCM scm) throws IOException {
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

    protected static Action getActionByClassName(Run run, String actionName) {
        for (Action action : run.getAllActions()) {
            if (actionName.equals(action.getClass().getName())) {
                return action;
            }
        }

        return null;
    }

    private static String getBuildURL(Run run) {
        String buildURL = null;
        try {
            URI buildURI = getBuildURI(run);
            buildURL = buildURI.toString();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Failed to parse build URL", e);
        } catch (NullPointerException e){
            return buildURL;
        }

        return buildURL;
    }

    private static String getTestResultsURL(Run run, AbstractTestResultAction testResultAction) {
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

    private static ChangeLogSet<? extends ChangeLogSet.Entry> findChangeSet(Run run) {
        ChangeLogSet<? extends ChangeLogSet.Entry> cs = getChangeSet(run);
        if (run != null && cs.isEmptySet()) {
            return findChangeSet(run.getPreviousBuild());
        }
        return cs;
    }

    private static ChangeLogSet<? extends ChangeLogSet.Entry> getChangeSet(Run run) {
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

    private static SCM getSCM(Run build) {
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

    private static Run getRawBuild(Run build) {
        Run rawBuild = null;
        try {
            rawBuild = Run.fromExternalizableId(build.getExternalizableId());
        }
        catch(Exception e) {
            logger.warning("Failed to get raw build for " + (build == null ? "???" : build.getNumber()));
        }
        return rawBuild;
    }
}