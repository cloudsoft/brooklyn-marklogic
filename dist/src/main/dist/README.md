Brooklyn MarkLogic
==================

This project contains Brooklyn entities for MarkLogic, and example applications to deploy to Amazon, Rackspace or Google Compute Engine. Before getting started, make sure you have installed Brooklyn. If you're a new user, follow Brooklyn's [quickstart guide](http://brooklyncentral.github.io/use/guide/quickstart/index.html).


Configuring Marklogic for Brooklyn
==================================

Add the following properties to `~/.brooklyn/brooklyn.properties` and substitute your data for <wrapped values>. Default values have been given in brackets where available.

```
brooklyn.marklogic.licensee=<your marklogic licensee name>
brooklyn.marklogic.license-key=<your marklogic license key>
brooklyn.marklogic.license-type=<(development) the type of the license. use evaluation if part of the early access program>
brooklyn.marklogic.cluster=<a name for your clusters>
brooklyn.marklogic.version=7.0-20131006
# Necessary to download the MarkLogic installer
brooklyn.marklogic.website-username=<your marklogic website login name>
brooklyn.marklogic.website-password=<your marklogic website password>
```

To run in AWS, you should give these properties:
```
brooklyn.location.jclouds.aws-ec2.identity=<your amazon identity>
brooklyn.location.jclouds.aws-ec2.credential=<your amazon credential>
brooklyn.location.jclouds.aws-ec2.imageId=us-east-1/ami-7d7bfc14
brooklyn.location.named.marklogic-us-east-1=jclouds:aws-ec2:us-east-1c
brooklyn.location.named.marklogic-us-east-1.minRam=2500
```

To run in Rackspace, you should give these properties:
```
brooklyn.location.named.marklogic-rackspace-uk=jclouds:rackspace-cloudservers-uk
brooklyn.location.named.marklogic-rackspace-uk.identity=<rackspace identity>
brooklyn.location.named.marklogic-rackspace-uk.credential=<rackspace credential>
brooklyn.location.named.marklogic-rackspace-uk.imageNameRegex=CentOS 6.4
brooklyn.location.named.marklogic-rackspace-uk.minRam=2000
brooklyn.location.named.marklogic-rackspace-uk.openIptables=true
```

Finally, to run in Google Compute Engine, you should give these properties:
```
# Configures general Google Compute Engine credentials
brooklyn.location.jclouds.google-compute-engine.identity=<gce identity>
brooklyn.location.jclouds.google-compute-engine.credential=<gce credential>
brooklyn.location.jclouds.google-compute-engine.privateKeyFile=<path to private key>
brooklyn.location.jclouds.google-compute-engine.publicKeyFile=<path to public key>

# Configures specifics for MarkLogic in Google Compute Engine
brooklyn.location.named.marklogic-gce=jclouds:google-compute-engine
brooklyn.location.named.marklogic-gce.imageId=centos-6-v20130325
brooklyn.location.named.marklogic-gce.uri=https://www.googleapis.com/compute/v1beta15/projects/google/global/images/centos-6-v20130325
brooklyn.location.named.marklogic-gce.imageNameRegex=.*centos-6-v20130325.*
brooklyn.location.named.marklogic-gce.openIptables=true
```

Running the demo
================

Assuming the `brooklyn` program is on your PATH, run one of `start-ec2.sh`, `start-rackspace.sh` or `start-gce.sh` from the bin directory. Brooklyn's management console will be available at http://localhost:8081 with default login credentials of admin:password. 

The application creates:
* A cluster of three d-nodes
* A cluster of one e-node
* One nginx server to front the e-node cluster

Follow the progress of the launch from the console, or open `brooklyn.log` for much more detailed logging.  Eventually Brooklyn will print details of the cluster it has created:

```
2013-10-11 18:03:59,935 INFO  =========================== MarkLogicDemoApp: Starting postStart =========================== 
2013-10-11 18:03:59,935 INFO  MarkLogic Nginx http://162.222.178.5
2013-10-11 18:03:59,935 INFO  MarkLogic Cluster is available at 'http://162.222.178.143:8000'
2013-10-11 18:03:59,936 INFO  MarkLogic Cluster summary is available at 'http://162.222.178.143:8001'
2013-10-11 18:03:59,936 INFO  E-Nodes
2013-10-11 18:03:59,936 INFO     1 MarkLogic node http://162.222.178.143:8000
2013-10-11 18:03:59,936 INFO  D-Nodes
2013-10-11 18:03:59,936 INFO     1 MarkLogic node http://162.222.177.121:8000
2013-10-11 18:03:59,936 INFO     2 MarkLogic node http://162.222.178.77:8000
2013-10-11 18:03:59,936 INFO     3 MarkLogic node http://162.222.178.185:8000
2013-10-11 18:03:59,936 INFO  MarkLogic Monitoring Dashboard is available at 'http://162.222.178.143:8002/dashboard'
```
It then creates a database with two replicated forests. When running in Rackspace and AWS, Brooklyn will create and attach volumes to virtual machines for these forests. When running in GCE Brooklyn just uses the local disk.

When start up completes, try using the web console to resize the d or e-node clusters. Check the MarkLogic console to see how it manages forests.


Finishing up
============

Terminate Brooklyn with ctrl+c or the `kill` command. Brooklyn will dispose of the VMs it created, but check your cloud provider's console to be sure.
