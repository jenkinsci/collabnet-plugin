package hudson.plugins.collabnet.actionhub;

import hudson.model.*;

import java.util.HashMap;

public class BuildCause extends Cause {

    private final String queueName;
    private final String ruleName;
    private final String userName;
    private final String projectName;
    private final String projectId;

    public BuildCause(String queueName, HashMap<String, String> ruleInformation) {
        this.queueName = queueName;

        ruleName = ruleInformation.get(Constants.REQUEST_JSON_RULE_INFO_NAME);
        userName = ruleInformation.get(Constants.REQUEST_JSON_USER_NAME);
        projectName = ruleInformation.get(Constants.REQUEST_JSON_PROJECT_NAME);
        projectId = ruleInformation.get(Constants.REQUEST_JSON_PROJECT_ID);

    }

    @Override
    public String getShortDescription() {
        return "Triggered by ActionHub user '" + userName + "' using rule '" + ruleName + "' from the " + projectName + " project (" + projectId + ")  via remote build message from queue: " + queueName;
    }

}