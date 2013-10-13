#!/bin/sh
if [ -z "${BROOKLYN_MARKLOGIC_HOME}" ] ; then
    export BROOKLYN_MARKLOGIC_HOME=$(cd $(dirname $(readlink -f $0 2> /dev/null || readlink $0 2> /dev/null || echo $0))/.. && pwd)
fi

export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/conf/:${BROOKLYN_MARKLOGIC_HOME}/lib/*:${BROOKLYN_MARKLOGIC_HOME}/lib/
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicDemoApplicationWithoutVolumes --location named:marklogic-gce

