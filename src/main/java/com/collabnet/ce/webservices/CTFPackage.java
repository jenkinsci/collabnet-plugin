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
 * @author Kohsuke Kawaguchi
 */
public class CTFPackage extends CTFFolder {

    static Logger logger = Logger.getLogger(CTFPackage.class.getName());

    Helper helper = new Helper();

    private static final String RELEASE_STATUS_ACTIVE = "active";
    private static final String MATURITY_NONE = "";
    private String description = "";

    CTFPackage(CTFObject parent, JSONObject data) {
        super(parent, data, data.get("id").toString(), data.get("parentFolderId").toString());
    }

    /**
     * Deletes this package.
     */
    public void delete() throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.PACKAGE_URL + getId();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.DELETE, null);
        int status = response.getStatus();
        String result = response.readEntity(String.class);
        if (status >= 300) {
            logger.log(Level.WARNING, "Error while deleting a package - " + status);
            throw new IOException("Error while deleting a package - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }

    public CTFRelease createRelease(String title, String description, String status, String maturity) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.PACKAGE_URL + getId() + "/releases" ;
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        requestPayload.put("status", status);
        requestPayload.put("maturity", maturity);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFRelease(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createRelease()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a release - " + statusCode + "Error Msg - " + result);
            throw new IOException("Error creating a release - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return null;
    }

    /**
     * Finds a release by its title, or return null if not found.
     */
    public CTFRelease getReleaseByTitle(String title) throws IOException {
        String relTitle = null;
        if (title != null) {
            String val[] = title.split("/");
            relTitle = val[val.length -1 ];
        }
        for (CTFRelease p : getReleases()) {
            if (p != null) {
                if (p.getTitle().equals(relTitle))
                    return getReleaseById(p.getId());
            }
        }
        return null;
    }


    public CTFRelease getReleaseById(String releaseId) throws IOException {
        CTFRelease ctfRelease = null;
        String end_point = app.getServerUrl() + CTFConstants.RELEASE_URL + releaseId;
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfRelease = new CTFRelease(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getReleasetById() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the release details - " + status  + ", Error Msg - " + result);
            throw new IOException("Error getting the release details - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return  ctfRelease;
    }

    public CTFList<CTFRelease> getReleases() throws IOException {
        CTFList<CTFRelease> r = new CTFList<CTFRelease>();
        String end_point = app.getServerUrl() + CTFConstants.PACKAGE_URL + getId() + "/releases";
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
                            r.add(new CTFRelease(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getReleases() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the release list " + status + "Error Msg - " + result);
            throw new IOException("Error getting the release list  - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }
}
