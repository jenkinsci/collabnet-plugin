package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFPackage extends CTFFolder {

    private static final String PACKAGE_URL = "/ctfrest/frs/v1/packages/";

    static Logger logger = Logger.getLogger(CTFPackage.class.getName());

    CTFPackage(CTFObject parent, JSONObject data) {
        super(parent, data, data.get("id").toString(), data.get("parentFolderId").toString());
    }

    /**
     * Deletes this package.
     */
    public void delete() throws IOException {
        String end_point =  app.getServerUrl() + PACKAGE_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpDelete delete = new HttpDelete(end_point);
            delete.setHeader("Accept", "application/json");
            delete.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(delete);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error while deleting a package - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public CTFRelease createRelease(String title, String description, String status, String maturity) throws IOException {
        String end_point =  app.getServerUrl() + PACKAGE_URL + getId() + "/releases" ;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +app.getSessionId());
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("title", title));
            params.add(new BasicNameValuePair("description", description));
            params.add(new BasicNameValuePair("status", status));
            params.add(new BasicNameValuePair("maturity", maturity));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFRelease(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a release" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
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
        for (CTFRelease p : getReleases())
            if (p.getTitle().equals(relTitle))
                return p;
        return null;
    }

    public CTFList<CTFRelease> getReleases() throws IOException {
        CTFList<CTFRelease> r = new CTFList<CTFRelease>();
        String end_point = app.getServerUrl() + PACKAGE_URL + getId() + "/releases";
        CloseableHttpClient httpClient;
        CloseableHttpResponse response;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-type", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFRelease(this,jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the release list" + e.getLocalizedMessage(), e);
        }
        return r;
    }
}
