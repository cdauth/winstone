@echo off

set JAVA_HOME=c:\java\jdk1.4.2
set WINSTONE_HOME=d:\rick\winstone
set CATALINA_HOME=c:\java\tomcat
set JAVA_OPTS=-Djava.endorsed.dirs=%JAVA_HOME%\jre\lib\ext

set CP=%WINSTONE_HOME%\dist\jsp-servlet.jar
set CP=%CP%;%WINSTONE_HOME%\dist\winstone.jar
@rem set WINSTONE_OPTS=--prefix=/jsp-examples --debug=7 --httpPort=9080 --controlPort=9081 --webroot=%CATALINA_HOME%-5\webapps\jsp-examples --argumentsRealm.passwd.rickk=rickk --argumentsRealm.roles.rickk=test,tomcat --useJNDI --jndi.resource.mail/Session=javax.mail.Session --jndi.param.mail/Session.mail.smtp.host=smtp.ponbiki.org --jndi.param.mail/Session.mail.smtp.user=rickk@ponbiki.org
@rem set WINSTONE_OPTS=--prefix=/examples --controlPort=8081 --webroot=%CATALINA_HOME%\webapps\examples
@rem set WINSTONE_OPTS=--prefix=/examples --debug=7 --webroot=%CATALINA_HOME%-5\webapps\examples
@rem set WINSTONE_OPTS=--prefix=/training --debug=8 --httpPort=9080 --controlPort=8081 --webroot=d:\rick\training\build
@rem set WINSTONE_OPTS=--prefix=/tristero --debug=7 --webroot=d:\download\neurogrid
@rem set WINSTONE_OPTS=--webroot=%CATALINA_HOME%\webapps\m3career --httpPort=80 --controlPort=8081 --debug=7
set WINSTONE_OPTS=--webroot=%WINSTONE_HOME%\build\testWebApp --httpPort=9080 --controlPort=9081 --debug=7 --useInvoker

@rem ********************************************************************
@rem            Uncomment for non-1.4 jdks
@rem ********************************************************************
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\gnujaxp.jar
set CP=%CP%;%WINSTONE_HOME%\dist\xml-apis.jar
set CP=%CP%;%WINSTONE_HOME%\dist\xercesImpl.jar

@rem ********************************************************************
@rem            Uncomment for jsp support
@rem ********************************************************************
set CP=%CP%;%CATALINA_HOME%-5\common\lib\jasper-runtime.jar
set CP=%CP%;%CATALINA_HOME%-5\common\lib\jasper-compiler.jar
set CP=%CP%;%CATALINA_HOME%\common\lib\commons-logging-api.jar
set CP=%CP%;%CATALINA_HOME%-5\common\lib\commons-el.jar
set CP=%CP%;%CATALINA_HOME%\common\lib\ant.jar
set CP=%CP%;%JAVA_HOME%\lib\tools.jar
set WINSTONE_OPTS=%WINSTONE_OPTS% --useJasper

@rem ********************************************************************
@rem            Uncomment for invoker support (ie Tomcat style)
@rem ********************************************************************
set WINSTONE_OPTS=%WINSTONE_OPTS% --useInvoker

set CP=%CP%;c:\java\mail\activation.jar
set CP=%CP%;c:\java\mail\mail.jar

echo Options: %WINSTONE_OPTS%

%JAVA_HOME%\bin\java -server -cp %CP% %JAVA_OPTS% winstone.Launcher %WINSTONE_OPTS%