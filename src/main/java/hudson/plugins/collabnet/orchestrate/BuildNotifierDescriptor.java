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

	/** The default EventQ server URL. */
	private String serverUrl;

	/** The username for authentication against the EventQ or MQ server */
	private String serverUsername;

	/** The password for authentication against the EventQ or MQ server */
	private Secret serverPassword;

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
		return "Notify TeamForge EventQ when a build completes";
	}

	/** {@inheritDoc} */
	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
			throws FormException {
		serverUrl = formData.getString("serverUrl");
		serverUsername = formData.getString("serverUsername");
		serverPassword = Secret
				.fromString(formData.getString("serverPassword"));
		save();
		return super.configure(req, formData);
	}

	/**
	 * Gets the URL to the EventQ server to connect to. Used by the Jenkins
	 * configuration UI.
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * Reads the server authentication username field
	 * 
	 * @return String
	 */
	public String getServerUsername() {
		return serverUsername;
	}

	/**
	 * Reads the server authentication password field in plain text
	 * 
	 * @return String
	 */
	public String getServerPassword() {
		String plainTextPassword = Secret.toString(this.serverPassword);
		return (StringUtils.isBlank(plainTextPassword)) ? null
				: plainTextPassword;
	}

	/**
	 * Validates that the user provided a server URL.
	 *
	 * @param serverUrl
	 *            the URL provided by the user
	 * @return whether or not the validation succeeded
	 */
	public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
		return FormValidation.validateRequired(serverUrl);
	}
	
	/**
	 * Validates that the user provided a server Username.
	 *
	 * @param serverUsername
	 *            the username provided by the user
	 * @return whether or not the validation succeeded
	 */
	public FormValidation doCheckServerUsername(@QueryParameter String serverUsername) {
		return FormValidation.validateRequired(serverUsername);
	}
	
	/**
	 * Validates that the user provided a server Password.
	 *
	 * @param serverPassword
	 *            the password provided by the user
	 * @return whether or not the validation succeeded
	 */
	public FormValidation doCheckServerPassword(@QueryParameter String serverPassword) {
		return FormValidation.validateRequired(serverPassword);
	}


	/**
	 * Validates that the user provided a source key.
	 *
	 * @param sourceKey
	 *            the key provided by the user
	 * @return whether or not the validation succeeded
	 */
	public FormValidation doCheckSourceKey(@QueryParameter String sourceKey) {
		return FormValidation.validateRequired(sourceKey);
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