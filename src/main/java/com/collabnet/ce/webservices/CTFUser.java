package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
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

    static Logger logger = Logger.getLogger(CTFUser.class.getName());

    Helper helper = new Helper();

    CTFUser(CollabNetApp app, JSONObject data) {
        super(app,data.get("id").toString());
        this.userName = data.get("username").toString();
        this.fullName = data.get("fullname").toString();
        this.email = data.get("email").toString();
    }

    private JSONObject data() throws IOException {
        if (userData == null) {
            String end_point = app.getServerUrl() + CTFConstants.USERS_URL + getId();
            Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
            String result = response.readEntity(String.class);
            int status = response.getStatus();
            if (status == 200) {
                try {
                    userData = (JSONObject) new JSONParser().parse(result);
                } catch (ParseException e) {
                    logger.log(Level.WARNING, "Unable to parse the json content in data() - " + e.getLocalizedMessage(), e);
                }
            } else {
                logger.log(Level.WARNING, "Error getting the user data - " + status + ", Error Msg - " + result);
                throw new IOException("Error getting the user data - " + status + ", Error Msg - " + helper.getErrorMessage(result));
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

    private CTFList<CTFGroup> getUserGroupListForUser() throws IOException {
        CTFList<CTFGroup> groups = new CTFList<CTFGroup>();
        String end_point =  app.getServerUrl() + CTFConstants.USERS_URL + "by-username/" + userName + "/groups";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        groups.add(app.getGroupByTitle(jsonObject.get("fullname").toString()));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getUserGroupListForUser() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the group list - " + status + ", Error Msg - " + result);
            throw new IOException("Error getting the group list - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return groups;
    }
}
