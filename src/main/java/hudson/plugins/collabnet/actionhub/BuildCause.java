package hudson.plugins.collabnet.actionhub;

import hudson.model.*;

import java.util.HashMap;

public class BuildCause extends Cause {

    private final String queueName;
    private final String ruleName;
    private final String userName;
    private final String projectName;
    private final String projectId;
    private final Integer matchCount;
    private final String matchCountString;

    public BuildCause(String queueName, HashMap<String, Object> ruleInformation) {
        this.queueName = queueName;

        ruleName = (String)ruleInformation.get(Constants.REQUEST_JSON_RULE_INFO_NAME);
        userName = (String)ruleInformation.get(Constants.REQUEST_JSON_USER_NAME);

        if (ruleInformation.get(Constants.REQUEST_JSON_PROJECT_NAME) == null) {
            projectName = Constants.NOT_AVAILABLE;
        } else {
            projectName = (String) ruleInformation.get(Constants.REQUEST_JSON_PROJECT_NAME);
        }

        if (ruleInformation.get(Constants.REQUEST_JSON_PROJECT_ID) == null) {
            projectId = Constants.NOT_AVAILABLE;
        } else {
            projectId = (String) ruleInformation.get(Constants.REQUEST_JSON_PROJECT_ID);
        }

        if (ruleInformation.get(Constants.REQUEST_JSON_MATCH_COUNT) == null) {
            matchCount = 0;
            matchCountString = Constants.NOT_AVAILABLE;
        } else {
            matchCount = (Integer) ruleInformation.get(Constants.REQUEST_JSON_MATCH_COUNT);
            matchCountString = matchCount.toString();
        }
    }

    @Override
    public String getShortDescription() {
        return "Triggered when rule '" + ruleName + "' from the " + projectName + " project (" + projectId + ") was matched " + matchCountString + " time(s). Triggerred by ActionHub user '" + userName + "' via remote build message from queue "  + queueName + ".";
    }

}