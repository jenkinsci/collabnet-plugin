package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFRelease extends CTFFolder {

    static Logger logger = Logger.getLogger(CTFRelease.class.getName());

    Helper helper = new Helper();

    CTFRelease(CTFObject parent, JSONObject data) {
        super(parent, data, data.get("id").toString(), data.get("parentFolderId").toString());
    }

    /**
     * The HTTP URL of this release on the server.
     */
    public String getUrl() {
        return app.getServerUrl()+"/sf/frs/do/viewRelease/"+getPath();
    }

    public void delete() throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.RELEASE_URL + getId();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.DELETE, null);
        int status = response.getStatus();
        String result = response.readEntity(String.class);
        if (status != 200) {
            logger.log(Level.WARNING, "Error while deleting a package - " + status + ", Error Msg - " + result);
            throw new IOException("Error while deleting a package - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }

    public CTFReleaseFile getFileByTitle(String title) throws IOException {
        for (CTFReleaseFile f : getFiles())
            if (f != null) {
                if (f.getTitle().equals(title))
                    return f;
            }
        return null;
    }

    public List<CTFReleaseFile> getFiles() throws IOException {
        List<CTFReleaseFile> r = new ArrayList<CTFReleaseFile>();
        String end_point = app.getServerUrl() + CTFConstants.RELEASE_URL + getId() + "/files";
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
                        if (jsonObject != null) {
                            r.add(new CTFReleaseFile(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getFiles() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the file release lists - " + status + ", Error Msg - " + result);
            throw new IOException("Error getting the file release lists - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    public CTFReleaseFile addFile(String fileName, String mimeType, CTFFile file)
        throws IOException {
        CTFReleaseFile ctfReleaseFile = null;
        String end_point =  app.getServerUrl() + CTFConstants.RELEASE_URL + getId() + "/files";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("fileName", fileName);
        requestPayload.put("mimeType", mimeType);
        requestPayload.put("fileId", file!=null?file.getId():null);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 201) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfReleaseFile = new CTFReleaseFile(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in addFile()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error adding a file to the file release - " + statusCode +  ", Error Msg - " + result);
            throw new IOException("Error adding a file to the file release - " + statusCode + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return ctfReleaseFile;
    }
}
