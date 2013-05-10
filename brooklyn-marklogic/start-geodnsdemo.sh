#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes:$(pwd)/../demo-war/target/
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.GeoScalingMarklogicDemoApplication --location aws-ec2:us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.GeoScalingMarklogicDemoApplication --location aws-ec2:eu-west-1,aws-ec2:ap-southeast-1,aws-ec2:us-west-1