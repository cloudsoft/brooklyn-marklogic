#!/bin/sh
if [ -z "${BROOKLYN_MARKLOGIC_HOME}" ] ; then
    export BROOKLYN_MARKLOGIC_HOME=$(cd $(dirname $(readlink -f $0 2> /dev/null || readlink $0 2> /dev/null || echo $0))/.. && pwd)
fi

export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/conf/:${BROOKLYN_MARKLOGIC_HOME}/lib/*
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.GeoScalingMarkLogicDemoApplication --location aws-ec2:us-east-1,aws-ec2:us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.GeoScalingMarkLogicDemoApplication --location aws-ec2:eu-west-1,aws-ec2:ap-southeast-1,aws-ec2:us-west-1

