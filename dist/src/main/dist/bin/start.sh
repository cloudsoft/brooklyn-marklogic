#!/bin/sh

export BROOKLYN_CLASSPATH=${PWD}/../lib/*;${PWD}/../conf/*;
brooklyn launch
