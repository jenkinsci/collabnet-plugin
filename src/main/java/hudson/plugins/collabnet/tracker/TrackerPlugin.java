package hudson.plugins.collabnet.tracker;

import hudson.plugins.collabnet.CollabNetPlugin;

import hudson.tasks.BuildStep;

/**
 * Entry point for the Tracker plugin.
 *
 */
public class TrackerPlugin extends CollabNetPlugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(CNTracker.DESCRIPTOR);
    }
}
