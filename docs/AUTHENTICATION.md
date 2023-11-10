# General instructions on how to setup Authentication/Authorization

## Authenticate users in Jenkins

Enable security on your Jenkins site to authenticate users against Digital.ai Teamforge server.

1.  In your Jenkins installation, go to Manage Jenkins\> Configure System \> Jenkins.
2.  Select Enable Security.\# In the Access Control section, select Digital.ai Security Realm to authenticate usernames and passwords.
3.  Enter the URL for your Teamforge site. For example, [https://forge.collab.net](https://forge.collab.net/).
4.  Click Save.

## Authorize users at the site level

At the site level, you can assign administration or read-only permissions to Teamforge users and groups.

1.  Make sure that the Authentication and Authorization plugin is installed and you've enabled Digital.ai security.
2.  In your Jenkins installation, go to Manage Jenkins\> Configure System \> Jenkins.
    a.  Select Digital.ai Authorization to specify what Teamforge users or groups can do on the site.
    b.  Enter a comma-separated list of users or groups to whom you want to grant administer privileges.
        **Note**: A Teamforge site administrator is automatically assigned administer privileges in Jenkins.
    c.  Specify the Teamforge users or groups to whom you want to grant read-only privileges.
        **Note**: To grant users permissions within individual projects, see these instructions.
3.  Click Save.

## Authorize users at the project level

Assign Teamforge users default permissions for a Jenkins project, or set up user roles for that project in Teamforge.  
**Note**: While configuring project-level access, you associate one Teamforge project to one Jenkins project

1.  In the Jenkins project, click **Configure**.
2.  In the **Authorization from Digital.ai** section, enter the name of the Teamforge project. This project is used to determine what its members can do in the Jenkins project.
3.  To automatically set up Jenkins -related roles in the Teamforge project, select Create Jenkins roles on Teamforge server in this project.
4.  Here's an example of the roles.

![](https://wiki.jenkins.io/download/attachments/59512142/hudson_rolesinCTF.png?version=1&modificationDate=1330958312000&api=v2)

1. Once the roles are available in Teamforge, the project administrator must assign them to project members to specify the things they can do in the Jenkins project.
2.  To give project administrators full permissions in the project, and all members read permission, select Grant default permissions to members of the project.
    **Note**: To be able to do this, you must be the Teamforge project administrator
3.  Click Save.

## Assign project-level Jenkins roles in Teamforge

As a Teamforge project administrator, assign roles to individual users and groups for the associated Jenkins job.  
Jenkins -related roles are automatically created in a Teamforge project when the Create Hudson roles on Teamforge server in this project option is selected in Jenkins.

1.  In the Teamforge project, click Project Admin in the project navigation bar.
2.  On the Project Admin menu, click Permissions.
    a.  To assign roles to a project member, click the User-Role Matrix tab.
    b.  To assign roles to a group, click the Group-Role Matrix tab.  The role matrix lists all project members or groups on the left and all available roles on the top.
3.  Select the appropriate roles.\# Click Save.The roles are now assigned to the project member or group.
4.  Click Save.

Setup for the Authentication and Authorization components starts on the main Jenkins configuration page.
