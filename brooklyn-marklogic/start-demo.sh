#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=.
export BROOKLYN_CLASSPATH=$(pwd)/conf/:$(pwd)/target/classes:$(pwd)/../demo-war/target/
brooklyn launch --app io.cloudsoft.marklogic.demo.MarkLogicDemoApp --location named:marklogic-us-east-1
#brooklyn launch --app io.cloudsoft.marklogic.demo.MarkLogicDemoApp --location localhost

#brooklyn launch --app io.cloudsoft.marklogic.demo.MarkLogicDemoApp --location "byon:(hosts=ec2-54-235-9-187.compute-1.amazonaws.com,user=ec2-user)"

#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location cloudservers-uk
#brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-eu-west-1
