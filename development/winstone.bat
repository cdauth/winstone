@echo off

set JAVA_HOME=c:\java\jdk1.4.2
set WINSTONE_HOME=d:\rick\winstone
set CATALINA_HOME=c:\java\tomcat
set JAVA_OPTS=-Djava.endorsed.dirs=%JAVA_HOME%\jre\lib\ext

set CP=%CP%;%WINSTONE_HOME%\dist\winstone.jar
set WINSTONE_OPTS=--prefix=/examples 
set WINSTONE_OPTS=%WINSTONE_OPTS% --webroot=%CATALINA_HOME%\webapps\examples
set WINSTONE_OPTS=%WINSTONE_OPTS% --debug=7
set WINSTONE_OPTS=%WINSTONE_OPTS% --commonLibFolder=d:\lib 
set WINSTONE_OPTS=%WINSTONE_OPTS% --javaHome=%JAVA_HOME%
set WINSTONE_OPTS=%WINSTONE_OPTS% --argumentsRealm.passwd.rickk=rickk --argumentsRealm.roles.rickk=test,tomcat
set WINSTONE_OPTS=%WINSTONE_OPTS% --useJNDI --jndi.resource.mail/Session=javax.mail.Session --jndi.param.mail/Session.mail.smtp.host=smtp.ponbiki.org --jndi.param.mail/Session.mail.smtp.user=rickk@ponbiki.org

@rem ********************************************************************
@rem            Uncomment for non-1.4 jdks
@rem ********************************************************************
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\gnujaxp.jar
set CP=%CP%;%WINSTONE_HOME%\dist\xml-apis.jar
set CP=%CP%;%WINSTONE_HOME%\dist\xercesImpl.jar

@rem ********************************************************************
@rem            Uncomment for jsp support
@rem ********************************************************************
@rem set CP=%CP%;%CATALINA_HOME%-5\common\lib\jasper-runtime.jar
@rem set CP=%CP%;%CATALINA_HOME%-5\common\lib\jasper-compiler.jar
@rem set CP=%CP%;%CATALINA_HOME%\common\lib\commons-logging-api.jar
@rem set CP=%CP%;%CATALINA_HOME%-5\common\lib\commons-el.jar
@rem set CP=%CP%;%CATALINA_HOME%\common\lib\ant.jar
@rem set CP=%CP%;%JAVA_HOME%\lib\tools.jar
set WINSTONE_OPTS=%WINSTONE_OPTS% --useJasper

@rem ********************************************************************
@rem            Uncomment for invoker support (ie Tomcat style)
@rem ********************************************************************
set WINSTONE_OPTS=%WINSTONE_OPTS% --useInvoker

set CP=%CP%;c:\java\mail\activation.jar
set CP=%CP%;c:\java\mail\mail.jar

echo Options: %WINSTONE_OPTS%

%JAVA_HOME%\bin\java -server -cp %CP% %JAVA_OPTS% winstone.Launcher %WINSTONE_OPTS%