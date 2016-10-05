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

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Notifies EventQ when a build is complete. This is job-specific
 * configuration.
 */
public class BuildNotifier extends Notifier {

	/** The root URL to the EventQ or MQ server. */
	private String serverUrl;

	/** The username for authentication against the EventQ or MQ server */
	private String serverUsername;

	/** The password for authentication against the EventQ or MQ server */
	private Secret serverPassword;

	/** The key to the source to publish the build to. */
	private String sourceKey;

	/** The CTF project and credentials for traceability */
	private String ctfUrl;
	private String ctfUser;
	private Secret ctfPassword;

	/** Converts builds. */
	private transient BuildToOrchestrateAPI converter;

	/** Communicates with the EventQ server. */
	private transient OrchestrateClient orchestrateClient;

	/** Prefix for messages appearing in the console log, for readability */
	private static String LOG_MESSAGE_PREFIX = "TeamForge EventQ Build Notifier - ";

	private String url;
	private String username;
	private Secret password;
	private boolean override_auth = true;
	private boolean useAssociationView = false;
	
	/**
	 * Creates a new BuildNotifier. Arguments are automatically supplied when
	 * the job configuration is read from the configuration file.
	 * 
	 * @param serverUrl
	 *            the root of the EventQ application or MQ
	 * @param serverUsername
	 *            the username for authentication against the EventQ or MQ
	 *            server
	 * @param serverPassword
	 *            the password for authentication against the EventQ or MQ
	 *            server
	 * @param sourceKey
	 *            the association key for the source to publish to
	 * @param ctfUrl
	 *            The project homepage URL
	 * @param ctfUser
	 *            User id with traceability API access
	 * @param ctfPassword
	 *            Password for the user
	 */

	@DataBoundConstructor
	public BuildNotifier(OptionalAssociationView associationView,
			String serverUrl, String serverUsername, Secret serverPassword,
			String sourceKey) {
		if (associationView != null) {
			ConnectionFactory connectionFactory = associationView.connectionFactory;
			this.override_auth = connectionFactory != null;
			if (override_auth) {
				this.url = connectionFactory.getUrl();
				this.username = connectionFactory.getUsername();
				this.password = connectionFactory.getPassword();
			}
			this.ctfUrl = this.getCollabNetUrl();
			this.ctfUser = this.getUsername();
			this.ctfPassword = Secret.fromString(this.getPassword());
			this.setUseAssociationView(true);
		} else {
			this.ctfUrl = null;
			this.setUseAssociationView(false);
		}
		this.serverUrl = serverUrl;
		this.serverUsername = serverUsername;
		this.serverPassword = serverPassword;
		this.sourceKey = sourceKey;
	}

	public static class OptionalAssociationView {
		private ConnectionFactory connectionFactory;

		@DataBoundConstructor
		public OptionalAssociationView(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}
	}

	/**
	 * @return the collabneturl for the CollabNet server.
	 */
	public String getCollabNetUrl() {
		if (this.overrideAuth()) {
			return this.url;
		} else {
			return getTeamForgeShareDescriptor().getCollabNetUrl();
		}
	}

	/**
	 * @return the username used for logging in.
	 */
	public String getUsername() {
		if (this.overrideAuth()) {
			return this.username;
		} else {
			return getTeamForgeShareDescriptor().getUsername();
		}
	}

	/**
	 * @return whether or not auth is overriden
	 */
	public boolean overrideAuth() {
		return this.override_auth;
	}

	/**
	 * @return the password used for logging in.
	 */
	public String getPassword() {
		if (this.overrideAuth()) {
			return Secret.toString(this.password);
		} else {
			return getTeamForgeShareDescriptor().getPassword();
		}
	}

	public ConnectionFactory getConnectionFactory() {
		if (this.overrideAuth())
			return new ConnectionFactory(getCollabNetUrl(), getUsername(),
					getPassword());
		return null;
	}

	/**
	 * @return the TeamForge share descriptor.
	 */
	public static TeamForgeShare.TeamForgeShareDescriptor getTeamForgeShareDescriptor() {
		return TeamForgeShare.getTeamForgeShareDescriptor();
	}

	/** {@inheritDoc} */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	/**
	 * Reads the server URL to contact. Used by Jelly to render the UI template.
	 * 
	 * @return the server URL
	 */
	public String getServerUrl() {
		return serverUrl;
	}

	/**
	 * Sets the server URL field.
	 * 
	 * @param serverUrl
	 */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
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
	 * Reads the source key that identifies this server. Used by Jelly to render
	 * the UI template.
	 * 
	 * @return the source key
	 */
	public String getSourceKey() {
		return sourceKey;
	}

