package hudson.plugins.collabnet.filerelease;

import hudson.model.Action;

/**
 * Displays the results of a File Release System upload attempt.
 */
public class CnfrResultAction implements Action {
    private String display;
    private String icon;
    private String resultUrl;
    private String cnfrUrl;
    private int numFiles = 0;
    
    public CnfrResultAction(String display, String icon, String resultUrl,
				 String cnfrUrl, int numFiles) {
	this.display = display;
	if (icon != null) {
	    this.icon = icon;
	} else {
	    this.icon = "clipboard.gif";
	}
	this.resultUrl = resultUrl;
	this.cnfrUrl = cnfrUrl;
	this.numFiles = numFiles;
    }
    
    public String getDisplayName() {
	return this.display;
    }
    
    public String getIconFileName() {
	// sidebar is invisible on failure
	if (this.isSuccess()) {
	    return this.icon;
	} else {
	    return null;
	}
    }

    public String getSummaryIcon() {
	// returns the icon, whether or not we succeed
	return this.icon;
    }
    
    public String getResultUrlName() {
	return this.resultUrl;
    }

    public String getCnfrUrlName() {
	return this.cnfrUrl;
    }

    public String getUrlName() {
	return this.getCnfrUrlName();
    }

    public int getNumFiles() {
        return this.numFiles;
    }

    public boolean isSuccess() {
	return (this.getNumFiles() > 0);
    }
}
