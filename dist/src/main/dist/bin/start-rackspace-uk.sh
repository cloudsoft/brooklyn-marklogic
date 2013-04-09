#!/bin/sh
export BROOKLYN_CLASSPATH=${PWD}/../lib/*;
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location cloudservers-uk
