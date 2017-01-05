package hudson.plugins.collabnet.actionhub;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.byteThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import com.rabbitmq.client.*;
import com.rabbitmq.tools.json.JSONReader;
import hudson.plugins.collabnet.share.TeamForgeShare;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.ExtensionList;
import hudson.model.*;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Log
public class ActionHubPluginTest {
    @Rule public JenkinsRule j = new JenkinsRule();
    Channel mockedChannel;
    AMQP.BasicProperties props;
    FreeStyleProject project1, project2;
    Envelope envelope;
    String someStr = "test";


    @Before
    public void setup() {
        try {
            TeamForgeShare.TeamForgeShareDescriptor descriptor = TeamForgeShare.getTeamForgeShareDescriptor();
            descriptor.setActionHubMsgIncludeRadio(Constants.RadioButtonState.ALL);

            // Create some test projects in Jenkins
            project1 = j.createFreeStyleProject(Constants.TestProject.ONE);
            project2 = j.createFreeStyleProject(Constants.TestProject.TWO);

            // mock rabbitMQ calls
            mockedChannel = mock(Channel.class);
            AMQP.Queue.DeclareOk declareOK = mock(AMQP.Queue.DeclareOk.class);
            when(declareOK.getQueue()).thenReturn(someStr);
            when(mockedChannel.queueDeclare()).thenReturn(declareOK);
            props = mock(AMQP.BasicProperties.class);
            when(props.getReplyTo()).thenReturn(someStr);
            envelope = mock(Envelope.class);
            when(envelope.getRoutingKey()).thenReturn(someStr);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPluginInitialize()  {
        boolean pluginInitialized = false;

        // get a list of plugin descriptors
        ExtensionList<Descriptor> extensions = Jenkins.getInstance().getExtensionList(Descriptor.class);
        
        for (Descriptor extension : extensions) {
            log.info(extension.getDisplayName());
            
            if (extension.getDisplayName() == Constants.PLUGIN_DISPLAY_NAME) {
                // our plugin has been initialized
                pluginInitialized = true;
                break;
            }
        }

        assertTrue(pluginInitialized);
    }

    @Test
    public void testReadWorkflowMsgAndTriggerBuild() {
        // Read test workflow JSON from file
        String testWorkflowJson = readtestJson(Constants.TestJsonFiles.WORKFLOW);

        try {
            // Parse the test workflow JSON
            JSONReader jsonParser = new JSONReader();
            Map<String, Object> request = (Map<String, Object>) jsonParser.read(testWorkflowJson);
            String testWorkFlowId = (String) request.get(Constants.REQUEST_JSON_WORKFLOW_ID);
            List<HashMap> passedInParameters = (ArrayList<HashMap>) request.get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS);
            int testParamCount = passedInParameters.size();
            StringParameterValue testParam1 = new StringParameterValue((String) passedInParameters.get(0).get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_NAME),
                    (String) passedInParameters.get(0).get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_VALUE));
            StringParameterValue testParam2 = new StringParameterValue((String) passedInParameters.get(1).get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_NAME),
                    (String) passedInParameters.get(1).get(Constants.REQUEST_JSON_WORKFLOW_ARGUMENTS_VALUE));

            // simulate reading a new workflow message and trigger a build
            ActionHubPlugin.channel = mockedChannel;
            ActionHubPlugin.initWorkflowQueueListener(someStr, someStr);
            ActionHubPlugin.workflowMsgConsumer.handleDelivery(someStr, envelope, props, testWorkflowJson.getBytes(Constants.CONTENT_TYPE_UTF_8));
            TimeUnit.SECONDS.sleep(10); //give it a few seconds for build to complete

            // test assertions
            FreeStyleBuild lastBuild = project1.getLastBuild();
            assertNotNull(lastBuild);
            Result lastBuildResult = lastBuild.getResult();
            List<ParametersAction> actions = lastBuild.getActions(ParametersAction.class);
            List<ParameterValue> lastBuildParameters = actions.get(0).getParameters();
            assertEquals(testParamCount, lastBuildParameters.size());
            assertTrue(lastBuildParameters.contains(testParam1));
            assertTrue(lastBuildParameters.contains(testParam2));
            assertEquals(Result.SUCCESS, lastBuildResult);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }


    @Test
    public void testReadActionsMsgAndRespond() {
        //read test JSON for getActions request and response
        String getActionsJsonTestRequest = readtestJson(Constants.TestJsonFiles.ACTIONS_REQUEST);
        String expectedGetActionsJsonResponse = readtestJson(Constants.TestJsonFiles.ACTIONS_RESPONSE);


        try {
            ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);

            // this call will simulate reading a get actions request and generate a response
            ActionHubPlugin.channel = mockedChannel;
            ActionHubPlugin.initActionsQueueListener(someStr, someStr);
            ActionHubPlugin.actionsMsgConsumer.handleDelivery(someStr, envelope, props, getActionsJsonTestRequest.getBytes(Constants.CONTENT_TYPE_UTF_8));

            // assertion
            verify(mockedChannel).basicPublish(anyString(), anyString(), (AMQP.BasicProperties)any(), argument.capture());
            String actualGetActionsJsonResponse = new String(argument.getValue(), Constants.CONTENT_TYPE_UTF_8);

            JSONReader jsonParser = new JSONReader();
            ArrayList<HashMap> expectedJobs =  (ArrayList<HashMap>)jsonParser.read(expectedGetActionsJsonResponse);
            ArrayList<HashMap> actualJobs =  (ArrayList<HashMap>)jsonParser.read(actualGetActionsJsonResponse);

            for (HashMap actualJob:actualJobs) {
                assertTrue(actualJob.containsKey("configurationUrl"));
                actualJob.remove("configurationUrl");
            }

            for (Object expectedJob:expectedJobs) {
                assertTrue(actualJobs.contains(expectedJob));
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }



    private String readtestJson(String fileName) {
        String retval = "";

        try {
            retval =  IOUtils.toString(this.getClass().getResourceAsStream(fileName), Constants.CONTENT_TYPE_UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retval;
    }

}
