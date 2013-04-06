#!/bin/sh

export BROOKLYN_CLASSPATH=${PWD}/../lib/*;${PWD}/../conf/*;
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-uswest2
