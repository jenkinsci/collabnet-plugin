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
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.Helper;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.*;


/**
 * Notifies WEBR when a build is complete. This is job-specific
 * configuration.
 */
public class BuildNotifier extends Notifier {

    private String webhookUrl;

    /** The CTF project and credentials for traceability */
    private String ctfUrl;
    private String ctfUser;
    private Secret ctfPassword;

    private PushNotification pushNotification;

    private String url;
    private String username;
    private Secret password;
    private boolean override_auth = true;
    private boolean useAssociationView = false;

    /**
     * Creates a new BuildNotifier. Arguments are automatically supplied when
     * the job configuration is read from the configuration file.
     *
     * @param webhookUrl
     *            the Webhook url to post build information
     * @param ctfUrl
     *            The project homepage URL
     * @param ctfUser
     *            User id with traceability API access
     * @param ctfPassword
     *            Password for the user
     */

    @DataBoundConstructor
    public BuildNotifier(OptionalAssociationView associationView, String webhookUrl) {
        if (associationView != null) {
            ConnectionFactory connectionFactory =
                associationView.connectionFactory;
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
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
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
                           BuildListener listener) {

        PrintStream consoleLogger = listener.getLogger();
        pushNotification = new PushNotification();

        try {
            //URL url = new URL(this.getCtfUrl());
            //String token = Helper.getToken(url, this.getCtfUser(), this.getCtfPassword());
            pushNotification.handle(build, getWebhookUrl(), listener, null, false);
        } catch (IllegalStateException ise) {
            Helper.markUnstable(
                         build,
                         consoleLogger,
                         "Build information NOT sent: plugin needs a Jenkins URL (go to Manage Jenkins > Configure " +
                                 "System; click Save)", getClass().getName());
        } catch (Exception e) {
            Helper.markUnstable(build, consoleLogger, e.getMessage(), getClass().getName());
            Helper.log("Build information NOT sent, details below", consoleLogger);
            e.printStackTrace(consoleLogger);
        }
        return true;
    }



    public boolean getUseAssociationView() {
        return useAssociationView;
    }

    public void setUseAssociationView(boolean useAssociationView) {
        this.useAssociationView = useAssociationView;
    }
}
