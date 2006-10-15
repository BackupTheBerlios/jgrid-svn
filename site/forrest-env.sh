#!/bin/sh
ANT_HOME=/opt/apache-ant-1.6.5
JAVA_HOME=/usr/java/jdk1.5.0_09
# Cygwin note: You can't symlink to forrest, ant doesn't know
# how to resolve symlinks other than it's own directory.
FORREST_HOME=/opt/apache-forrest-0.7
PATH=$PATH:$FORREST_HOME/bin:$ANT_HOME/bin:$JAVA_HOME/bin
case "`uname`" in
  CYGWIN*)
    FORREST_HOME=`cygpath --mixed $FORREST_HOME`
    ANT_HOME=`cygpath --mixed $ANT_HOME`
    ;;
esac
export PATH
export JAVA_HOME
export FORREST_HOME

