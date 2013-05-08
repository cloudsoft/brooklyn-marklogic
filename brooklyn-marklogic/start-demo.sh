#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes:$(pwd)/../demo-war/target/
brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicDemoApp --location named:marklogic-us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicDemoApp --location "byon:(hosts='ec2-23-22-133-15.compute-1.amazonaws.com,ec2-50-16-86-127.compute-1.amazonaws.com',user=ec2-user)"


#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicDemoApp --location localhost

#brooklyn launch --app io.cloudsoft.marklogic.brooklynapplications.MarkLogicDemoApp --location "byon:(hosts=ec2-54-224-37-173.compute-1.amazonaws.com,user=ec2-user)"

