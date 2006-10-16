#!/bin/sh

UNAME=`uname`
DIR=`dirname $0`
ABSDIR=`(cd $DIR 2> /dev/null && pwd ;)`

source $DIR/functions.sh

# --- main ---

findJava
setLocalClasspath

echo $LOCALCLASSPATH
$JAVA_HOME/bin/java -cp $LOCALCLASSPATH -Djava.net.preferIPv4Stack=true org.jegrid.ServerMain $@
