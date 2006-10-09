#!/bin/sh
export JAVA_HOME=/usr/java/j2sdk1.4.2_10
DIR=`dirname $0`
ABSDIR=`(cd $DIR 2> /dev/null && pwd ;)`
DIRLIBS="`ls $ABSDIR/../lib`"
for i in ${DIRLIBS}; do 
	if [ "$i" != "${DIRLIBS}" ] ; then
	    if [ -z "$LOCALCLASSPATH" ] ; then
		LOCALCLASSPATH=$i
	    else
		LOCALCLASSPATH="$ABSDIR/../lib/$i":$LOCALCLASSPATH
	    fi
	fi
done

LOCALCLASSPATH="$ABSDIR/../idea/jegrid_classes:$LOCALCLASSPATH"
echo $LOCALCLASSPATH
$JAVA_HOME/bin/java -cp $LOCALCLASSPATH -Djava.net.preferIPv4Stack=true org.jegrid.ServerMain $@
