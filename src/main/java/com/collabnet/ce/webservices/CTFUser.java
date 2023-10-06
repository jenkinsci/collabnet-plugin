package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFUser extends CTFObject implements ObjectWithTitle {
    private String userName;
    private String fullName;
    private String email;

    /**
     * Detailed data is obtained lazily.
     */
    private volatile JSONObject userData;

    private static final String FOUNDATION_URL = "/ctfrest/foundation/v1/";

    static Logger logger = Logger.getLogger(CTFUser.class.getName());

    CTFUser(CollabNetApp app, JSONObject data) {
        super(app,data.get("id").toString());
        this.userName = data.get("username").toString();
        this.fullName = data.get("fullname").toString();
        this.email = data.get("email").toString();
    }

    private JSONObject data() throws IOException {
        if (userData==null) {
            String end_point = app.getServerUrl() + FOUNDATION_URL + "users/" + getId();
            CloseableHttpClient httpClient = null;
            CloseableHttpResponse response = null;
            try {
                httpClient = CNFormFieldValidator.getHttpClient();
                HttpGet get = new HttpGet(end_point);
                get.setHeader("Accept", "application/json");
                get.setHeader("Authorization", "Bearer " + app.getSessionId());
                response = httpClient.execute(get);
                userData = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            } catch (Exception e) {

            } finally {
                if(response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            }
        }
        return userData;
    }

    public String getUserName() {
        return userName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Alias for {@link #getUserName()}.
     */
    @Override
    public String getTitle() {
        return userName;
    }

    public String getLocale() throws IOException {
        return data().get("locale").toString();
    }

    public String getTimeZone() throws IOException {
        return data().get("timezone").toString();
    }

    public boolean isSuperUser() throws IOException {
        return Boolean.parseBoolean(data().get("superUser").toString());
    }

    public boolean isRestrictedUser() throws IOException {
        return Boolean.parseBoolean(data().get("restrictedUser").toString());
    }

    public String getStatus() throws IOException {
        return data().get("status").toString();
    }

    /**
     * Gets the group full names that this user belongs to.
     *
     * This will only work
     * if logged in as the user in question, or if the logged in user has
     * superuser permissions.
     *
     * <p>
     * Because of the incompleteness in the API, this method cannot readily return
     * {@link CTFGroup}s.
     */
    public Set<String> getGroupNames() throws IOException {
        Set<String> groups = new HashSet<String>();
        CTFList<CTFGroup> groupList = getUserGroupListForUser();
        for (CTFGroup ctfGroup : groupList) {
            groups.add(ctfGroup.getFullName());
        }
        return groups;
    }

    public CTFList<CTFGroup> getGroups() throws IOException {
        CTFList<CTFGroup> groups = new CTFList<CTFGroup>();
        groups = getUserGroupListForUser();
        for (CTFGroup ctfGroup : groups) {
            groups.add(app.getGroupByTitle(ctfGroup.getFullName()));
        }
        return groups;
    }

    /**
     * Adds the user to the this group.
     */
    public void addTo(CTFGroup g) throws IOException {
        g.addMember(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CTFUser that = (CTFUser) o;
        return userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }

    private CTFList<CTFGroup> getUserGroupListForUser() {
        CTFList<CTFGroup> groups = new CTFList<CTFGroup>();
        String end_point = FOUNDATION_URL + "by-username/" + userName + "/groups";
        CloseableHttpClient httpClient;
        CloseableHttpResponse response;
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
                groups.add(app.getGroupByTitle(jsonObject.get("fullname").toString()));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the group list" + e.getLocalizedMessage(), e);
        }
        return groups;
    }
}
