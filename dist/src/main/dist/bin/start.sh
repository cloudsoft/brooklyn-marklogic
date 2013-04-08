#!/bin/sh
export JAVA_OPTS=" -Dlogback.configurationFile=../conf/logback.xml -Xms256m -Xmx1g -XX:MaxPermSize=256m"
export BROOKLYN_CLASSPATH=${PWD}/../lib/*;
brooklyn launch
