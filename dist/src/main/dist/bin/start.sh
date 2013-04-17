#!/bin/sh

if [ -z "${BROOKLYN_MARKLOGIC_HOME}" ] ; then
    export BROOKLYN_MARKLOGIC_HOME=$(cd $(dirname $(readlink -f $0 2> /dev/null || readlink $0 2> /dev/null || echo $0))/.. && pwd)
fi

export JAVA_OPTS="-Dbrooklyn.catalog.url=${BROOKLYN_MARKLOGIC_HOME}/conf/marklogic-catalog.xml -Xms256m -Xmx1g -XX:MaxPermSize=256m"
export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/conf/:${BROOKLYN_MARKLOGIC_HOME}/lib/*
brooklyn launch
