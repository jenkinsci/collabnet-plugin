package hudson.plugins.collabnet.actionhub;

import hudson.model.*;

public class BuildCause extends Cause {

    private final String queueName;
    private final String ruleName;
    private final String userName;

    public BuildCause(String queueName, String ruleName, String userName) {
        this.queueName = queueName;
        this.ruleName = ruleName;
        this.userName = userName;
    }

    @Override
    public String getShortDescription() {
        return "Triggered by ActionHub user '" + userName + "' using rule '" + ruleName + "' via remote build message from queue: " + queueName;
    }

}