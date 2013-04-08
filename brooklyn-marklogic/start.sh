#!/bin/sh

#export JAVA_OPTS=" -Dlogback.configurationFile=logback.xml -Xms256m -Xmx1g -XX:MaxPermSize=256m"
export BROOKLYN_CLASSPATH=$(pwd)/target/classes
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1