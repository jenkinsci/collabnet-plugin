## Set up a Jenkins build to which uploads a file to a Collabnet document application

Configure the Document Uploader component as a post-build action to upload Jenkins build files as document artifacts in a CollabNet SourceForge Enterprise or Teamforge project.

1.  In the Jenkins interface for the job, click **Configure**.
2.  In the **Post-build Actions** section, select **CollabNet Document Uploader**.
3.  Enter the URL of the CollabNet SourceForge or Teamforge site. For example, [https://forge.collab.net](https://forge.collab.net/).
4.  Enter the username and password of the CollabNet project member who will upload the files.
5.  Specify the name of the project and the path on the CollabNet server where you want the files uploaded.
6.  Enter description.Your description can include environment variables as well.
7.  Specify a file pattern so that files that match the pattern are uploaded. For example, "trunk/doc/\*\*"
8.  Click **Save**.

The Document Uploader can be used to upload any artifacts from the Jenkins workspace or the Build Log for the job.  The user specified must have permissions to upload documents in the project.

![](https://wiki.jenkins-ci.org/download/attachments/37323671/docuploader.png?version=2&modificationDate=1239098211000)

The resulting documents (if the build was successful) will be in the TeamForge Document

![](https://wiki.jenkins-ci.org/download/attachments/37323671/docupload-result.png?version=3&modificationDate=1239098229000)
