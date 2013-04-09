#!/bin/sh
export JAVA_OPTS="-Dbrooklyn.catalog.url=../conf/marklogic-catalog.xml -Xms256m -Xmx1g -XX:MaxPermSize=256m"
export BROOKLYN_CLASSPATH=${PWD}/../lib/*;
brooklyn launch
