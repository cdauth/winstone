@echo off

set JAVA_HOME=c:\java\jdk1.4.2
set WINSTONE_HOME=d:\rick\winstone
set JAVA_OPTS=-Djava.endorsed.dirs=%JAVA_HOME%\jre\lib\ext

set CP=%WINSTONE_HOME%\dist\winstone.jar
@rem set WINSTONE_OPTS=--prefix=/examples --debug=7 --httpPort=9080 --controlPort=9081 --webroot=c:\java\tomcat\webapps\examples
@rem set WINSTONE_OPTS=--prefix=/examples --debug=7 --webroot=c:\java\tomcat\webapps\examples
set WINSTONE_OPTS=--prefix=/training --debug=7 --httpPort=8080 --controlPort=8081 --webroot=d:\rick\training\build
@rem set WINSTONE_OPTS=--prefix=/tristero --debug=7 --webroot=d:\download\neurogrid

@rem ********************************************************************
@rem            Uncomment for non-1.4 jdks
@rem ********************************************************************
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\gnujaxp.jar

@rem ********************************************************************
@rem            Uncomment for jsp support
@rem ********************************************************************
@rem set CP=%CP%;c:\java\tomcat\common\lib\jasper-runtime.jar
@rem set CP=%CP%;c:\java\tomcat\common\lib\jasper-compiler.jar
@rem set CP=%CP%;c:\java\tomcat\common\lib\ant.jar
@rem set CP=%CP%;%WINSTONE_HOME%\development\lib\jsp-servlet.jar
@rem set CP=%CP%;%JAVA_HOME%\lib\tools.jar
@rem set WINSTONE_OPTS=%WINSTONE_OPTS% --useJasper

@rem ********************************************************************
@rem            Uncomment for invoker support (ie Tomcat style)
@rem ********************************************************************
@rem set WINSTONE_OPTS=%WINSTONE_OPTS% --useInvoker

%JAVA_HOME%\bin\java -server -cp %CP% %JAVA_OPTS% com.rickknowles.winstone.Launcher %WINSTONE_OPTS%
