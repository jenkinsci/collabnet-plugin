package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
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

    static Logger logger = Logger.getLogger(CTFRole.class.getName());

    Helper helper = new Helper();

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
        String end_point =  app.getServerUrl() + CTFConstants.ROLE_URL + getId() + "/members";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFUser(app, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getMembers() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the members of a role - " + status + ", Error Msg - " + result);
            throw new IOException("Error getting the members of a role - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    /**
     * Grants this role to the given user.
     */
    public void grant(String username) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.ROLE_URL + getId() + "/members/" + username;
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.PUT, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            logger.log(Level.INFO, username + " successfully added to the role");
        } else {
            logger.log(Level.WARNING, "Error while adding a member to the role - " + status +  ", Error Msg - " + result);
            throw new IOException("Error while adding a member to the role - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }

    public void grant(CTFUser u) throws IOException {
        grant(u.getUserName());
    }
}
