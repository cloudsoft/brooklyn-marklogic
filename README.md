Brooklyn MarkLogic
==================

This project provides [Brooklyn](http://brooklyncentral.github.io/) entities for MarkLogic 7.

Use these entities to:
* Deploy clusters of MarkLogic nodes to Amazon, Rackspace and Google Compute Engine
* Manage database and forest creation and forest replication
* Move forests between nodes in the cluster

To build the project, run:

`mvn clean install`

After completion of this command, you will find a zip and tar.gz distribution in the dist/target directory.

To run the examples, unpack either the zip or tar.gz file and follow the instructions in the [README.md](https://github.com/cloudsoft/brooklyn-marklogic/blob/master/dist/src/main/dist/README.md) in the root.


Watch this space
----------------

The code currently available is step one. It allows one to roll out a new MarkLogic cluster.

We are actively working on additional features for more control and flexibility when deploying and managing MarkLogic clusters. These include:

* Deploy to the Region of your choice, with your favourite AMI.
  Currently it requires a custom MarkLogic AMI, until there is a MarkLogic 7 download available.
* Allow new nodes to bind to existing EBS volumes. 
  For example, if a VM has failed or needs to be taken down for some reason, then a new one can be started to replace it.
* Allow the cluster to scale-up and scale-down.
  Scaling down may involve mounting the EBS volumes for a node's forests on other existing nodes.
  Scaling up may involve mounting existing volumes that are currently attached to existing nodes, to balance the load.
* Support other cloud providers, and support Bring-Your-Own-Nodes (e.g. where you have a list of IPs for pre-existing machines).
