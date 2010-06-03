package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFGroup;
import com.collabnet.ce.webservices.CTFUser;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.TestParam;

import java.rmi.RemoteException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractSecurityTestCase extends CNHudsonTestCase {
    @TestParam
    protected String admin_group = "hudsonAdmins";
    @TestParam
    protected String admin_group_member = "hudsonAdminMember";
    @TestParam
    protected String read_user = "hudsonRead";
    @TestParam
    protected String read_group = "hudsonReaders";
    @TestParam
    protected String read_group_member = "hudsonReadMember";

    protected void installSecurityRealm() {
        hudson.setSecurityRealm(new CollabNetSecurityRealm(teamforge_url, false, false));
    }

    /**
     * Installs global authorization strategy.
     */
    protected void installAuthorizationStrategy() throws Exception {
        installSecurityRealm();
        hudson.setAuthorizationStrategy(new CNAuthorizationStrategy(
                read_user, read_group, admin_user, admin_group, 5
        ));

        // make sure that groups and group members are exist and set up
        CollabNetApp cna = connect();
        CTFGroup ag = cna.getGroupByTitle(admin_group);
        if (ag==null)
            ag = cna.createGroup(admin_group,admin_group);
        CTFUser agm = cna.getUser(admin_group_member);
        if (agm==null) {
            agm = createUser(cna,admin_group_member);
        }
        ag.addMember(agm);

        CTFGroup rg = cna.getGroupByTitle(read_group);
        if (rg==null)
            rg = cna.createGroup(read_group,read_group);
        CTFUser rgm = cna.getUser(read_group_member);
        if (rgm==null) {
            rgm = createUser(cna,read_group_member);
        }
        rg.addMember(rgm);
    }

    public CTFUser createUser(CollabNetApp cna, String name) throws RemoteException {
        return cna.createUser(name, name +"@example.org", name,"en","PST",false,false, name);
    }

    protected WebClient createAdminWebClient() throws Exception {
        return createWebClient().login(admin_user,password);
    }
}
