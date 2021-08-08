# Changelog:

**Version 2.0.8:** Released 08/08/2021.

- Fix for CollabNet Security Realm bug in Jenkins-v2.266

**Version 2.0.7:** Released 07/16/2020.

-   Use HTTPS URL for plugin repository

**Version 2.0.6:** Released 11/06/2018.

-   Upgrade plugin to support both WEBR and EventQ

**Version 2.0.5:** Released 05/25/2018.

-   [Fix security issue](https://jenkins.io/security/advisory/2018-06-25/#SECURITY-941)

**Version 2.0.4**:  Released 09/08/2017.

-   Introduce 'publishEventQ' pipeline step. This step will report the
    current pipeline job status to TeamForge EventQ server ([Pull
    request
    \#19](https://github.com/jenkinsci/collabnet-plugin/pull/19))

-   Adds dependency on the 'credentials' plugin.

**Version 2.0.3**:  Released 05/27/2017.

-   Add enhancements for ActionHub related functions ([Pull request
    \#15](https://github.com/jenkinsci/collabnet-plugin/pull/15))

**Version 2.0.2**:  Released 11/28/2016.

-   No changes. Attempting to update automatic documentation

**Version 2.0.1**:  Released 11/25/2016.

-   Upgrade to use 1.645 jenkins plugin/core
-   Java 8 is required
-   EventQ and ActionHub integrations added
-   File Release will now create a new release, if it doesn't already
    exist (JENKINS-29314)

**Version 2.0.0**:  Not released.

**Version 1.2.0**:  Released 5/11/2016.

-   Upgrade plugin to use TeamForge SOAP60 API  (artf256273)

**Version 1.1.10**:  Released 10/11/2015.

-   Fixed: Document upload fails if TeamForge \> v8.0 (CNSC-176447)
-   Authenticated user belongs to authenticated magic group

**Version 1.1.9**:  Released 10/10/2013.

-   Fixed issue for large file uploads not matching original size
    (CNSC-157361)

**Version 1.1.8**:  Released 5/16/2013.

-   Fix for connection time out during large file uploads. ([Issue
    \#17152](https://issues.jenkins-ci.org/browse/JENKINS-17152))

**Version 1.1.7**:  Released 2/27/2013.

-   Fix for multiple field validations in document upload, tracker, and
    file release post-build actions including fixes for bugs causing
    stacktraces in the UI. ([Issue
    \#13742](https://issues.jenkins-ci.org/browse/JENKINS-13742))

**Version 1.1.6**:  Released 3/12/2011.

-   Misc code modernization/cleanup and bug fixes.
-   Fix to work with [Copy Artifact
    Plugin](https://wiki.jenkins.io/display/JENKINS/Copy+Artifact+Plugin)
    when using a build parameter for project name.
    ([JENKINS-8969](https://issues.jenkins-ci.org/browse/JENKINS-8969))

**Version 1.1.5**:  Released 4/22/2010.

-   Feature addition: Added configurable authorization cache when using
    TeamForge authorization to reduce load generated on TeamForge server
-   Feature addition: Passwords are stored more securely

**Version 1.1.4**:  Released 3/26/2010.

-   Bug fix: SVN tagging plugin does not work with 1.1.3

**Version 1.1.3**:  Released 2/26/2010.

-   Bug fix: A ClassCastException that caused builds to fail when the
    Tracker or Document Uploader component was enabled, is now fixed.
-   Feature addition: CollabNet Build & Test reauthenticates when
    TeamForge Single Sign On tokens from another user are observed.
-   Feature addition: A Hudson job that is not associated with a
    TeamForge project can now be viewed and configured by all
    authenticated users.

**Version 1.1.2**:  Released 12/21/2009.

-   Bug fix: FRS now can upload when using master/slave configuration
-   Bug fix: Error message related to Promoted Build plugin during
    startup
-   Bug fix: Having a trailing "/" in a CollabNet TeamForge URL ended
    the user's session and prevented single sign-on with Hudson
-   Feature addition: You can now use TeamForge's ScmViewer as the
    subversion repository viewer
-   Feature addition: You can enable/disable SSO between Hudson and
    CollabNet TeamForge

**Version 1.1.1**:  Released 09/03/2009.

-   Bug fix: Fixed a 500 error when the plugin is installed using some
    browsers (IE 8 and Chrome)
-   Bug fix: Fixed problem where triggered builds in Hudson (e.g.,
    periodic build or poll scm) won't run if CollabNet Authorization is
    in use.
-   Bug fix: Fixed problems that caused incorrect redirection on logging
-   Bug fix: Fixed problem where, for some users, the 'Assigned to'
    field in the Tracker component could not be set

**Version 1.1**:  Released 07/20/2009:

-   Fix to make file uploads faster (as of r17463).
-   Feature addition: Allow system-wide shared authentication which can
    be overridden for individual components (as of r18043).
-   Bug fix: Fixed errors that occurred when running Hudson in an iFrame
    in IE (r18646).
-   Bug fix: Fixed bug where SSO was not working on very first page load
    (r18684).

**Version 1.0**:  Released 04/14/2009.

