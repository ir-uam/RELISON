Experimental configuration
==========================

In our framework, we include a program that simplifies measuring the structural properties of a given network. This program can be executed as follows, once the 
binary for the library has been generated:

.. code:: bash

	java -jar RELISON.jar sna network multigraph directed weighted selfloops metrics output (-communities comm1,comm2,...,commN --distances)

where

* :code:`network`: a file containing the social network graph to analyze.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`metrics`: a configuration file for reading the structural properties we want to measure (see `Configuration file`_ below).
* :code::code:`output`: a directory in which to store the structural properties.
* Optional parameters:
	* :code:`-communities comm1,comm2,...,commN`: a comma-separated list of files containing community partitions of the users.
	* :code:`--distances`: indicates that we want to pre-compute the distance-based metrics in the network (recommended if more than one is used).

Configuration file
~~~~~~~~~~~~~~~~~~

In order to select a suitable set of metrics, the program receives, as input, a configuration file, specifying the different properties we 
want to measure and analyze. This is a Yaml file with the following format:

.. code:: yaml

    metrics:
      metric_name1:
        type: vertex/edge/pair/graph/indiv. community/global community
          params:
            param_name1:
              type: int/double/boolean/string/long/orientation/object
              values: [value1,value2,...,valueN] / value
              range:
              - start: startingValue
                end: endingValue
                step: stepValue
              - start: ...
              objects:
                name_of_the_object:
                  param_name1:
                    type: int/double/boolean/string/long/orientation/object
                    ...
                  param_name2:
                    type: int/double/boolean/string/long/orientation/object
                    ...
      metric_name2:
        ...

In this configuration file, we identify each metric by each name, and, afterwards, we identify its type. We differentiate between six groups of metrics:

* **Vertex metrics:** Properties of individual nodes in the network (e.g. degree, local clustering coefficient).
* **Edge/Pair metrics:** Properties of pairs of users in the network. If they are selected with the "edge" identifier, the metrics are only computed over the set of links in the network.
* **Graph metrics:** Global properties of the network (e.g. global clustering coefficient).
* **Individual community metrics:** Properties of a single community in the partition (e.g. community size, degree).
* **Global community metrics:** Global metrics depending on the community partition (e.g. modularity).