package hudson.plugins.collabnet.pblupload;

import hudson.plugins.collabnet.CollabNetPlugin;

import hudson.tasks.BuildStep;

/**
 * Entry point for the PblUPloader plugin.
 */
public class PblUploadPlugin extends CollabNetPlugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(PblUploader.DESCRIPTOR);
    }
}
