package hudson.plugins.collabnet.actionhub;

public class Constants {
    public static final String PLUGIN_DISPLAY_NAME = "CollabNet ActionHub Plugin";

    public static final String RABBIT_EXCHANGE_TYPE = "topic";
    public static final String CONTENT_TYPE_UTF_8 = "UTF-8";
    public static final String NOT_AVAILABLE = "N/A";
    public static final String RABBIT_TIME_TO_LIVE_KEY = "x-message-ttl";
    public static final int RABBIT_TIME_TO_LIVE_VALUE = 60000;
    public static final String RABBIT_CONNECTION_NAME = "CollabNetActionHub";
    static final int[] RABBIT_CONNECTION_RETRY_INTERVALS = {0, 1, 1, 2, 3, 5, 8, 13};

    public static final String REQUEST_JSON_REQUEST_TYPE = "requestType";
    public static final String REQUEST_JSON_MESSAGE_TYPE = "messageType";
    public static final String REQUEST_JSON_WORKFLOW_ID = "workflowId";
    public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS = "workflowArguments";
    public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS_NAME = "name";
    public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS_VALUE = "value";
    public static final String REQUEST_JSON_RULE_INFO = "ruleInformation";
    public static final String REQUEST_JSON_RULE_INFO_NAME = "ruleName";
    public static final String REQUEST_JSON_USER_NAME = "userName";
    public static final String REQUEST_JSON_PROJECT_NAME = "projectName";
    public static final String REQUEST_JSON_PROJECT_ID = "projectId";
    public static final String REQUEST_JSON_MATCH_COUNT = "matchCount";

    public static final String RESPONSE_JSON_OK = "{\"OK\"}";
    public static final String RESPONSE_JSON_JENKINS_CONFIG_URL_PATH = "configure";

    public static final String JENKINS_CONFIG_ERROR_MSG_HOST = "Please set a host name for where the rabbit mq instance is running.";
    public static final String JENKINS_CONFIG_ERROR_MSG_PORT = "Please set a port for where the rabbit mq instance is running.";
    public static final String JENKINS_CONFIG_ERROR_USERNAME = "Enter a username to the rabbit mq instance.";
    public static final String JENKINS_CONFIG_ERROR_PASSWORD = "Enter a password for the username above.";
    public static final String JENKINS_CONFIG_ERROR_MSG_EXCHANGE = "Please set an exchange for where the rabbit mq instance is listening.";
    public static final String JENKINS_CONFIG_ERROR_MSG_ROUTING_KEY_WF = "Please input the workflow routing key.";
    public static final String JENKINS_CONFIG_ERROR_MSG_ROUTING_KEY_ACTIONS = "Please input the actions routing key.";


    public static class RequestType {
        public static final String HEARTBEAT = "Heartbeat";
        public static final String GET_ACTIONS = "GetActions";
    }

    public static class ActionMessageType {
        public static final String MANUAL = "MANUAL";
        public static final String BUILD = "BUILD";
        public static final String REVIEW = "REVIEW";
        public static final String COMMIT = "COMMITCTF";
        public static final String WORKITEM = "WORKITEM";
        public static final String CUSTOM = "XDS";
    }

    public static class RadioButtonState {
        public static final String ALL = "allinclude";
        public static final String NONE = "noinclude";
        public static final String CUSTOM = "custominclude";
    }

    public static class ParamDataTypes {
        public static final String STRING = "string";
        public static final String ENUM = "enum";
    }


    public static class TestJson {
        public static final String WORKFLOW = "{\n" +
                "   \"workflowId\":\"TestProject1\",\n" +
                "   \"workflowName\":\"TestProject1\",\n" +
                "   \"workflowArguments\":[\n" +
                "      {\n" +
                "         \"name\":\"TestParameter2\",\n" +
                "         \"value\":\"two\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\":\"TestParameter1\",\n" +
                "         \"value\":\"value1\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"originalMessage\":\"{}\",\n" +
                "   \"senderInformation\":{\n" +
                "      \"port\":8187,\n" +
                "      \"hostName\":\"nshah-p50\",\n" +
                "      \"senderType\":\"Manual\"\n" +
                "   },\n" +
                "   \"ruleInformation\":{\n" +
                "      \"ruleId\":14,\n" +
                "      \"ruleUUID\":\"52c9ed4c-af42-490d-90d6-752e43c09dc1\",\n" +
                "      \"ruleName\":\"jenkinsTest\",\n" +
                "      \"userName\":\"admin\",\n" +
                "      \"processedTime\":\"Tue Oct 11 08:31:44 UTC 2016\",\n" +
                "      \"projectId\":\"proj1010\",\n" +
                "      \"projectName\":\"cnertest\",\n" +
                "      \"matchCount\":1\n" +
                "   }\n" +
                "}";


        public static final String ACTIONS_REQUEST = "{\"requestType\":\"GetActions\",\"messageType\":\"Manual\"}";
        public static final String ACTIONS_RESPONSE = "[{\"name\":\"TestProject1\",\"id\":\"TestProject1\"},{\"name\":\"TestProject2\",\"id\":\"TestProject2\"}]";


    }

    public static class TestProject {
        // these values should correspond to the json in the test files above
        public static final String ONE = "TestProject1";
        public static final String TWO = "TestProject2";
    }

}
