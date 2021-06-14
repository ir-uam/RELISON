Effects on the network structure
=================================================
The RELISON framework provides a program for determining the effect of link prediction and recommendation algorithms might have over the network structure. For this, the program adds the set of recommended links over the network structure, and computes structural metrics over that expanded network. In order to use this program, we use the following command:

.. code:: bash

	java -jar RELISON.jar effects train-network test-network multigraph directed weighted selfloops readtypes rec-folder comm-files config output rec-length full-graph onlyrel

where

* :code:`train-network`: a file containing the training social network, which is taken as input to recommenders.
* :code:`test-network` : a file containing the test social network.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`rec-folder`: a recommendation file / a directory containing recommendation files.
* :code:`config`: a Yaml configuration file for reading the structural metrics we want to apply (see `Configuration file`_ below).
* :code:`output`: a directory in which to store the metrics.
* :code:`rec-length`: the maximum number of recommended links to each user to consider.
* :code:`full-graph`: true if we use all edges/pairs to compute edge/pair metrics.
* :code:`onlyrel`: true if we only add the relevant edges to the training network.
* **Optional arguments:**
    * :code:`-communities file1,file2,...,fileN`: a comma-separated list of files containing a community partition of the network.
    * :code:`--distances`: true if we want to precompute distances between the users (by default: false). Recommended if we use several distance-based metrics.
    * :code:`--prediction user/global`: we include this option if we want to read the outcome of a link prediction. Then, :code:`user` indicates that we add :code:`rec-length` predicted links per user to the expanded network, whereas :code:`global` indicates that we just add the top :code:`rec-length` of the prediction.


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