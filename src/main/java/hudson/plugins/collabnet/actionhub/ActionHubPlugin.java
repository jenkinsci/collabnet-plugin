package hudson.plugins.collabnet.actionhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.Extension;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.util.FormValidation;
import hudson.model.*;
import hudson.security.*;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import jenkins.model.*;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.tools.json.JSONReader;

import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.java.Log;

/**
 * This plugin listens on Rabbit queues for messages from ActionHub.
 * It reads two types of messages- (1) get actions requests and (2) trigger build requests. 
 *
 * @author Neel Shah
 */
@Log
public class ActionHubPlugin extends Builder {

    static Channel channel = null;
    static Connection connection = null;
    static Consumer workflowMsgConsumer = null;
    static Consumer actionsMsgConsumer = null;
    static MQConnectionHandler shutDownListener = null;
    private final static Jenkins jenkins = Jenkins.getInstance();
    
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ActionHubPlugin() {
        super();
    }

    // This method is called after Jenkins is initialized and all jobs have been loaded
    // In case of an initial plugin install, it will be called right after install is completed.
    @Initializer(after=InitMilestone.JOB_LOADED)
    public static void init()  {
        boolean reConnectSuccess = true;
        int tries = 0;

        do {

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            log.info("Now going to try to connect to Rabbit.");
            reConnectSuccess = true; //assume retry succeeded

            try {
                connectAndListen();
            } catch (IOException ioe) {
                reConnectSuccess = false;
            } catch (TimeoutException te) {
                reConnectSuccess = false;
            }

            tries++;
        } while (!reConnectSuccess && tries < 5);

        if (!reConnectSuccess) {
            log.info("Unable to connect to Rabbit. Giving up. Check if Rabbit instance is up, then restart Jenkins.");
        }
    }


