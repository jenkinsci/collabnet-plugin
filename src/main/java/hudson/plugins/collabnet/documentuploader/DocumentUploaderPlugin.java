package hudson.plugins.collabnet.documentuploader;

import hudson.plugins.collabnet.CollabNetPlugin;

import hudson.tasks.BuildStep;

/**
 * Entry point of the document uploader plugin.
 *
 */
public class DocumentUploaderPlugin extends CollabNetPlugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(CNDocumentUploader.DESCRIPTOR);
    }
}
