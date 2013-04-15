#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/target/classes
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location cloudservers-uk
#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-eu-west-1