    public static void connectAndListen() throws IOException, TimeoutException {
        if (connection != null && connection.isOpen()) {
            if (connection.getClientProvidedName() == Constants.RABBIT_CONNECTION_NAME) {
                log.info("Closing the connection");
                connection.removeShutdownListener(shutDownListener);
                connection.close();
            }
        }

        TeamForgeShare.TeamForgeShareDescriptor descriptor = TeamForgeShare.getTeamForgeShareDescriptor();
        if (descriptor != null // possible when plugin is first installed 
                && descriptor.areActionHubSettingsValid() == true) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(descriptor.getActionHubMqHost().trim());
            factory.setPort(descriptor.getActionHubMqPort());
            factory.setUsername(descriptor.getActionHubMqUsername().trim());
            factory.setPassword(descriptor.getActionHubMqPassword().trim());
            connection = factory.newConnection(Constants.RABBIT_CONNECTION_NAME);
            shutDownListener = new MQConnectionHandler();
            connection.addShutdownListener(shutDownListener);
            channel = connection.createChannel();
            log.info("Opening a new connection with " + descriptor.getActionHubMqHost() + ":" + descriptor.getActionHubMqPort() + " on exchange " + descriptor.getActionHubMqExchange() + ". Actions Routing key is " + descriptor.getActionHubMqActionsQueue() + ". Workflow Routing key is " + descriptor.getActionHubMqWorkflowQueue());

            initWorkflowQueueListener(descriptor.getActionHubMqExchange().trim(), descriptor.getActionHubMqWorkflowQueue().trim());
            initActionsQueueListener(descriptor.getActionHubMqExchange().trim(), descriptor.getActionHubMqActionsQueue().trim());
        } else {
            log.info("Error: Unable to listen on queue. Check ActionHub connection settings.");
        }

    }

    public static void initWorkflowQueueListener(String exchange, String routingKey) throws IOException  {
        channel.exchangeDeclare(exchange, Constants.RABBIT_EXCHANGE_TYPE, true, false, false, null);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchange, routingKey);

        // The Consumer will Listen for messages
        workflowMsgConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)     {
                String message = "";
                try {
                    message = new String(body, Constants.CONTENT_TYPE_UTF_8);
                    log.info("Received : " + message + " on " + envelope.getRoutingKey());
                } catch (IOException e) {
                    log.info("Could not read workflow message from queue: " + e.getMessage());
                }

                // Parse the received message
                JSONReader jsonParser = new JSONReader();
                Map<String, Object> request = (Map<String, Object>) jsonParser.read(message);
                String passedInWorkFlowId = (String) request.get(Constants.REQUEST_JSON_WORKFLOW_ID);
                        
                if (jenkins != null) {
                    authenticate();

                    // Get the list of buildable items. This includes projects, pipelines, etc.
                    List<BuildableItem> buildableItemList = jenkins.getAllItems(BuildableItem.class);
                    boolean found = false;

                    if (buildableItemList != null) {

                        // Go through the list and see if the passedInWorkFlowId is a buildable item
                        for (BuildableItem buildItem : buildableItemList) {
                            if (buildItem.getFullName().equals(passedInWorkFlowId)) {
                                found = true;
                                boolean buildKickoff = false;
                                HashMap<String, Object> ruleInformation = (HashMap) request.get(Constants.REQUEST_JSON_RULE_INFO);
                                Cause cause = new BuildCause(envelope.getRoutingKey(), ruleInformation);

                                List<HashMap> passedInParameters = (ArrayList<HashMap>) request.get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS);
                                List<ParameterValue> parameters = new ArrayList<ParameterValue>();

                                if (passedInParameters != null) {
                                    for (HashMap passedInParameter : passedInParameters) {
                                        String name = (String) passedInParameter.get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_NAME);
                                        String value = (String) passedInParameter.get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_VALUE);
                                        parameters.add(new StringParameterValue(name, value));
                                    }
                                }

                                ParametersAction paramAction = new ParametersAction(parameters);

                                if (buildItem instanceof AbstractProject) {
                                    // This is a freestyle project. Can pass params.
                                    AbstractProject project = (AbstractProject)buildItem;
                                    buildKickoff = project.scheduleBuild(0, cause, paramAction);
                                    //Schedule a build, and return a Future object to wait for the completion of the build.
                                    //Works only with objects of type AbstractProject, not with BuildableItem.
                                    //We can use this to pass status message back to rabbitMQ
                                    //QueueTaskFuture<AbstractBuild> statusListener = p.scheduleBuild2(0);
                                    //AbstractBuild buildInfo = statusListener.get();
                                    //Result result = buildInfo.getResult();
                                    //LOG.info("Result: " + result.toString());
                                } else if (buildItem instanceof WorkflowJob) {
                                    // This is a Pipeline. Can pass params.
                                    log.info("This is a Pipeline job");

                                    WorkflowJob project = (WorkflowJob)buildItem;
                                    CauseAction causeAction = new CauseAction(cause);
                                    project.scheduleBuild2(0, paramAction, causeAction);
                                    buildKickoff = true;
                                } else {
                                    // This is a buildable item but not a freestyle project or workflow project (pipeline). Cannot pass params.
                                    buildKickoff = buildItem.scheduleBuild(0, cause);
                                }
    
                                if (buildKickoff == true) {
                                    log.info("Now building: " + buildItem.getFullName());
                                }
    
                                break;
                            } 
                        }
                        
                        if (!found) {
                            log.info("Could not find that Project.");
                        } 
                    } else {
                        log.info("The list was null. No buildable items found.");
                    }
                } else {
                    log.info("Error: Jenkins was null.");
                }
            }
        };
        
        channel.basicConsume(queueName, true, workflowMsgConsumer);
        log.info("Waiting for workflow messages ...");
    }    


    public static void initActionsQueueListener(String exchange, String routingKey) throws IOException {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(Constants.RABBIT_TIME_TO_LIVE_KEY, Constants.RABBIT_TIME_TO_LIVE_VALUE);
        channel.exchangeDeclare(exchange, Constants.RABBIT_EXCHANGE_TYPE, true, false, false, args);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchange, routingKey);
        
        // The Consumer will Listen for messages
        actionsMsgConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                String message = "";
                String jsonResponse = "";

                try {
                    message = new String(body, Constants.CONTENT_TYPE_UTF_8);
                    log.info("Received : " + message + " on " + envelope.getRoutingKey());
                } catch (IOException e) {
                    log.info("Could not read action request from queue: " + e.getMessage());
                }


                // Parse the received message
                JSONReader jsonParser = new JSONReader();
                Map<String, Object> request = (Map<String, Object>) jsonParser.read(message);
                String requestType = (String) request.get(Constants.REQUEST_JSON_REQUEST_TYPE);

                if (requestType.equals(Constants.RequestType.HEARTBEAT)) {
                    jsonResponse = Constants.RESPONSE_JSON_OK;
                } else if (requestType.equals(Constants.RequestType.GET_ACTIONS)) {
                    String messageType = (String) request.get(Constants.REQUEST_JSON_MESSAGE_TYPE);

                    Set<Job> itemList = prepareJobList(messageType);

                    List<Workflow> workflows = new ArrayList<Workflow>();

                    for (Job item : itemList) {
                        if (item instanceof BuildableItem) {
                            String buildId = item.getFullName();
                            String buildName = item.getName();
                            String buildDescription = item.getDescription();
                            String configurationUrl = item.getAbsoluteUrl() + Constants.RESPONSE_JSON_JENKINS_CONFIG_URL_PATH;

                            Map<String, WorkflowParameter> buildParametersMap = new HashMap<String, WorkflowParameter>();

                            ParametersDefinitionProperty definitions = (ParametersDefinitionProperty) item.getProperty(ParametersDefinitionProperty.class);
                            if (definitions != null) {
                                // this is a parametrized job
                                List<ParameterDefinition> parameters = definitions.getParameterDefinitions();

                                for (ParameterDefinition parameter : parameters) {
                                    String paramName = parameter.getName();
                                    String paramDescription = parameter.getDescription();
                                    String paramType = "";
                                    String[] paramChoices = new String[0];
                                    if (parameter instanceof StringParameterDefinition) {
                                        paramType = Constants.ParamDataTypes.STRING;
                                    } else if (parameter instanceof ChoiceParameterDefinition) {
                                        paramType = Constants.ParamDataTypes.ENUM;
                                        List<String> choiceList = ((ChoiceParameterDefinition) parameter).getChoices();
                                        paramChoices = choiceList.toArray(new String[choiceList.size()]);
                                    }

                                    String paramDefaultValue = "";

                                    ParameterValue defaultVal = parameter.getDefaultParameterValue();
                                    if (defaultVal != null) {
                                        Object defaultValObj = defaultVal.getValue();
                                        if (defaultValObj != null) {
                                            paramDefaultValue = defaultValObj.toString();
                                        }
                                    }

                                    WorkflowParameter paramAttributes = new WorkflowParameter(paramDescription, paramType, paramDefaultValue, paramChoices);
                                    buildParametersMap.put(paramName, paramAttributes);
                                }
                            }

                            Workflow workflow = new Workflow(buildName, buildId, buildDescription, configurationUrl, buildParametersMap);
                            workflows.add(workflow);
                        }
                    }

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                        jsonResponse = mapper.writeValueAsString(workflows);
                    } catch (JsonProcessingException e) {
                        log.info("Could not make json response: " + e.getMessage());
                    }
                }

                // Send the response back to ActionHub
                String replyto = properties.getReplyTo();
                AMQP.BasicProperties props = new AMQP.BasicProperties();
                try {
                    log.info("Replying on " + replyto + " with response: " + jsonResponse);
                    channel.basicPublish("", replyto, props, jsonResponse.getBytes(Constants.CONTENT_TYPE_UTF_8));
                } catch (Exception e) {
                    log.info("Could not process return json: " + e.getMessage());
                }
            }
        };

        channel.basicConsume(queueName, true, actionsMsgConsumer);
        log.info("Waiting for actions requests ...");
    }
    
    private static void authenticate() {
        //We will need to authenticate the plugin before we can read jenkins items 
        GrantedAuthority[] authorities = new GrantedAuthority[]{SecurityRealm.AUTHENTICATED_AUTHORITY};
        Authentication authentication = new UsernamePasswordAuthenticationToken("ActionHubPlugin", "", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Set<Job> prepareJobList (String requestMessage) {
        Set<Job> returnList = new HashSet<Job>();
        TeamForgeShare.TeamForgeShareDescriptor descriptor = TeamForgeShare.getTeamForgeShareDescriptor();

        boolean respondToGetActionsMessage = Util.respondsTo(
                descriptor.getActionHubMsgIncludeRadio(),
                descriptor.isActionHubMsgManual(),
                descriptor.isActionHubMsgBuild(),
                descriptor.isActionHubMsgCommit(),
                descriptor.isActionHubMsgWorkitem(),
                descriptor.isActionHubMsgReview(),
                descriptor.isActionHubMsgCustom(),
                descriptor.getActionHubMsgCustomTxt(),
                requestMessage);

        if (jenkins != null) {
            authenticate();
            // Get the list of jobs to send back to ActionHub. This includes projects, pipelines, etc.
            List<Job> itemList = jenkins.getAllItems(Job.class);

            if (itemList != null) {

                if (respondToGetActionsMessage) {
                    // put everything in the return list first
                    for (Job item : itemList) {
                        returnList.add(item);
                    }
                }

                for (Job item : itemList) {
                    //log.info(item.getName() + " -- " + item.getClass().toString());

                    // apply per-job overrides
                    Map<JobPropertyDescriptor, JobProperty> props = item.getProperties();

                    for (JobProperty value : props.values()) {
                        if (value instanceof Tagger) {
                            Tagger tagger = (Tagger) value;

                            if (tagger.isGlobalOverride()) {
                                if (tagger.respondsTo(requestMessage)) {
                                    returnList.add(item);
                                } else {
                                    returnList.remove(item);
                                }
                            }

                        }
                    }
                }
            }
        } else {
            log.info("Sorry Jenkins was null.");
        }

        return returnList;
    }


    // Overridden for better type safety.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link ActionHubPlugin}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            // Setting this to false as this is plugin is not meant to run as part of a build
            return false;
        }

        @Override
        public String getDisplayName() {
            return Constants.PLUGIN_DISPLAY_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();

            return super.configure(req, formData);
        }

    }
}

