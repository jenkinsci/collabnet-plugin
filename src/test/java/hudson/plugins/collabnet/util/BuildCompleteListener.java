package hudson.plugins.collabnet.util;

import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;

import hudson.model.listeners.RunListener;

/**
 * This class will wait for a build to complete for a particular
 * job and then return to the caller.
 */
public class BuildCompleteListener <R extends Run> extends RunListener<R> {
    private Job job = null;
    private int buildNumber = 0;

    public BuildCompleteListener(Class<R> targetType, Job job) {
        this(targetType, job, 0);
    }

    public BuildCompleteListener(Class<R> targetType, Job job, 
                                 int buildNumber) {
        super(targetType);
        this.job = job;
        this.buildNumber = buildNumber;
    }

    public void waitForBuildToComplete(int max_wait) {
        RunListener.all().add(this);
        synchronized(this) {
            // check that the build is not already complete
            Run lastRun = this.job.getLastCompletedBuild();
            if (lastRun != null) {
                int lastNumber = lastRun.getNumber();
                if (lastNumber >= this.buildNumber) {
                    return;
                }
            }
            try {
                this.wait(max_wait);
            } catch (InterruptedException ie) {
                return;
            }
        }
        this.unregister();
    }

    @Override
    public void onCompleted(R r, TaskListener listener) {
        synchronized(this) {
            Job j = r.getParent();
            if (j == job) {
                if (r.getNumber() >= this.buildNumber) {
                    this.notify();
                }
            }
        }
    }


}
