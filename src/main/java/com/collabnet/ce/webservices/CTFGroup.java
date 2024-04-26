package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFGroup extends CTFObject implements ObjectWithTitle {
    private final String fullName, description;

    static Logger logger = Logger.getLogger(CTFGroup.class.getName());

    Helper helper = new Helper();

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
        String end_point =  app.getServerUrl() + CTFConstants.FOUNDATION_URL + "users/by-username/" + u.getUserName() +
                "/groups/" + getId();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.PUT, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            logger.log(Level.INFO, u.getUserName() + " successfully added to the group");
        } else {
            logger.log(Level.WARNING, "Error while adding a member to the group - " + status + ", Error Msg - " + result);
            throw new IOException("Error while adding a member to the group - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }
}
