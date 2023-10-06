package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A folder in the documents section.
 * @author Kohsuke Kawaguchi
 */
public class CTFDocumentFolder extends CTFFolder {

    private static final String DOCUMENT_FOLDER_URL = "/ctfrest/docman/v1/projects/";
    private static final String DOC_FOLDER_URL = "/ctfrest/docman/v1/documentfolders/";

    static Logger logger = Logger.getLogger(CTFDocumentFolder.class.getName());

    CTFDocumentFolder(CTFObject parent, JSONObject object) {
        super(parent, object, object.getString("id"), object.getString("parentId"));
    }

    public CTFList<CTFDocumentFolder> getFolders() throws IOException {
        CTFList<CTFDocumentFolder> r = new CTFList<CTFDocumentFolder>();
        String end_point =  app.getServerUrl() + DOC_FOLDER_URL + getId() + "/documentfolders";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            URI uri = new URIBuilder(get.getURI()).addParameter(
                    "recursive", "false").addParameter("basic", "false").build();
            get.setURI(uri);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFDocumentFolder(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Get the folder list - " + e.getLocalizedMessage(), e);
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

    public CTFDocumentFolder createFolder(String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + DOC_FOLDER_URL + getId() + "/documentfolders";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + app.getSessionId());
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("title", title));
            params.add(new BasicNameValuePair("description", description));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFDocumentFolder(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a document folder" + e.getLocalizedMessage(), e);
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

    public CTFList<CTFDocument> getDocuments() throws IOException {
        CTFList<CTFDocument> r = new CTFList<CTFDocument>();
        String end_point =  app.getServerUrl() + DOC_FOLDER_URL + getId() + "/documents";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            URI uri = new URIBuilder(get.getURI()).addParameter(
                    "offset", "0").addParameter("count", "25").build();
            get.setURI(uri);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFDocument(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Get the folder list - " + e.getLocalizedMessage(), e);
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

    public CTFDocument createDocument(java.lang.String title,
                              java.lang.String description,
                              java.lang.String versionComment,
                              java.lang.String status,
                              boolean createLocked,
                              java.lang.String fileName,
                              java.lang.String mimeType,
                              CTFFile file,
                              java.lang.String associationId,
                              java.lang.String associationDesc) throws IOException {
        String end_point =  app.getServerUrl() + DOC_FOLDER_URL + getId() + "/documents";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONObject docObj = new JSONObject()
                    .element("title", title)
                    .element("description", description)
                    .element("status", status)
                    .element("fileName", fileName)
                    .element("mimeType", mimeType)
                    .element("fileId", file.getId());
            StringEntity stringEntity = new StringEntity(docObj.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 201) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFDocument(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a document folder" + e.getLocalizedMessage(), e);
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

    public String getURL() {
        return app.getServerUrl() + "/ctf/documents/home/" + getPath();
    }
}
