Older versions of this plugin may not be safe to use. Please review the following warnings before using an older version:

-   [Plugin globally disables SSL/TLS certification validation in Jenkins](https://jenkins.io/security/advisory/2018-06-25/#SECURITY-941)

The Digital.ai Plugin is an integration with [Digital.ai TeamForge](https://digital.ai/products/teamforge/ "Digital.ai TeamForge"). Don't

Provided features include:

-   Authenticate users from TeamForge.  If setup as the "Build & Test" application, it can even use Single Sign-On.
-   Authorization from TeamForge, including the ability to set permissions in Jenkins based on roles in your TeamForge project.
-   Upload the build log or workspace artifacts to the TeamForge Documents.
-   Upload workspace artifacts to the TeamForge File Release System, as a post-build publishing task or as a build promotion task.
-   Open/update/close TeamForge Tracker artifacts based on the Jenkins build status.

# Requirements:

The Digital.ai Plugin requires the [Subversion Plugin](https://wiki.jenkins.io/display/JENKINS/Subversion+Plugin).

The Digital.ai Plugin requires a [Digital.ai TeamForge 23.0](https://docs.digital.ai/bundle/teamforge230/page/index.html "Digital.ai TeamForge 23.0") or
[Digital.ai TeamForge 23.0](https://docs.digital.ai/bundle/teamforge230/page/index.html "Digital.ai TeamForge 23.0")+.

# Configuring Components:

Following individual component can be configured within each Jenkins job

## [Authentication](https://github.com/jenkinsci/collabnet-plugin/blob/master/docs/AUTHENTICATION.md)

## [Document Uploader](https://github.com/jenkinsci/collabnet-plugin/blob/master/docs/DOCUMENTS.md)

## [File Release to TeamForge](https://github.com/jenkinsci/collabnet-plugin/blob/master/docs/FRS.md)

## [Tracker](https://github.com/jenkinsci/collabnet-plugin/blob/master/docs/TRACKER.md)

## [SCMViewer for Subversion](https://github.com/jenkinsci/collabnet-plugin/blob/master/docs/SCM.md)

# FAQ:

   1.  Does this integrate with / update the [SFEE Plugin](https://wiki.jenkins.io/display/JENKINS/SFEE+Plugin)?

        No, the [SFEE Plugin](https://wiki.jenkins.io/display/JENKINS/SFEE+Plugin) is a separate plugin that is not maintained by [Digital.ai](https://digital.ai/ "Digital.ai"). .

   2.  Uploads fail when using an SSL-enabled TeamForge server.  What do I do?

        You'll need to add the server's certificate to your java keystore on the Jenkins server.  First dowload the certificate to your Jenkins server.            You should be able to get it by opening a browser window on the TeamForge server, viewing the certificate associated with that page, and          exporting the certificate file.  Then, you'll need to run something like "sudo keytool -keystore $JAVA\_HOME/jre/lib/security/cacerts -                   import -file teamforge.cert".  (On Windows, replace $JAVA\_HOME with %JAVA\_HOME%).  If you haven't changed your java keystore's                    password, it will be "changeit".  After you've imported the cert to the java instance that Jenkins is using, it should be able to upload.

   3.  I'm having problems with login and logout when using Digital.ai Authentication.   It seems to be redirecting to an unex pected site.

        Go to the Jenkins configure page and look in the Email Notification section. The Digital.ai plugin uses the value of the Jenkins URL set                 here for redirection. If it's wrong, logins will go astray. Check that it's set to the real Jenkins URL. 

   4.  I've setup authentication, but can no longer get to the "Manage Jenkins" pages.&nbps; How do I get back into the system?

        As long as you have access to the machine and user running Jenkins, you'll be able to get back in. Just log into the machine. Find the                 Jenkins home (by default, this will be the \~/.jenkins or \~/.hudson directory of the user running Jenkins). Edit the config.xml, changing the           value for "useSecurity" to false. Restart Jenkins and you should not need to log in to get access.

# Running Tests

There are some tests that require a live CTF instance. If you do not have a CTF instance, you can skip those tests by running Maven with the "-Doffline" option.

Otherwise, you need the "~/.teamforge" property file that contains information about how to access the CTF instance.

This file has to contain the following 4 information:

```
teamforge_url=https://test.example.org/
admin_user=admin
password=****
teamforge_project=hudson-test
```

The user has to have a full access on the specified CTF instance. The test involves messing around with the specified project, so you'd better specify the project whose contents is not important.

## More information

* [Changelog](https://github.com/jenkinsci/collabnet-plugin/blob/master/CHANGELOG.md)
