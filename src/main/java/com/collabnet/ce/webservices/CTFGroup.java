package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFGroup extends CTFObject implements ObjectWithTitle {
    private final String fullName, description;

    static Logger logger = Logger.getLogger(CTFGroup.class.getName());

    private static final String GROUPS_URL = "/ctfrest/foundation/v1/groups";

    CTFGroup(CollabNetApp app, JSONObject data) {
        super(app,data.get("id").toString());
        this.fullName = data.get("fullname").toString();
        this.description = data.get("description").toString();
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Alias for {@link #getFullName()}.
     */
    @Override
    public String getTitle() {
        return getFullName();
    }

    /**
     * Adds the user to the this group.
     */
    public void addMember(CTFUser u) throws IOException {
        String end_point =  app.getServerUrl() + GROUPS_URL + getId() + "/members/" + u.getUserName();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPut put = new HttpPut(end_point);
            put.setHeader("Accept", "application/json");
            put.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(put);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error while adding a member to the group - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }
}
