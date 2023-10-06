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
import hudson.plugins.collabnet.util.Helper;
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
 * Notifies TeamForge when a build is complete. This is job-specific
 * configuration.
 */
public class BuildNotifier extends Notifier {

    /** The CTF project and credentials for traceability */
    private String ctfUrl;
    private String ctfUser;
    private Secret ctfPassword;

    private PushNotification pushNotification;

    /** Prefix for messages appearing in the console log, for readability */
    private static String LOG_MESSAGE_PREFIX = "TeamForge Build Notifier - ";

    private String url;
    private String username;
    private Secret password;
    private boolean override_auth = true;
    private boolean useAssociationView = false;
    private boolean supportWebhook = false;
    private String webhookUrl;
    private String webhookUsername;
    private Secret webhookPassword;

    /**
     * Creates a new BuildNotifier. Arguments are automatically supplied when
     * the job configuration is read from the configuration file.
     *
     * @param associationView 
     *            The Association View
     * @param webhook
     *            The TeamForge webhook info
     */
    @DataBoundConstructor
    public BuildNotifier(OptionalAssociationView associationView,
                         OptionalWebhook webhook) {
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
        if(webhook != null){
            this.webhookUrl = webhook.webhookUrl;
            this.webhookUsername = webhook.webhookUsername;
            this.webhookPassword = webhook.webhookPassword;
            this.setSupportWebhook(true);
        }
    }

    public String getWebhookUsername() {
        return webhookUsername;
    }

    public Secret getWebhookPassword() {
        return webhookPassword;
    }

    public static class OptionalAssociationView {
        private ConnectionFactory connectionFactory;

        @DataBoundConstructor
        public OptionalAssociationView(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }
    }

    public static class OptionalWebhook{

        private String webhookUrl;
        private String webhookUsername;
        private Secret webhookPassword;

        @DataBoundConstructor
        public OptionalWebhook(String webhookUrl, String webhookUsername, String webhookPassword) {
            this(webhookUrl, webhookUsername, Secret.fromString(webhookPassword));
        }

        public OptionalWebhook(String webhookUrl, String webhookUsername, Secret webhookPassword) {
            this.webhookUrl = webhookUrl;
            this.webhookUsername = webhookUsername;
            this.webhookPassword = webhookPassword;
        }

        public String getWebhookUrl(){
            return this.webhookUrl;
        }

        public String getWebhookUsername(){
            return this.webhookUsername;
        }

        public String getWebhookPassword(){
            return Secret.toString(this.webhookPassword);
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

    public OptionalWebhook getWebhook(){
        if(this.getSupportWebhook()){
            return new OptionalWebhook(getWebhookUrl(), getWebhookUsername(), getWebhookPassword());
        }
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
                           BuildListener listener)
            throws InterruptedException, IOException {

        // logging setup
        PrintStream consoleLogger = listener.getLogger();

        try {
            if(getSupportWebhook() == true){
                pushNotification = new PushNotification();
                pushNotification.handle(build, getWebhook(), listener,
                        null, false);
            }

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

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean getSupportWebhook() {
        return supportWebhook;
    }

    public void setSupportWebhook(boolean supportWebhook) {
        this.supportWebhook = supportWebhook;
    }
}
