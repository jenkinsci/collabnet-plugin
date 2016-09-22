package hudson.plugins.collabnet.orchestrate;

import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.util.DescribableList;
import org.apache.commons.validator.routines.UrlValidator;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;

/**
 * Created by sureshk on 08/01/16.
 */
public class TraceabilityAction implements Action {
    private final transient AbstractBuild build;

    private static final String BLANK_TFURL = "TeamForge site URL is missing. Please enter valid TeamForge site URL";

    private static final String BLANK_CREDENTIALS = "TeamForge username or password is missing. Please enter valid TeamForge user credentials.";

    private static final String INVALID_TFURL = "TeamForge site URL is invalid";

    private static final String EMPTY_STRING = "";

    Logger logger = Logger.getLogger(getClass().getName());

    private String errorMessage;

    public TraceabilityAction(AbstractBuild abstractBuild) {
        this.build = abstractBuild;
    }

    @Override
    public String getIconFileName() {
        if (getNotifier() != null) {
            return "/plugin/collabnet/images/48x48/cloud.png";
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (getNotifier() != null) {
            return "TeamForge Associations";
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        return "teamforge";
    }

    public AbstractBuild getBuild() {
        return this.build;
    }

    private BuildNotifier getNotifier() {
        DescribableList publisherList = build.getProject().getPublishersList();
        return (BuildNotifier) publisherList.get(BuildNotifier.class);
    }

    public String getCtfUrl() {
        return getNotifier().getCtfUrl();
    }

    public String getCtfUser() {
        return getNotifier().getCtfUser();
    }

    public String getCtfPassword() {
        return getNotifier().getCtfPassword();
    }

    public String getSourceKey() {
        return getNotifier().getSourceKey();
    }

    public String getReqUrl() {
        String ctfUrl = this.getCtfUrl();
        return (ctfUrl + "/orc/api/2/reporting");
    }
    
    public boolean getAssociationView(){
    	return getNotifier().getUseAssociationView();
    }

    public boolean getValidation() {
    	if(!getAssociationView()){
    		addErrorMsg("TeamForge Association view not configured.");
    		return true;
    	}
        String ctfUrl = this.getCtfUrl();
        String ctfUserName = this.getCtfUser();
        String ctfPassword = this.getCtfPassword();
        if (ctfUrl.isEmpty()) {
            addErrorMsg(BLANK_TFURL);
            return true;
        } else if ((ctfUserName.isEmpty()) || (ctfPassword == null)) {
            addErrorMsg(BLANK_CREDENTIALS);
            return true;
        } else {
            String soapSession = this.getSoapSessionId();
            if (soapSession.isEmpty()) {
                return true;
            }
            return false;
        }
    }

    public void addErrorMsg(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMsg() {
        return errorMessage;
    }

    public String getSoapSessionId() {
        String ctfUrl = this.getCtfUrl();
        String ctfUserName = this.getCtfUser();
        String ctfPassword = this.getCtfPassword();
        try {
            URL url = getUrl(ctfUrl);
            validateCtfUrl(url);
            return getCtfSession(url, ctfUserName, ctfPassword);
        } catch (MalformedURLException e) {
            addErrorMsg(INVALID_TFURL);
            logger.log(Level.INFO, INVALID_TFURL, e);
            return EMPTY_STRING;
        } catch (URISyntaxException e) {
            addErrorMsg(INVALID_TFURL);
            logger.log(Level.INFO, INVALID_TFURL, e);
            return EMPTY_STRING;
        } catch (RemoteException e) {
            String errMsg = e.getMessage();
            if (errMsg.startsWith("(301)Moved Permanently")) {
                addErrorMsg("Connection refused. Please check the configuration.");
            } else if (errMsg.startsWith("(301)Redirect")) {
                addErrorMsg("Connection refused. Please check the configuration.");
            } else if (errMsg
                    .startsWith("; nested exception is: \n\tjava.net.UnknownHostException:")) {
                addErrorMsg("TeamForge configuration is invalid");
            } else if (errMsg
                    .startsWith("; nested exception is: \n\tjavax.net.ssl.SSLHandshakeException: ")) {
                addErrorMsg("SSL configuration error. Please check your SSL certificate and retry.");
            } else if (errMsg
                    .endsWith("java.net.SocketTimeoutException: connect timed out")) {
                addErrorMsg("Connection timed out. Please check the configuration.");
            } else if (errMsg
                    .endsWith("java.net.ConnectException: Connection timed out")) {
                addErrorMsg("Connection timed out. Please check the configuration.");
            } else if (errMsg
                    .endsWith("java.net.NoRouteToHostException: No route to host")) {
                addErrorMsg("Connection refused. Please check the configuration.");
            } else if (errMsg.endsWith(": Connection refused")) {
                addErrorMsg("Connection refused. Please check the configuration.");
            } else {
                addErrorMsg(e.getLocalizedMessage());
            }
            logger.log(Level.INFO,
                    "TeamForge Associations - " + e.getLocalizedMessage(), e);
            return EMPTY_STRING;
        }
    }

    private URL getUrl(String ctfUrl) throws URISyntaxException,
            MalformedURLException {
        return new URL(ctfUrl);
    }

    private String getCtfSession(URL ctfUrl, String ctfUserName,
            String ctfPassword) throws RemoteException {

        ICollabNetSoap cnWebService = (ICollabNetSoap) ClientSoapStubFactory
                .getSoapStub(ICollabNetSoap.class, String.format(
                        "%s://%s/ce-soap60/services/CollabNet",
                        ctfUrl.getProtocol(), ctfUrl.getHost()));

        return cnWebService.login(ctfUserName, ctfPassword);
    }

    private void validateCtfUrl(URL ctfUrl) throws URISyntaxException {
        UrlValidator urlValidator = new UrlValidator(ALLOW_LOCAL_URLS);
        if (!urlValidator.isValid(ctfUrl.toString())) {
            throw new URISyntaxException("URISyntaxException", "Invalid url");
        }
    }
}
