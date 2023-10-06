package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A role in CTF belongs to a project.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFRole extends CTFObject implements ObjectWithTitle {
    private final String title, description;

    private static final String ROLE_URL = "/ctfrest/foundation/v1/roles/";

    static Logger logger = Logger.getLogger(CTFRole.class.getName());

    public CTFRole(CTFProject parent, JSONObject data) {
        super(parent, data.get("id").toString());
        this.title = data.get("title").toString();
        this.description = data.get("description").toString();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the users who has this role (in the project that the role belongs to.)
     */
    public CTFList<CTFUser> getMembers() throws IOException {
        CTFList<CTFUser> r = new CTFList<CTFUser>();
        String end_point =  app.getServerUrl() + ROLE_URL + getId() + "/members";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFUser(app, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Getting the artifact details failed - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return r;
    }

    /**
     * Grants this role to the given user.
     */
    public void grant(String username) throws IOException {
        String end_point =  app.getServerUrl() + ROLE_URL + getId() + "/members/" + username;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPut put = new HttpPut(end_point);
            put.setHeader("Accept", "application/json");
            put.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(put);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error while adding a member to the role - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public void grant(CTFUser u) throws IOException {
        grant(u.getUserName());
    }
}
