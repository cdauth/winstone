
Winstone Servlet Container v0.1
------------------

This is an alpha release of the Winstone Servlet Container.  The homepage for
this project is at 'https://sourceforge.net/project/showfiles.php?group_id=84866'.

Author - Rick Knowles (rick@knowleses.org)

What is Winstone ?
------------------

Winstone is a servlet container that was written out of a desire to provide servlet
functionality without the bloat that full J2EE compliance introduces.

It is not intended to be a completely fully functional J2EE style servlet 
container - this is left to Tomcat, Resin, Weblogic et al. 

The original goals in writing Winstone were:
 - Supply fast, reliable servlet container functionality for a single webapp per server
 - Limit the size of the core distribution jar to less than 100KB for as long as possible.
 - Keep configuration files to an absolute minimum, using command line options to 
   optionally override sensible compiled in defaults. 
 - Eventually compile with GCJ to make a 3-4Meg windows exe for local development/deployment 
   of servlets. This has not happened yet, because of some linking problems in the build.
 - Optionally support JSP compilation using Apache's Jasper. (http://jakarta.apache.org)

Using Winstone
--------------

To build Winstone, unpack the tree:

  tar zxf winstone_src_v0.1.tar.gz

Then build it:

  cd winstone/development

  build dist (Win32) 
  -or- 
  build.sh dist (Unix)

To run it:

  java -jar winstone_v0.1.jar

The Winstone.jar file will be in the winstone/dist directory after the build is complete.

Caveats
-------

As a result of the design goals, there are a lot of things Winstone doesn't do (at least not yet).
 - Servlet Filters are not supported (yet)
 - Servlet Attribute and Context listeners are not supported very well (yet)
 - There is no invoker servlet as in Tomcat, so you have to map absolutely every servlet
 - There is no security model at all (yet)
 - HttpSession support is cookie-based only (no URL rewriting). I don't see any reason to 
   build this, since I've never come across anyone that actually uses URL rewriting. 
 - The messages are all in English only. These will be updated to use resource bundles soon.

Security Warning
----------------

If you enable the controlPort, be aware that any connection on that port whatsoever will trigger
a server shutdown. As shutdown is the only purpose of the control port at the moment, it seemed
fair to leave like this. As the admin features grow, I'll alter this.

License
-------

The Web-App DTD files (web-app_2_2.dtd & web-app_2_3.dtd in the src/javax/servlet/resources folder)
are covered by the Apache software license, as described at the top of each file.

All other files are covered by the GNU Public License, as described in LICENSE.txt.

Contacting the Author
---------------------

My email is 'rick@knowleses.org'.  If you have any general comments or
questions about the software please mail me - I'll try to start a faq soon.

I'm open to help from anyone who's willing to help me meet the above goals for Winstone.
Just mail me.
