package hudson.plugins.collabnet;

import hudson.plugins.collabnet.auth.AuthPlugin;
import hudson.plugins.collabnet.documentuploader.DocumentUploaderPlugin;
import hudson.plugins.collabnet.filerelease.FileReleasePlugin;
import hudson.plugins.collabnet.pblupload.PblUploadPlugin;
import hudson.plugins.collabnet.tracker.TrackerPlugin;

import hudson.Plugin;

/**
 * Entry point for the plugins.  Initializes each sub-plugin.
 */
public class CollabNetPlugin extends Plugin {
    public void start() throws Exception {
        AuthPlugin auth = new AuthPlugin();
        auth.start();
        DocumentUploaderPlugin du = new DocumentUploaderPlugin();
        du.start();
        FileReleasePlugin fr = new FileReleasePlugin();
        fr.start();
        PblUploadPlugin pbl = new PblUploadPlugin();
        pbl.start();
        TrackerPlugin tracker = new TrackerPlugin();
        tracker.start();
        super.start();
    }
}
