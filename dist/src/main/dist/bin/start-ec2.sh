#!/bin/sh

export BROOKLYN_MARKLOGIC_HOME=..
export BROOKLYN_CLASSPATH=${BROOKLYN_MARKLOGIC_HOME}/lib/*
brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp --location named:marklogic-us-east-1
