@echo off
REM convience bat file to build with

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set the JAVA_HOME environment variable to point at your JDK
goto finish
:gotJavaHome

set _ANT_HOME=%ANT_HOME%
set ANT_HOME=ant

set _ANT_OPTS=%ANT_OPTS%

set _CLASSPATH=%CLASSPATH%
set CLASSPATH=%ANT_HOME%\lib\ant.jar;%ANT_HOME%\lib\jaxp.jar;%ANT_HOME%\lib\crimson.jar
set CLASSPATH=%CLASSPATH%;%ANT_HOME%\lib\optional.jar;%JAVA_HOME%\lib\tools.jar

%JAVA_HOME%\bin\java %ANT_OPTS% org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

:clean

rem clean up classpath after
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=
set ANT_HOME=%_ANT_HOME%
set _ANT_OPTS=
set ANT_OPTS=%_ANT_OPTS%
set _ANT_HOME=
:finish
