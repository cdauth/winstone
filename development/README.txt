
Winstone Servlet Container v0.2
------------------

This is an alpha release of the Winstone Servlet Container.  The homepage for
this project is at 'http://winstone.sourceforge.net'

Author - Rick Knowles (contact details below)

What is Winstone ?
------------------

Winstone is a servlet container that was written out of a desire to provide servlet
functionality without the bloat that full J2EE compliance introduces.

It is not intended to be a completely fully functional J2EE style servlet 
container - this is left to Tomcat, Jetty, Resin, JRun, Weblogic et al. 

The original goals in writing Winstone were:
 - Supply fast, reliable servlet container functionality for a single webapp per server
 - Limit the size of the core distribution jar to less than 100KB for as long as possible
   (currently 101KB ... doh !!! time to trim some comments from the DTDs)
 - Keep configuration files to an absolute minimum, using command line options to 
   optionally override sensible compiled in defaults. 
 - Eventually compile with GCJ to make a 3-4Meg windows exe for local development/deployment 
   of servlets. This has not happened yet, because of some linking problems in the build.
 - Optionally support JSP compilation using Apache's Jasper. (http://jakarta.apache.org)

Using Winstone
--------------

To build Winstone, unpack the tree:

  tar zxf winstone_src_v0.2.tar.gz

Then build it:

  cd winstone/development

  build dist (Win32) 
  -or- 
  build.sh dist (Unix)

To run it:

  java -jar winstone_v0.2.jar -webroot <location of webroot> (+ other options)

The Winstone.jar file will be in the winstone/dist directory after the build is complete.

Command-line options:
---------------------

   -webroot                = set document root folder. ** REQUIRED OPTION **
   -prefix                 = add this prefix to all URLs (eg http://localhost:8080/prefix/resource). Default is none

   -debug                  = set the level of debug msgs (1-9). Default is 5 (INFO level)
   -port                   = set the listening port. Default is 8080
   -controlPort            = set the listening port. -1 to disable, Default disabled

   -directoryListings      = enable directory lists (true/false). Default is true
   -doHostnameLookups      = enable host name lookups on incoming connections (true/false). Default is true
   -useJasper              = enable jasper JSP handling (true/false). Default is false
   -useWinstoneClassLoader = enable WebApp classLoader (true/false). Default is true
   -useInvoker             = enable the servlet invoker (true/false). Default is true
   -invokerPrefix          = set the invoker prefix. Default is /servlet/

   -usage / -help          = show usage message

Why is it called Winstone ?
---------------------------

The short version (because the long version is way too politically incorrect) is as follows: 

Winstone is the name of a rather large Jamaican man a friend of mine met one night, while 
he was out clubbing in the Roppongi area of Tokyo one night. He (my friend) was a little 
liquored up at the time, and when Winstone suggested they head to "this really cool club" 
he knew, he didn't think anything was wrong. It wasn't until Winstone lead him down a dark 
stairwell and dropped his trousers that my friend clued in and ran like hell.

It was too good a story to let die, so I named this project Winstone so that said friend 
will continue to be reminded of it. Heheheh ....

JSPs (aka using with Jasper)
----------------------------

Winstone does support JSPs, using Apache's Jasper JSP compiler. Thanks to a rather clever 
design by the Jasper team, this was much less work than I expected - well done Jasper guys.

In order to turn on JSP compilation, you need to include the following additional jars in 
the startup classpath:

  * jsp-servlet.jar

         This is a stripped down version of the tomcat servlet.jar. I removed 
         the javax/servlet and javax/servlet/http sections from the original 
         servlet.jar though, to avoid conflicts with Winstone's api classes. 
         This is in the Winstone development/lib folder.

  * jasper-compiler, jasper-runtime.jar, ant.jar

         Not supplied. You can get this from the standard Tomcat binary download location. 
         Just download the latest Tomcat, and copy these three files into the startup classpath 
         for Winstone. They will be in the tomcat_home/common/lib folder.

  * tools.jar

         You should already have this in the <java_home>/lib folder of your jdk. You'll need
         to add this, cause the ant javac compile task uses it, and this is needed to turn the
         generated .java files into .class files at runtime.

Tomcat is available at http://jakarta.apache.org/tomcat/index.html - but please note that the
license for Tomcat is not the same as Winstone. Please read carefully before packaging.

Caveats
-------

As a result of the design goals, there are a lot of things Winstone doesn't do (at least 
not yet).
 - Servlet Filters are not supported (yet)
 - Servlet Attribute and Context listeners are not supported very well (yet)
 - There is no security model at all (yet)
 - HttpSession support is cookie-based only (no URL rewriting). I don't see any reason to 
   build this, since I've never come across anyone that actually uses URL rewriting. 
 - The messages are all in English only. These have been updated to use resource 
   bundles, but no translations yet.

Security Warning
----------------

If you enable the controlPort, be aware that any connection on that port whatsoever will 
trigger a server shutdown. As shutdown is the only purpose of the control port at the moment, 
it seemed fair to leave like this. As the admin features grow, I'll alter this.

License
-------

The Web-App DTD files (web-app_2_2.dtd & web-app_2_3.dtd in the src/javax/servlet/resources 
folder) and the lib/jsp-servlet.jar files are covered by the Apache software license, as 
described at the top of each file, and at http://www.apache.org/licenses/LICENSE

The ant jar files (included in the ant/lib folder) are also covered by the Apache software 
license: see http://www.apache.org/licenses/LICENSE

All other files are covered by the GNU Public License, as described in LICENSE.txt.

Contacting the Author
---------------------

You can contact me through the Winstone development list at sourceforge 
(winstone-devel@lists.sourceforge.net).  If you have any general comments or questions 
about Winstone please mail me on that list - I'll try to start a faq soon.

I'm open to help from anyone who's willing to help me meet the above goals for Winstone.
Just mail me.

GCJ compilation
---------------

I've had a little bit of success with this recently. Under the gcj folder in the source
distribution there is a Makefile that I used to build Winstone to a native binary. It worked
quite well - and to my naked eye, looked as fast as Apache 1.3 for static pages - except for 
one problem: class loading.

The problem is this: When you call "Class.forName(className)", the effect should be the 
same as calling "Class.forName(className, true, this.getClass().getClassLoader())", and 
the short answer is that in recent GCJ versions, it's not.

Recent GCJ versions use a "bogus" method to find the current class loader (their words 
not mine). Basically GCJ generates a stack trace and walks back over it, trying to find a 
class loader within recent memory. If none is found, it uses the system class loader.

In most cases this is fine, but in a servlet container this is no good, because unless you
have a webapp that has no WEB-INF/lib and WEB-INF/classes folders, you need to use a class
loader other than the system classloader.

One possible solution is to compile the webapp classes and Winstone into native binary format.
This works except that if the servlet container tries to use another class loader anyway, you
still have problems with which loader loaded which classes.

To get around this, I have included an option "-useWinstonClassLoader true/false". This allows
you to disable the webapp class loader, and operate entirely with a single class loader. This
works ok for servlet-only environments, but it seems Jasper uses it's own classloader, so 
you may still have problems in jsp + GCJ-compiled-Winstone environments.

NOTE: The Makefile included uses Crimson as an XML parser, which has since been replaced 
with the GNU XML parser due to License conflicts.
