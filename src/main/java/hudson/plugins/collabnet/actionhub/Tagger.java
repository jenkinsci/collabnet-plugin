package hudson.plugins.collabnet.actionhub;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.collabnet.share.TeamForgeShare;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

public class Tagger extends JobProperty {

    private boolean globalOverride = false;
    private String includeRadio = null;
    private boolean manual = false;
    private boolean workitem = false;
    private boolean build = false;
    private boolean review = false;
    private boolean commit = false;
    private boolean custom = false;
    private String customMessages = null;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public Tagger(boolean globalOverride,
                  String includeRadio,
                  boolean manual,
                  boolean workitem,
                  boolean build,
                  boolean review,
                  boolean commit,
                  boolean custom,
                  String customMessages) {
        this.globalOverride = globalOverride;
        this.includeRadio = includeRadio;
        this.manual = manual;
        this.workitem = workitem;
        this.build = build;
        this.review = review;
        this.commit = commit;
        this.custom = custom;
        this.customMessages = customMessages;
    }

    public boolean isGlobalOverride(){
        return globalOverride;
    }

    public String getIncludeRadio() {
        return includeRadio;
    }

    public boolean isManual() {
        return manual;
    }

    public boolean isWorkitem(){
        return workitem;
    }

    public boolean isBuild() {
        return build;
    }

    public boolean isReview(){
        return review;
    }

    public boolean isCommit() {
        return commit;
    }

    public boolean isCustom(){
        return custom;
    }

    public String getCustomMessages() {
        return customMessages;
    }

    public boolean respondsTo(String requestMessage) {

        return Util.respondsTo(includeRadio, manual, build, commit, workitem, review, custom, customMessages, requestMessage);

    }



    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends JobPropertyDescriptor {


        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }


        public boolean isApplicable(Class<? extends Job> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "ActionHub";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req, formData);
        }

    }
}

