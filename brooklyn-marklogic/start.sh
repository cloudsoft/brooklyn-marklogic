#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicApp --location named:marklogic-us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicApp --location "byon:(hosts='ec2-23-22-133-15.compute-1.amazonaws.com,ec2-50-16-86-127.compute-1.amazonaws.com',user=ec2-user)"

#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicApp --location cloudservers-uk
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicApp --location "byon:(hosts='95.138.184.208,5.79.5.133')"

#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicApp --location named:marklogic-eu-west-1
