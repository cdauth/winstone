#! /bin/sh
# $Id$

if [ -z "$JAVA_HOME" ]
then
JAVACMD=`which java`
if [ -z "$JAVACMD" ]
then
echo "Cannot find JAVA. Please set your PATH."
exit 1
fi

JAVA_BINDIR=`dirname $JAVACMD`
JAVA_HOME=$JAVA_BINDIR/..
fi

if [ -z "$ANT_HOME" ] ; then
ANT_HOME="./ant"
fi

JAVACMD=$JAVA_HOME/bin/java

cp=$ANT_HOME/lib/ant.jar:./lib/gnujaxp.jar
cp=$cp:$ANT_HOME/lib/commons-net.jar:$ANT_HOME/lib/ant-launcher.jar
cp=$cp:$ANT_HOME/lib/ant-commons-net.jar:$JAVA_HOME/lib/tools.jar

$JAVACMD -classpath $cp:$CLASSPATH $ANT_OPTS org.apache.tools.ant.Main "$@"
