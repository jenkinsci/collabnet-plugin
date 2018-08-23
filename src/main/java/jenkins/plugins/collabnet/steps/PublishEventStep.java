/*
 * Copyright 2017 CollabNet, Inc.
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

package jenkins.plugins.collabnet.steps;

import hudson.plugins.collabnet.orchestrate.PushNotification;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.collect.ImmutableSet;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class PublishEventStep extends Step {

    /** The REST end-point URL to the WEBR. */
    private final String webhookUrl;

    //private String credentialsId;

    /** The flag to mark current run unstable if this step fails to notify EventQ. */
    @DataBoundSetter public boolean markUnstable;

    /** The (optional) status to report explicitly to EventQ. */
    @DataBoundSetter public String status;

    /** The flag to control whether to exclude associated commit info in the EventQ notification. */
    @DataBoundSetter public boolean excludeCommitInfo;

    @DataBoundConstructor
    public PublishEventStep(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        PublishEventStepExecution execution = new PublishEventStepExecution(this, context);
        return execution;
    }

    /**
     * Reads the server URL to contact. Used by Jelly to render the UI template.
     *
     * @return the server URL
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }

//    public String getCredentialsId() {
//        return this.credentialsId;
//    }
//
//    @DataBoundSetter public void setCredentialsId(String credentialsId) {
//        this.credentialsId = credentialsId;
//    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "publishWEBR";
        }

        @Override
        public String getDisplayName() {
            return "Notify TeamForge WEBR";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, EnvVars.class);
        }

//        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item owner) {
//            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
//                return new StandardUsernameListBoxModel().withEmptySelection();
//            }
//            String useServerUrl = null;
//            return new StandardUsernameListBoxModel()
//                    .withEmptySelection()
//                    .withAll(PublishEventStepExecution.lookupCredentials(owner, useServerUrl));
//        }

        public ListBoxModel doFillStatusItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("-- Auto --", "");
            items.add("SUCCESS", "SUCCESS");
            items.add("UNSTABLE", "UNSTABLE");
            items.add("ABORTED", "ABORTED");
            items.add("FAILURE", "FAILURE");
            return items;
        }

        /**
         * Validates that the user provided a server URL.
         *
         * @param webhookUrl
         *            the URL provided by the user
         * @return whether or not the validation succeeded
         */
        public FormValidation doCheckWebhookUrl(@QueryParameter String webhookUrl) {
            return FormValidation.validateRequired(webhookUrl);
        }

        /**
         * Validates that the user provided EventQ status.
         *
         * @param status
         *            the status provided by the user
         * @return whether or not the validation succeeded
         */
        public FormValidation doCheckStatus(@QueryParameter String status) {
            if (isBlank(status) // empty is ok
                    || status.toLowerCase().startsWith("success")
                    || status.toLowerCase().equals("unstable")
                    || status.toLowerCase().equals("aborted")
                    || status.toLowerCase().startsWith("fail")
                    ) {
                return FormValidation.ok();
            }
            return FormValidation.error("Invalid WEBR status value");
        }
//
//        public FormValidation doCheckCredentialsId(
//                @QueryParameter final String credentialsId,
//                @AncestorInPath final Item owner) {
//            // TODO make sure that fallback credentials (i.e. TeamForge) exists if
//            // none is selected
//            return FormValidation.ok();
//        }
    }

    public static class PublishEventStepExecution extends SynchNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(PublishEventStepExecution.class.getName());

        private transient PublishEventStep step;
        private transient TaskListener listener;
        private transient Run<?,?> run;

        private transient PushNotification pushNotification;

        /** Prefix for messages appearing in the console log, for readability */
        public static String LOG_MESSAGE_PREFIX = "TeamForge Build Notifier - ";

        /** Message for invalid WEBR server URL */
        public static String LOG_MESSAGE_INVALID_URL = "The URL to the TeamForge WEBR server is missing.";

        public PublishEventStepExecution(final PublishEventStep step, @Nonnull final StepContext ctx)
                throws IOException, InterruptedException {
            super(ctx);
            this.step = step;
            this.listener = getContext().get(TaskListener.class);
            this.run = getContext().get(Run.class);
        }

        @Override
        protected Void run() throws Exception {
            PrintStream consoleLogger = this.listener.getLogger();
            pushNotification = new PushNotification();
            String webhookUrl = this.step.getWebhookUrl();
            if (isBlank(webhookUrl)) {
                markUnstable(
                        consoleLogger,
                        "Build information NOT sent: " + LOG_MESSAGE_INVALID_URL);
                return null;
            }

            try {
                pushNotification.handle(run, webhookUrl, listener, this.step.status,
                        this.step.excludeCommitInfo);
            } catch (IllegalStateException ise) {
                markUnstable(consoleLogger,
                        "Build information NOT sent: this step needs a Jenkins URL " +
                        "(go to Manage Jenkins > Configure System; click Save)");
                ise.printStackTrace(consoleLogger);

            } catch (Exception e) {
                markUnstable(consoleLogger, e.getMessage());
                log("Build information NOT sent, details below", consoleLogger);
                e.printStackTrace(consoleLogger);
            }
            return null;
        }

        /**
         * Marks the current run as unstable and logs a message.
         *
         * @param consoleLogger
         *            the logger to log to
         * @param message
         *            the message to log
         */
        private void markUnstable(PrintStream consoleLogger, String message) {
            log(message, consoleLogger);
            logger.warning(message);
            if (this.step == null || this.step.markUnstable) {
                if (this.run != null) {
                    this.run.setResult(Result.UNSTABLE);
                }
            }
        }

        /**
         * Logging helper that prepends the log message prefix
         *
         * @param msg
         * @param printStream
         */
        private void log(String msg, PrintStream printStream) {
            printStream.print(LOG_MESSAGE_PREFIX);
            printStream.println(msg);
        }


//        public StandardUsernamePasswordCredentials getCredentials() {
//            return getCredentials(this.run.getParent(),
//                    this.step.getCredentialsId(), this.step.getWebhookUrl());
//        }
//
//        public static StandardUsernamePasswordCredentials getCredentials(Item owner,
//                String credentialsId, String webhookUrl) {
//            StandardUsernamePasswordCredentials result = null;
//            if (!isBlank(credentialsId)) {
//                for (StandardUsernamePasswordCredentials c : lookupCredentials(owner, webhookUrl)) {
//                    if (c.getId().equals(credentialsId)) {
//                        result = c;
//                        break;
//                    }
//                }
//            }
//            return result;
//        }
//
//        public static List<StandardUsernamePasswordCredentials> lookupCredentials(Item owner, String webhookUrl) {
//            URIRequirementBuilder rBuilder = isBlank(webhookUrl) ?
//                    URIRequirementBuilder.create() : URIRequirementBuilder.fromUri(webhookUrl);
//            return CredentialsProvider.lookupCredentials(
//                    StandardUsernamePasswordCredentials.class, owner, null, rBuilder.build());
//        }
    }

}
