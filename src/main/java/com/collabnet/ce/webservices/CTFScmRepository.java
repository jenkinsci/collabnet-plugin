package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A SCM repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFScmRepository extends CTFFolder {
    /**
     * Lazily fetched.
     */

    static Logger logger = Logger.getLogger(CTFScmRepository.class.getName());

    Helper helper = new Helper();

    public String systemId;
    public String systemTitle;
    public String repositoryDirectory;
    public String scmViewerUrl;
    public String scmAdapterName;
    public boolean idRequiredOnCommit;
    public boolean isOnManagedScmServer;

    private volatile JSONObject scmData;

    public CTFScmRepository(CTFProject parent, JSONObject data) {
        super(parent, data, data.get("id").toString(), data.get("parentId").toString());
    }

    private JSONObject data() throws IOException {
        if (scmData == null) {
            String end_point = app.getServerUrl() + CTFConstants.SCM_REPO_URL + getId();
            Map<String, String> queryParam = new HashMap<>();
            queryParam.put("includeWebhooks", "false");
            Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, queryParam);
            String result = response.readEntity(String.class);
            int status = response.getStatus();
            if (status < 300) {
                try {
                    scmData = (JSONObject) new JSONParser().parse(result);
                } catch (ParseException e) {
                    logger.log(Level.WARNING, "Unable to parse the json content in data() - " + e.getLocalizedMessage(), e);
                }
            } else {
                logger.log(Level.WARNING, "Error getting the repository data - " + status + ", Error Msg - " + result);
                throw new IOException("Error getting the repository data - " + status + ", Error Msg - " + helper.getErrorMessage(result));
            }
        }
        return scmData;
    }

    public String getSystemId() throws IOException {
        return data().get("systemId").toString();
    }

    public String getSystemTitle() throws IOException {
        return data().get("systemTitle").toString();
    }

    public String getRepositoryDirectory() throws IOException {
        return data().get("repositoryDirectory").toString();
    }

    public String getScmViewerUrl() throws IOException {
        return data().get("scmViewerUrl").toString();
    }

    public String getScmAdapterName() throws IOException {
        return data().get("scmAdapterName").toString();
    }

    public boolean getIdRequiredOnCommit() throws IOException {
        return Boolean.parseBoolean(data().get("idRequiredOnCommit").toString());
    }

    public boolean getIsOnManagedScmServer() throws IOException {
        return Boolean.parseBoolean(data().get("isOnManagedScmServer").toString());
    }
}