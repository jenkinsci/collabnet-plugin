package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFReleaseFile extends CTFItem {
    private final String description, mimeType, filename;
    private final long size;

    static Logger logger = Logger.getLogger(CTFReleaseFile.class.getName());

    Helper helper = new Helper();

    protected CTFReleaseFile(CTFObject parent, JSONObject data) {
        super(parent, data);
        this.description = data.get("description")!= null ? data.
                get("description").toString() : null;
        this.mimeType = data.get("mimeType").toString();
        this.filename = data.get("filename").toString();
        this.size = Integer.parseInt(data.get("size").toString());
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/frs/do/downloadFile/" + getPath();
    }

    public void delete() throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.RELEASE_FILE_URL + getId();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.DELETE, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status >= 300) {
            logger.log(Level.WARNING, "Error while deleting a release file - " + status +  ", Error Msg - " + result);
            throw new IOException("Error while deleting a release file - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }
}
