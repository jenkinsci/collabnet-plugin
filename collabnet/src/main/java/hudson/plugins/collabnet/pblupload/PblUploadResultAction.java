package hudson.plugins.collabnet.pblupload;

import hudson.model.Action;


public class PblUploadResultAction implements Action {
	private static final long serialVersionUID = 1L;
	private String display;
    private String icon;
    private String resultUrl;
    private String pblUrl;
    private boolean success = false;
    
    public PblUploadResultAction(String display, String icon, String resultUrl,
				 String pblUrl, boolean success) {
	this.display = display;
	if (icon != null) {
	    this.icon = icon;
	} else {
	    this.icon = "clipboard.gif";
	}
	this.resultUrl = resultUrl;
	this.pblUrl = pblUrl;
	this.success = success;
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

    public String getPblUrlName() {
	return this.pblUrl;
    }

    public String getUrlName() {
	return this.getPblUrlName();
    }

    public boolean isSuccess() {
	return this.success;
    }
}
