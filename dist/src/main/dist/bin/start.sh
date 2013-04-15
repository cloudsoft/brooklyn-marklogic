#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=..
export JAVA_OPTS="-Dbrooklyn.catalog.url=${BROOKLYN_MARKLOGIC_HOME}/conf/marklogic-catalog.xml -Xms256m -Xmx1g -XX:MaxPermSize=256m"
export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/lib/*
brooklyn launch
