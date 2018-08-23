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

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class BuildNotifierDescriptor extends BuildStepDescriptor<Publisher> {

    /** The default WEBR server URL. */
    private String webhookUrl;

    /**
     * Creates a new plugin descriptor.
     */
    public BuildNotifierDescriptor() {
        super(BuildNotifier.class);
        load();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "Notify TeamForge WEBR when a build completes";
    }

    /** {@inheritDoc} */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
        throws FormException {
        webhookUrl = formData.getString("webhookUrl");
        save();
        return super.configure(req, formData);
    }

    /**
     * Gets the URL to the WEBR server to connect to. Used by the Jenkins
     * configuration UI.
     */
    public String getWebhookUrl() {
        return webhookUrl;
    }

   /**
     * Validates that the user provided a webhook URL.
     *
     * @param webhookUrl
     *            the URL provided by the user
     * @return whether or not the validation succeeded
     */
    public FormValidation doCheckWebhookUrl(@QueryParameter String webhookUrl) {
        return FormValidation.validateRequired(webhookUrl);
    }

    /**
     * @return true if there is auth data that can be inherited.
     */
    public boolean canInheritAuth() {
        return getTeamForgeShareDescriptor().useGlobal();
    }

    public static TeamForgeShare.TeamForgeShareDescriptor getTeamForgeShareDescriptor() {
        return TeamForgeShare.getTeamForgeShareDescriptor();
    }
}
