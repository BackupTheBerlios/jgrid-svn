#!/bin/bash

cygwin=false;
darwin=false;
case `uname` in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

if [ -z "$FORREST_HOME" ] ; then
    export FORREST_HOME=/usr/local/apache-forrest-0.7
fi

if [ -z "$JAVA_HOME" ] ; then
    export JAVA_HOME=/usr/java/j2sdk1.4.2_09
fi

if [ ! -d "$JAVA_HOME" ] ; then
    echo "ERROR: JAVA_HOME does not point to a valid directory! $JAVA_HOME"
    exit -1
fi

export PATH=$JAVA_HOME/bin:$FORREST_HOME/bin:$PATH

# Cygwin-ify the home directory environment variables.
if [ "$cygwin" = "true" ] ; then
  export FORREST_HOME=`cygpath --mixed "$FORREST_HOME"`
  export JAVA_HOME=`cygpath --mixed "$JAVA_HOME"`
fi
