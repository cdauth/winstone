
Winstone Servlet Container v0.3
------------------

This is an alpha release of the Winstone Servlet Container.  The homepage for
this project is at 'http://winstone.sourceforge.net'

Author - Rick Knowles (contact details below)

What is Winstone ?
------------------

Winstone is a servlet container that was written out of a desire to provide servlet
functionality without the bloat that full J2EE compliance introduces.

It is not intended to be a completely fully functional J2EE style servlet 
container (by this I mean supporting extraneous APIs unrelated to Servlets, such as 
JNDI, JavaMail, EJBs, etc) - this is left to Tomcat, Jetty, Resin, JRun, Weblogic et al. 

Sometimes you want just a simple servlet container - without all the other junk - that just goes. 
This is where Winstone is best suited.

The original goals in writing Winstone were:
 - Supply fast, reliable servlet container functionality for a single webapp per server
 - Limit the size of the core distribution jar to less than 100KB for as long as possible
   (currently 111KB ... doh !!!)
 - Keep configuration files to an absolute minimum, using command line options to 
   optionally override sensible compiled in defaults. 
 - Eventually compile with GCJ to make a 3-4Meg windows exe for local development/deployment 
   of servlets. This has not happened yet, because of some GCJ class loading problems.
 - Optionally support JSP compilation using Apache's Jasper. (http://jakarta.apache.org)

Using Winstone
--------------

If you want to build from source code, you will need to download and install Apache Ant. The
following instructions assume you have already installed Ant and have the ant shell script in
your path (to get Ant, see http://ant.apache.org/).

To build Winstone, unpack the tree:

  tar zxf winstone_src_v0.3.tar.gz

Then build it:

  cd winstone/development
  ant dist

To run it:

  java -jar ../dist/winstone_v0.3.jar -webroot <location of webroot> (+ other options)

     - OR -

  java -jar ../dist/winstone_v0.3.jar -warfile <location of warfile> (+ other options)

The winstone.jar file will be in the winstone/dist directory after the build is complete.

Command-line options:
---------------------

Required options: either --webroot OR --warfile
   --webroot=location       = set document root folder.
   --warfile=location       = set location of warfile to extract from.

   --config=file            = load configuration properties from specified file name (if supplied). Default is ./winstone.properties
   --prefix=prefix          = add this prefix to all URLs (eg http://localhost:8080/prefix/resource). Default is none
   --debug=level            = set the level of debug msgs (1-9). Default is 5 (INFO level)
   --port=port              = set the listening port. Default is 8080
   --controlPort=port       = set the listening port. -1 to disable, Default disabled

   --directoryListings      = enable directory lists (true/false). Default is true
   --doHostnameLookups      = enable host name lookups on incoming connections (true/false). Default is true
   --useJasper              = enable jasper JSP handling (true/false). Default is false
   --useWinstoneClassLoader = enable WebApp classLoader (true/false). Default is true
   --useInvoker             = enable the servlet invoker (true/false). Default is false
   --invokerPrefix=prefix   = set the invoker prefix. Default is /servlet/

   --usage / --help         = show usage message

Why is it called Winstone ?
---------------------------

The short version (because the long version is way too politically incorrect) is as follows: 

Winstone is the name of a rather large Jamaican man a friend of mine met one night, while 
he was out clubbing in the Roppongi area of Tokyo one night. He (my friend) was a little 
liquored up at the time, and when Winstone suggested they head to "this really cool club" 
he knew, he didn't think anything was wrong. It wasn't until Winstone led him down a dark 
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

As a result of the design goals, there are some things Winstone doesn't do (at least 
not yet).
 - There is no HTTP authentication/security model at all (yet). This is big goal no 1 for 
   next release.
 - The only protocol connector available with Winstone currently is the direct HTTP connector.
   It doesn't support SSL/HTTPS or other protocols, such as AJP13 (yet). Big goal no 2 for v0.4 
   is  to implement an AJP13 connector, which will allow Winstone to operate behind 
   Apache/IIS/iPlanet/WebSphere the same way Tomcat, Resin, etc currently do.
 - There is no clustering support (yet). This will be implemented eventually by allowing Winstone
   instances to transfer sessions on request over the controlPort. This is a longer term goal 
   though.
 - HttpSession support is cookie-based only (no URL rewriting). I don't see any reason to 
   build this, since I've never come across anyone that actually uses URL rewriting. 
 - The messages are all in English only. These have been updated to use resource 
   bundles, but no translations yet.

Recent additions
----------------

New features in v0.3:

 - The request handler threads now use monitors, which allows them to exist beyond a single
   request/response cycle. The benefit here is performance - it removes a serious bottleneck.
 - WAR files are now supported via the -warfile attribute. If you specify a warfile in this 
   manner, it will be automatically extracted by the container to a temp directory, overwriting
   only files that are older than the archive date.
 - Servlet attribute, session, and context listeners are now fully supported. The 
   HttpSessionActivationListener class will be fully supported once the session transfer
   is implemented.
 - Servlet Filters (Servlet spec v2.3) are now fully supported.

Security Warning
----------------

If you enable the controlPort, be aware that any connection on that port whatsoever will 
trigger a server shutdown. As shutdown is the only purpose of the control port at the moment, 
it seemed fair to leave like this. As the admin features grow, I'll alter this.

The controlPort is disabled by default. If you choose not to enable it, the only way to shut
Winstone down is to kill the process (either by Ctrl-C or kill command). The reason I left it 
disabled by default was because I don't like the idea of an nmap scan inadvertedly shutting 
Winstone down.

License
-------

The Web-App DTD files (web-app_2_2.dtd & web-app_2_3.dtd in the src/javax/servlet/resources 
folder) and the lib/jsp-servlet.jar files are covered by the Apache software license, as 
described at the top of each file, and at http://www.apache.org/licenses/LICENSE

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
quite well - and to my naked eye, looked like lightning for static pages - except for 
one problem: class loading.

The problem is this: When you call "Class.forName(className)", the effect should be the 
same as calling "Class.forName(className, true, Thread.currentThread().getContextClassLoader())", and 
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
