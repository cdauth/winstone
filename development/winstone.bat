@echo off

set JAVA_HOME=c:\java\jdk1.4.2
set WINSTONE_HOME=c:\rick\winstone
set CATALINA_HOME=c:\tomcat4.1
set JAVA_OPTS=-Djava.endorsed.dirs=%JAVA_HOME%\jre\lib\ext

set CP=%WINSTONE_HOME%\dist\winstone.jar
@rem set WINSTONE_OPTS=--prefix=/examples --debug=7 --httpPort=9080 --controlPort=9081 --webroot=c:\java\tomcat\webapps\examples --argumentsRealm.passwd.rickk=rickk --argumentsRealm.roles.rickk=test,tomcat --useJNDI --jndi.resource.mail/Session=javax.mail.Session --jndi.param.mail/Session.mail.smtp.host=smtp.ponbiki.org --jndi.param.mail/Session.mail.smtp.user=rickk@ponbiki.org
@rem set WINSTONE_OPTS=--prefix=/examples --controlPort=8081 --webroot=c:\java\tomcat\webapps\examples
@rem set WINSTONE_OPTS=--prefix=/examples --debug=7 --webroot=c:\java\tomcat\webapps\examples
@rem set WINSTONE_OPTS=--prefix=/training --debug=8 --httpPort=9080 --controlPort=8081 --webroot=d:\rick\training\build
@rem set WINSTONE_OPTS=--prefix=/tristero --debug=7 --webroot=d:\download\neurogrid
set WINSTONE_OPTS=--prefix=/m3career --webroot=c:\tomcat4.1\webapps\m3career --httpPort=9080 --controlPort=8081 --debug=7

@rem ********************************************************************
@rem            Uncomment for non-1.4 jdks
@rem ********************************************************************
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\gnujaxp.jar

@rem ********************************************************************
@rem            Uncomment for jsp support
@rem ********************************************************************
set CP=%CP%;%CATALINA_HOME%\common\lib\jasper-runtime.jar
set CP=%CP%;%CATALINA_HOME%\common\lib\jasper-compiler.jar
set CP=%CP%;%CATALINA_HOME%\common\lib\ant.jar
set CP=%CP%;%WINSTONE_HOME%\development\lib\jsp-servlet.jar
set CP=%CP%;%JAVA_HOME%\lib\tools.jar
set WINSTONE_OPTS=%WINSTONE_OPTS% --useJasper

@rem ********************************************************************
@rem            Uncomment for invoker support (ie Tomcat style)
@rem ********************************************************************
set WINSTONE_OPTS=%WINSTONE_OPTS% --useInvoker

set CP=%CP%;c:\java\mail\activation.jar
set CP=%CP%;c:\java\mail\mail.jar

echo Options: %WINSTONE_OPTS%

%JAVA_HOME%\bin\java -server -cp %CP% %JAVA_OPTS% com.rickknowles.winstone.Launcher %WINSTONE_OPTS%
pause