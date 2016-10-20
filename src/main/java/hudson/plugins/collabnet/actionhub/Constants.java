package hudson.plugins.collabnet.actionhub;

public class Constants {
	public static final String PLUGIN_DISPLAY_NAME = "CollabNet ActionHub Plugin";

	public static final String RABBIT_EXCHANGE_TYPE = "topic";
	public static final String CONTENT_TYPE_UTF_8 = "UTF-8";
	public static final String RABBIT_TIME_TO_LIVE_KEY = "x-message-ttl";
	public static final int RABBIT_TIME_TO_LIVE_VALUE = 60000;
	public static final String RABBIT_CONNECTION_NAME = "CollabNetActionHub";
	static final int[] RABBIT_CONNECTION_RETRY_INTERVALS = {1,1,2,3,5,8,13};

	public static final String REQUEST_JSON_WORKFLOW_ID = "workflowId";
	public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS = "workflowArguments";
	public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS_NAME = "name";
	public static final String REQUEST_JSON_WORKFLOW_ARGUMENTS_VALUE = "value";
	public static final String REQUEST_JSON_RULE_INFO = "ruleInformation";
	public static final String REQUEST_JSON_RULE_INFO_NAME = "ruleName";

	public static final String JENKINS_CONFIG_ERROR_MSG_HOST = "Please set a host name for where the rabbit mq instance is running.";
	public static final String JENKINS_CONFIG_ERROR_MSG_PORT = "Please set a port for where the rabbit mq instance is running.";
	public static final String JENKINS_CONFIG_ERROR_MSG_EXCHANGE = "Please set an exchange for where the rabbit mq instance is listening.";
	public static final String JENKINS_CONFIG_ERROR_MSG_ROUTING_KEY_WF = "Please input the workflow routing key.";
	public static final String JENKINS_CONFIG_ERROR_MSG_ROUTING_KEY_ACTIONS = "Please input the actions routing key.";
	
	
	public static class ParamDataTypes {
		public static final String STRING = "string";
		public static final String ENUM = "enum";
	}


	public static class TestJsonFiles {
		public static final String WORKFLOW = "workflow_request.json";
		public static final String ACTIONS_REQUEST = "actions_request.json";
		public static final String ACTIONS_RESPONSE = "actions_response.json";
	}

	public static class TestProject {
		// these values should correspond to the json in the test files above
		public static final String ONE = "TestProject1";
		public static final String TWO = "TestProject2";
	}

}
