Brooklyn MarkLogic
==================

This project provides [Brooklyn](http://brooklyncentral.github.io/) entities for MarkLogic 7.

Use these entities to:
* Deploy clusters of MarkLogic nodes to AWS, Rackspace and Google Compute Engine
* Auto-scale clusters to meet changing demand
* Manage database creation, forest creation and forest replication
* Keep your forests on volumes rather than the VM's disk
* Move forests between nodes of a cluster

To build the project run `mvn clean install`. When Maven is done you will find a zip and tar.gz distribution in the dist/target directory.

To run the examples, unpack either the zip or tar.gz file and follow the instructions in the [README.md](https://github.com/cloudsoft/brooklyn-marklogic/blob/master/dist/src/main/dist/README.md) in the root.


Watch this space
----------------

We are actively working on additional features for more control and flexibility when deploying and managing MarkLogic clusters. These include:
* Allowing new nodes to bind to existing EBS volumes. For example if a VM fails failed or needs to be terminated then a replacement can be started very quickly.
* Automatically scaling clusters of d- and e-nodes up and down depending on load and other custom policies.
* Balancing load on d-nodes by redistributing forests as clusters resize.
* Deploying multiple clusters and setting up replication between them in different regions, or even different cloud providers.

We are keen to hear about your favourite use-cases. Contact us at info@cloudsoftcorp.com.

================

&copy; Cloudsoft Corporation 2013. All rights reserved.

Use of this software is subject to the Cloudsoft EULA, provided in LICENSE.txt and at http://www.cloudsoftcorp.com/cloudsoft-developer-license

