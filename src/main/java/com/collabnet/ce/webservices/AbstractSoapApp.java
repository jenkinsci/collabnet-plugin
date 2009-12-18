package com.collabnet.ce.webservices;

/**
 * Base class for CollabNet's Soap App wrapper classes.
 */
public abstract class AbstractSoapApp {
    private CollabNetApp mCollabNetApp;

    /**
     * Constructor.
     * @param collabNetApp collabNetApp
     */
    public AbstractSoapApp(CollabNetApp collabNetApp) {
        mCollabNetApp = collabNetApp;
    }

    /**
     * Check if the current session is valid.
     */
    protected void checkValidSessionId() {
        mCollabNetApp.checkValidSessionId();
    }

    /**
     * Get the session id
     * @return session id
     */
    protected String getSessionId() {
        return mCollabNetApp.getSessionId();
    }

    /**
     * Get the server url
     * @return the server url
     */
    protected String getServerUrl() {
        return mCollabNetApp.getServerUrl();
    }
}
