#!/bin/sh

if [ -z "${BROOKLYN_MARKLOGIC_HOME}" ] ; then
    BROOKLYN_MARKLOGIC_HOME=$(cd $(dirname $(readlink -f $0 2> /dev/null || readlink $0 2> /dev/null || echo $0))/.. && pwd)
fi

export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/lib/*
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1
