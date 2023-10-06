package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
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

    private static final String SCM_URL = "/ctfrest/scm/v1/repositories/";

    static Logger logger = Logger.getLogger(CTFScmRepository.class.getName());

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
            String end_point = app.getServerUrl() + SCM_URL + getId() + "/releases";
            CloseableHttpClient httpClient;
            CloseableHttpResponse response;
            try {
                httpClient = CNFormFieldValidator.getHttpClient();
                HttpGet get = new HttpGet(end_point);
                get.setHeader("Accept", "application/json");
                get.setHeader("Authorization", "Bearer " + app.getSessionId());
                response = httpClient.execute(get);
                scmData = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            } catch (Exception e) {
                logger.log(Level.INFO, "Error getting the repository data" + e.getLocalizedMessage(), e);
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