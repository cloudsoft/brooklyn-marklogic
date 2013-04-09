Brooklyn MarkLogic
==================

This project contains Brooklyn entities for MarkLogic, and an example application which deploys it to to Amazon.

To build the project, run:

`mvn clean install`

After completion of this command, you will find the zip & tar.gz distribution in the dist/target directory.

For more information about how to run the examples, unpack the zip or tar.gz file and following the README.md in the root.


Watch this space
----------------

The code currently available is just the first step. It allows one to rollout a new MarkLogic cluster.

We are actively working on additional features for more control and flexibility when deploying and managing MarkLogic clusters. These include:

* Deploy to the Region of your choice, with your favourite AMI.
  Currently it requires a custom MarkLogic AMI, until there is a MarkLogic 7 download available.
* Allow new nodes to bind to existing EBS volumes. 
  For example, if a VM has failed or needs to be taken down for some reason, then a new one can be started to replace it.
* Allow the cluster to scale-up and scale-down.
  Scaling down may involve mounting the EBS volumes for a node's forests on other existing nodes.
  Scaling up may involve mounting existing volumes that are currently attached to existing nodes, to balance the load.
* Support other cloud providers, and support Bring-Your-Own-Nodes (e.g. where you have a list of IPs for pre-existing machines).
