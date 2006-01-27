#!/bin/bash

# Find the real path where this script lives.
whoami=`basename $0`
#trick to resolve symlinks
whereami=`echo $0 | sed -e "s#^[^/]#\`pwd\`/&#"`
whereami=`dirname $whereami`
source $whereami/set-env.sh
cd $whereami


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

if [ "$cygwin" = "true" ] ; then
    # Set a default prompt of: user@host and current_directory
    PS1='\[\033]0;\w\007
\033[0;33m\]\u@\h \[\033[1;35m\w\033[0m\]
$ '
    export PS1
fi

# Launch a new shell
exec $SHELL

