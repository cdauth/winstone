@echo off

set JAVA_HOME=c:\java\jdk1.4.2
set WINSTONE_HOME=d:\rick\winstone
set JAVA_OPTS=-Djava.endorsed.dirs=%JAVA_HOME%\jre\lib\ext

set CP=%WINSTONE_HOME%\dist\winstone.jar
@rem set WINSTONE_OPTS=-prefix /training -debug 7 -webroot ..
@rem set WINSTONE_OPTS=-prefix /examples -debug 7 -webroot d:\download\tomcat\examples
@rem set WINSTONE_OPTS=-prefix /training -debug 7 -webroot d:\rick\training\build
set WINSTONE_OPTS=-prefix /tristero -debug 7 -webroot d:\download\neurogrid

@rem ********************************************************************
@rem            Uncomment for non-1.4 jdks
@rem ********************************************************************
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\jaxp.jar
@rem set CP=%CP%;%WINSTONE_HOME%\build\lib\crimson.jar

@rem ********************************************************************
@rem            Uncomment for jsp support
@rem ********************************************************************
@rem set CP=%CP%;c:\java\tomcat\common\lib\jasper-runtime.jar
@rem set CP=%CP%;c:\java\tomcat\common\lib\jasper-compiler.jar
@rem set CP=%CP%;c:\java\tomcat\common\lib\ant.jar
@rem set CP=%CP%;%WINSTONE_HOME%\development\lib\jsp-servlet.jar
@rem set CP=%CP%;%JAVA_HOME%\lib\tools.jar
@rem set WINSTONE_OPTS=%WINSTONE_OPTS% -jasper true

%JAVA_HOME%\bin\java -server -cp %CP% %JAVA_OPTS% com.rickknowles.winstone.Listener %WINSTONE_OPTS%