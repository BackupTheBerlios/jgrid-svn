#!/bin/bash

DEFAULT_ANT_HOME=/opt/apache-ant-1.6.5
DEFAULT_FORREST_HOME=/opt/apache-forrest-0.7
DEFAULT_JAVA_HOME=/usr/java/j2sdk1.4.2_12

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

if [ -z "$ANT_HOME" ] ; then
    if [ -d $DEFAULT_ANT_HOME ] ; then
        export ANT_HOME=$DEFAULT_ANT_HOME
    else
        echo "ERROR: Unable to locate ANT. Please set ANT_HOME."
        exit -1
    fi
fi

if [ -z "$FORREST_HOME" ] ; then
    if [ -d $DEFAULT_FORREST_HOME ] ; then
        export FORREST_HOME=$DEFAULT_FORREST_HOME
    else
        echo "ERROR: Unable to locate FORREST. Please set FORREST_HOME."
        exit -1
    fi
fi

if [ -z "$JAVA_HOME" ] ; then
    export JAVA_HOME=$DEFAULT_JAVA_HOME
fi

if [ ! -d "$JAVA_HOME" ] ; then
    echo "ERROR: JAVA_HOME does not point to a valid directory! $JAVA_HOME"
    exit -1
fi

export PATH=$JAVA_HOME/bin:$ANT_HOME/bin:$FORREST_HOME/bin:$PATH

# Cygwin-ify the home directory environment variables.
if [ "$cygwin" = "true" ] ; then
  export FORREST_HOME=`cygpath --mixed "$FORREST_HOME"`
  export JAVA_HOME=`cygpath --mixed "$JAVA_HOME"`
  export ANT_HOME=`cygpath --mixed "$ANT_HOME"`
fi
