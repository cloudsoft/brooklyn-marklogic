#!/bin/sh
export BROOKLYN_CLASSPATH=${PWD}/../lib/*;
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1
