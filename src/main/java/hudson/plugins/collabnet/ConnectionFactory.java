package hudson.plugins.collabnet;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.collabnet.auth.CollabNetSecurityRealm;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Represents the information about the connectivity to TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConnectionFactory extends
		AbstractDescribableImpl<ConnectionFactory> {
	private final String url;
	private final String username;
	private final Secret password;

	@DataBoundConstructor
	public ConnectionFactory(String url, String username, String password) {
		this(url, username, Secret.fromString(password));
	}

	public ConnectionFactory(String url, String username, Secret password) {
		this.url = CNHudsonUtil.sanitizeCollabNetUrl(url);
		this.username = username;
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public Secret getPassword() {
		return password;
	}

	// the equality test is really just for testing
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ConnectionFactory that = (ConnectionFactory) o;

		if (password != null ? !password.equals(that.password)
				: that.password != null)
			return false;
		if (url != null ? !url.equals(that.url) : that.url != null)
			return false;
		if (username != null ? !username.equals(that.username)
				: that.username != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = url != null ? url.hashCode() : 0;
		result = 31 * result + (username != null ? username.hashCode() : 0);
		result = 31 * result + (password != null ? password.hashCode() : 0);
		return result;
	}

	/**
	 * A databinding method from Stapler.
	 */
	public static ConnectionFactory fromStapler(@QueryParameter String url,
			@QueryParameter String username, @QueryParameter String password) {

		if (CommonUtil.unset(url) || CommonUtil.unset(username)
				|| CommonUtil.unset(password)) {
			return null;
		}
		return new ConnectionFactory(url, username, password);
	}

	@Extension
	public static final class DescriptorImpl extends
			Descriptor<ConnectionFactory> {
		public static String url;
		public static boolean isSiteConfigured = false;

		public String getDisplayName() {
			return "";
		}

		public static String getUrl() {
			if (isSiteConfigured) {
				return url;
			} else {
				return getTeamForgeShareDescriptor().getCollabNetUrl();
			}
		}

		public boolean useGlobal() {
			return getTeamForgeShareDescriptor().useGlobal();
		}

		/**
		 * Form validation for the CollabNet URL.
		 *
		 * @param value
		 *            url
		 */
		public FormValidation doCheckUrl(@QueryParameter String value) {
			return CNFormFieldValidator.soapUrlCheck(value);
		}

		public boolean isSiteConfigured() {
			try {
				SecurityRealm securityRealm = Hudson.getInstance()
						.getSecurityRealm();
				CollabNetSecurityRealm cnRealm = (CollabNetSecurityRealm) securityRealm;
				url = cnRealm.getCollabNetUrl();
				isSiteConfigured = true;
				return true;
			} catch (ClassCastException e) {
				return false;
			}
		}

		/**
		 * @return the TeamForge share descriptor.
		 */
		public static TeamForgeShare.TeamForgeShareDescriptor getTeamForgeShareDescriptor() {
			return TeamForgeShare.getTeamForgeShareDescriptor();
		}

		/**
		 * Form validation for username.
		 *
		 * @param value
		 *            to check
		 */
		public FormValidation doCheckUsername(@QueryParameter String value) {
			return CNFormFieldValidator.requiredCheck(value, "username");
		}

		/**
		 * Check that a password is present and allows login.
		 */
		public FormValidation doCheckPassword(
				ConnectionFactory connectionFactory,
				@QueryParameter String value) {
			CollabNetApp app = (connectionFactory == null) ? null
					: CNHudsonUtil.getCollabNetApp(connectionFactory.getUrl(),
							connectionFactory.getUsername(), connectionFactory
									.getPassword().getPlainText());
			return CNFormFieldValidator.loginCheck(app, value);
		}
	}
}
