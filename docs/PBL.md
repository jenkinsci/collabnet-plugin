## Setup the Project Build Library component

**_General Instructions_**:

Configure this component as a post-build action to automatically upload files such as daily build results which do not need permanent storage, to a Lab Management Project Build Library.

Before you set up this component, make sure that there is a project on the Lab Management manager node where the specified user has at least the "Lab Management - Root User" role.

The Project Build Library is the place within CollabNet Lab Management where build files are created and stored. Unlike a project's Subversion repository, files in the Project Build Library are not under revision control, and may be permanently deleted. This makes the PBL an ideal place to upload large binaries which may not be needed forever, like daily builds. Project members can browse to this location and view and download the build results they need.

1.  In the Jenkins interface for the job, click **Configure**.
2.  In the **Post-build Actions** section, select **Lab Management Project Build Library (PBL) Uploader**.
3.  Enter the URL of the Lab Management manager node. For example, [https://mgr.cubit.collab.net](https://mgr.cubit.collab.net/).
4.  Enter the username of the user who will upload the files.
5.  Enter the user's API key for the Lab Management web service.
6.  Specify the name of the project where you want the build files uploaded.
7.  Select whether you want to upload the files to the Public or Private location on the Project Build Library. Public files are accessible to all users who have "Project Document - View" permission in any project. Private files are accessible only to users who have "Project Document - View" permission in the project where the file resides.
8.  Specify a file pattern so that files that match the pattern are uploaded.
9.  Enter the path where the files are to be uploaded
     **NOTE**: For example, if your path is  /my/path/$ {BUILD\_ID), and you chose the private location in the Project Build Library, the files will be uploaded at <https://mgr.cubit.collab.net/pbl/project_name/priv/my/path/$>{BUILD\_ID)
10.  Select whether you want to overwrite existing files. Select **True** if you have files you want to replace after each build.
11. Enter a description. For example, Here is ${BUILD\_ID)uploaded from ${HUDSON\_URL)
12. Enter an optional comment for the files. The comment is stored in the Project Build Library Audit Log.
13. Click **Save**.

The build results from your Jenkins server are available under Most Recent Uploads in the Project Build Library.

The Project Build Library component requires  [CollabNet Lab Management](http://www.open.collab.net/products/CUBiT/).  Like File Release, it may be used as a post-build action or as a promoted-builds action.  Below is an example of how to configure it as a post-build action.

![](https://wiki.jenkins-ci.org/download/attachments/37323671/pbl.png?version=2&modificationDate=1239098247000)

The resulting files will appear in the Lab Management Project Build Library, like so:

![](https://wiki.jenkins-ci.org/download/attachments/37323671/pbl-result.png?version=2&modificationDate=1239098253000)
