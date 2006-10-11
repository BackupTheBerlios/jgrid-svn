#!/bin/sh

UNAME=`uname`
DIR=`dirname $0`
ABSDIR=`(cd $DIR 2> /dev/null && pwd ;)`

function findJavaHome
{
    for javadir in $@
    do
        javaname=`ls -1 $javadir 2> /dev/null | grep 'j2sdk1.4.2' | tail -n 1`
	if [[ ! -z "$javaname" ]] ; then
		return
	fi
    done
    javaname=""
    javadir=""
}

function findJava
{
    case "$UNAME" in
        CYGWIN*)
            if [ "${JAVA_HOME:-}" == "" ]; then
                findJavaHome '/cygdrive/c' '/cygdrive/c/java'
                JAVA_HOME="$javadir/$javaname"
            else
                JAVA_HOME=`cygpath --mixed $JAVA_HOME`
            fi
            ;;
        Linux*)
            if [ "${JAVA_HOME:-}" == "" ] ; then
                findJavaHome '/usr/java' '/kdx/app/java' '/share/app/java' '/opt'
                JAVA_HOME="$javadir/$javaname"
            else
                JAVA_HOME=$JAVA_HOME
            fi
            ;;
    esac

    if [[ -z "$JAVA_HOME" ]] ; then
	echo "ERROR: Couldn't find java!"
	exit -1
    fi
    if [[ ! -d $JAVA_HOME ]] ; then
        echo "ERROR: $JAVA_HOME does not exist!  Try setting JAVA_HOME in your ~/.bashrc file or something."
        exit -1
    fi

    if [[ ! -f $JAVA_HOME/lib/tools.jar ]] ; then
        echo "ERROR: tools.jar was not found in $JAVA_HOME/lib !"
        exit -1
    fi
}

# --- main ---

findJava

DIRLIBS="`ls $ABSDIR/lib`"
for i in ${DIRLIBS}; do 
	if [ "$i" != "${DIRLIBS}" ] ; then
	    if [ -z "$LOCALCLASSPATH" ] ; then
		    LOCALCLASSPATH="$ABSDIR/lib/$i"
	    else
		    LOCALCLASSPATH="$ABSDIR/lib/$i":$LOCALCLASSPATH
	    fi
	fi
done

LOCALCLASSPATH="$ABSDIR/jegrid.jar:$LOCALCLASSPATH"
echo $LOCALCLASSPATH
$JAVA_HOME/bin/java -cp $LOCALCLASSPATH -Djava.net.preferIPv4Stack=true org.jegrid.ServerMain $@
