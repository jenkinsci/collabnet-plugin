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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class BuildNotifierDescriptor extends BuildStepDescriptor<Publisher> {

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
        return "Notify TeamForge when a build completes";
    }

    /** {@inheritDoc} */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
        throws FormException {
        save();
        return super.configure(req, formData);
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
