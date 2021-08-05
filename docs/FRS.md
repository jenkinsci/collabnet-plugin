## Setup Jenkins build which uploads file to File Release System

To upload build results to a file release in CollabNet SourceForge Enterprise or CollabNet Teamforge, configure the file release component as a post-build action in Jenkins.

Before you set up the CollabNet plugin, make sure that the CollabNet SorceForge or Teamforge project contains valid package and file release folders. 

1.  In the Hudson interface for the job, click **Configure**.
2.  In the **Post-build Actions** section, select **CollabNet File Release**.
3.  Enter the URL of the CollabNet site. For example, [https://forge.collab.net](https://forge.collab.net/).
4.  Enter the username and password of the CollabNet project member who will upload the files.
5.  Enter the name of the CollabNet project, the package and the file release where you want the build files added.
6.  Specify whether you want existing files with the same name to be overwritten.
7.  Specify a file pattern so that files that match the pattern are uploaded. You may use Hudson environmental variables in your file patterns. For example, pkg/subversion-r $ {SVN\_REVISION).tar.gz, in a job to build Subversion source code.
8.  Click **Save**.

The File Release plugin can be used to upload any workspace artifact to the File Release system.  It can be configured as a post-build action or a promoted-builds action. The file release user must have permission to upload add file releases in the project.  Below is how a promoted-builds setup might look.  
![](https://wiki.jenkins-ci.org/download/attachments/37323671/filerelease.png?version=2&modificationDate=1239098234000)

The resulting files will be in the File Release System, like so:

![](https://wiki.jenkins-ci.org/download/attachments/37323671/fr-result.png?version=2&modificationDate=1239098241000)
