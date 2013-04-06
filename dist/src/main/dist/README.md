Brooklyn MarkLogic
==================

This project contains Brooklyn entities for MarkLogic, and an example application which deploys it to to Amazon.

Setting up Brooklyn
==================

The MarkLogic Brooklyn integration relies on Brooklyn 0.5.0-rc.1

Check the following page on how to install Brooklyn:

http://brooklyncentral.github.io/start/download.html


Configuring Marklogic in Brooklyn:
==================

The following properties need to added to ~/.brooklyn/brooklyn.properties

brooklyn.location.named.marklogic-useast1=jclouds:aws-ec2:us-east-1
brooklyn.location.named.marklogic-useast1.imageId=us-east-1/ami-4e2ab027
brooklyn.location.named.marklogic-useast1.user=ec2-user

Starting the examples:
==================

Go to the bin directory and execute ./start-ec2.sh. This will first spawn a new EC2 machine and then installs MarkLogic.