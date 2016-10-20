package hudson.plugins.collabnet.actionhub;

import hudson.model.*;

public class BuildCause extends Cause {

    private final String queueName;
    private final String ruleName;

    public BuildCause(String queueName, String ruleName) {
        this.queueName = queueName;
        this.ruleName = ruleName;
    }

    @Override
	public String getShortDescription() {
        return "Triggered by rule: " + ruleName + " via remote build message from queue: " + queueName;
    }

}