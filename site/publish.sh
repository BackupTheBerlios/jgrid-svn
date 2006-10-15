#!/bin/sh
# Builds and publishes the project web site.

DIR=`dirname $0`
ABSDIR=`(cd $DIR 2> /dev/null && pwd ;)`

source $ABSDIR/forrest-env.sh
ant site
rsync --rsh=ssh -v -z -r $ABSDIR/site/build/site/* jdavis@shell.berlios.de:/home/groups/jgrid/htdocs

