#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location "byon:(hosts=ec2-54-224-165-91.compute-1.amazonaws.com,user=ec2-user)"

#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location cloudservers-uk
#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-eu-west-1