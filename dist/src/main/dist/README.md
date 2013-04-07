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

brooklyn.location.named.marklogic-us-east-1=jclouds:aws-ec2:us-east-1
brooklyn.location.named.marklogic-us-east-1.imageId=us-east-1/ami-4e2ab027
brooklyn.location.named.marklogic-us-east-1.user=ec2-user

brooklyn.jclouds.aws-ec2.identity=<your amazon identity>
brooklyn.jclouds.aws-ec2.credential=<your amazon credentials>
brooklyn.jclouds.aws-ec2.minRam=2500
brooklyn.marklogic.aws-access-key=<your amazon identity>
brooklyn.marklogic.aws-secret-key=<your amazon credentials>

brooklyn.markLogic.licenseKey=<your marklogic license key>
brooklyn.markLogic.licensee=<your marklogic licensee name>
brooklyn.marklogic.fcount=4
brooklyn.marklogic.cluster=<clustername>

Since we are relying on EC2 for the examples, the brooklyn.jclouds.aws-ec2.* properties need to be provided.

Starting the examples:
==================

Go to the bin directory and execute ./start-ec2.sh. This will first spawn a new EC2 machine and then installs MarkLogic.