	/**
	 * Sets the source key field
	 * 
	 * @param sourceKey
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * Reads the ctf project URL
	 * 
	 * @return the CTF project URL
	 */
	public String getCtfUrl() {
		return ctfUrl;
	}

	/**
	 * Sets the CTF URL
	 * 
	 * @param ctfUrl
	 */
	public void setCtfUrl(String ctfUrl) {
		this.ctfUrl = ctfUrl;
	}

	/**
	 * Reads the ctf credentials - user
	 * 
	 * @return the CTF project User
	 */
	public String getCtfUser() {
		return ctfUser;
	}

	/**
	 * Sets the CTF User
	 * 
	 * @param ctfUser
	 */
	public void setCtfUser(String ctfUser) {
		this.ctfUser = ctfUser;
	}

	/**
	 * Reads the ctf passowrd
	 * 
	 * @return What should be displayed as ctf password
	 */
	public String getCtfPassword() {
		String plainTextPassword = Secret.toString(this.ctfPassword);
		return (StringUtils.isBlank(plainTextPassword)) ? null
				: plainTextPassword;
	}

	/**
	 * Gets the plugin descriptor with its global configuration.
	 * 
	 * @return the descriptor
	 */
	@Override
	public BuildNotifierDescriptor getDescriptor() {
		return (BuildNotifierDescriptor) super.getDescriptor();
	}

	/** {@inheritDoc} */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		// logging setup
		PrintStream consoleLogger = listener.getLogger();

		if (isBlank(getServerUrl())) {
			markUnstable(
					build,
					consoleLogger,
					"Build information NOT sent: the URL to the TeamForge EventQ server is missing.");
			return true;
		}

		if (isBlank(getSourceKey())) {
			markUnstable(
					build,
					consoleLogger,
					"Build information NOT sent: the source key for the TeamForge EventQ build source is missing.");
			return true;
		}

		try {
			initialize();
			log("Sending build information using "
					+ orchestrateClient.getClass().getSimpleName(),
					consoleLogger);
			String json = converter.toOrchestrateAPI(build, getSourceKey()
					.trim());
			orchestrateClient.postBuild(getServerUrl().trim(),
					getServerUsername(), getServerPassword(), json);
			log("Build information sent", consoleLogger);
		} catch (IllegalStateException ise) {
			markUnstable(
					build,
					consoleLogger,
					"Build information NOT sent: plugin needs a Jenkins URL (go to Manage Jenkins > Configure System; click Save)");
		} catch (Exception e) {
			markUnstable(build, consoleLogger, e.getMessage());
			log("Build information NOT sent, details below", consoleLogger);
			e.printStackTrace(consoleLogger);
		}
		return true;
	}

	/**
	 * Marks the build as unstable and logs a message.
	 * 
	 * @param build
	 *            the build to mark unstable
	 * @param consoleLogger
	 *            the logger to log to
	 * @param message
	 *            the message to log
	 */
	private void markUnstable(AbstractBuild<?, ?> build,
			PrintStream consoleLogger, String message) {
		log(message, consoleLogger);
		Logger logger = Logger.getLogger(getClass().getName());
		logger.warning(message);
		build.setResult(Result.UNSTABLE);
	}

	/**
	 * Jenkins (un)helpfully wipes out any initialization done in constructors
	 * or class definitions before executing this #perform method. So we need to
	 * initialize it in case it wasn't already.
	 */
	private void initialize() throws URISyntaxException {
		if (converter == null) {
			converter = new DefaultBuildToOrchestrateAPI(
					new DefaultBuildToJSON());
		}

		if (orchestrateClient == null) {
			orchestrateClient = new AmqpOrchestrateClient();
		}
	}

	/**
	 * Sets the converter to use.
	 * 
	 * @param newConverter
	 *            the new converter
	 */
	public void setConverter(BuildToOrchestrateAPI newConverter) {
		this.converter = newConverter;
	}

	/**
	 * Sets the Orchestrate client to use.
	 * 
	 * @param client
	 *            the new client
	 */
	public void setOrchestrateClient(OrchestrateClient client) {
		this.orchestrateClient = client;
	}

	/**
	 * Returns the Orchestrate client
	 * 
	 * @return an orchestrateClient instance
	 */
	public OrchestrateClient getOrchestrateClient() {
		return this.orchestrateClient;
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

	public boolean getUseAssociationView() {
		return useAssociationView;
	}

	public void setUseAssociationView(boolean useAssociationView) {
		this.useAssociationView = useAssociationView;
	}

}
