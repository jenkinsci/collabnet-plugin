package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFRelease extends CTFFolder {

    private static final String FRS_URL = "/ctfrest/frs/v1/releases/";

    static Logger logger = Logger.getLogger(CTFRelease.class.getName());

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
        String end_point =  app.getServerUrl() + FRS_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpDelete delete = new HttpDelete(end_point);
            delete.setHeader("Accept", "application/json");
            delete.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(delete);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error while deleting a release - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public CTFReleaseFile getFileByTitle(String title) throws RemoteException {
        for (CTFReleaseFile f : getFiles())
            if (f.getTitle().equals(title))
                return f;
        return null;
    }

    public List<CTFReleaseFile> getFiles() throws RemoteException {
        List<CTFReleaseFile> r = new ArrayList<CTFReleaseFile>();
        String end_point = app.getServerUrl() + FRS_URL + getId() + "/files";
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
                r.add(new CTFReleaseFile(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the file release lists" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public CTFReleaseFile addFile(String fileName, String mimeType, CTFFile file)
        throws IOException {
        CTFReleaseFile ctfReleaseFile = null;
        String end_point =  app.getServerUrl() + FRS_URL + getId() + "/files";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPost post = new HttpPost(end_point);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONObject fileRelObj = new JSONObject()
                    .element("fileName", fileName)
                    .element("mimeType", mimeType)
                    .element("fileId", file.getId());
            StringEntity stringEntity = new StringEntity(fileRelObj.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(stringEntity);
            response = httpClient.execute(post);
            if(response.getStatusLine().getStatusCode() == 201) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFReleaseFile(this, data);
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error adding a file to the file release - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return ctfReleaseFile;
    }
}
