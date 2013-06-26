#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicTestApplication --location named:marklogic-us-east-1c
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicTestApplication --location "byon:(hosts='ec2-54-227-14-251.compute-1.amazonaws.com,ec2-50-19-1-97.compute-1.amazonaws.com',user=ec2-user)"

#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicTestApplication --location cloudservers-uk
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicTestApplication --location "byon:(hosts='94.236.40.98,94.236.40.163')"

#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicTestApplication --location named:marklogic-eu-west-1
