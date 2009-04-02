package hudson.plugins.collabnet.filerelease;

import hudson.plugins.collabnet.CollabNetPlugin;

import hudson.tasks.BuildStep;

/**
 * Entry point of the file release plugin.
 *
 */
public class FileReleasePlugin extends CollabNetPlugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(CNFileRelease.DESCRIPTOR);
    }
}
