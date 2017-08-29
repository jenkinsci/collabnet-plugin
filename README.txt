Running Tests
=============
There are some tests that require a live CTF instance.
If you do not have a CTF instance, you can skip those tests by running
Maven with the "-Doffline" option.

Otherwise, you need the "~/.teamforge" property file that contains
information about how to access the CTF instance.

Added this to readme 



This file has to contain the following 4 information:

---------------
teamforge_url=https://test.example.org/
admin_user=admin
password=****
teamforge_project=hudson-test
---------------

The user has to have a full access on the specified CTF instance.
The test involves messing around with the specified project, so
you'd better specify the project whose contents is not important.
