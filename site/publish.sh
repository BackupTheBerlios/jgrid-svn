#!/bin/sh
# Builds and publishes the project web site.

DIR=`dirname $0`
ABSDIR=`(cd $DIR 2> /dev/null && pwd ;)`

source $ABSDIR/forrest-env.sh
ant site
RC=$?
if [ $RC -ne 0 ] ; then
	echo "ant returned $RC"
	exit -1
fi
rsync --rsh=ssh -v -z -r $ABSDIR/build/site/* jdavis@shell.berlios.de:/home/groups/jgrid/htdocs

