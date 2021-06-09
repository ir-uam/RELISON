Experimental configuration
======================================

In our framework, we include a program that simplifies finding the community division of a network. This program can be executed as follows, once the 
binary for the library has been generated:

.. code:: bash

	java -jar RELISON.jar communities network multigraph directed weighted selfloops algorithms output

where

* `network`: a file containing the social network graph to analyze.
* `multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* `directed`: true if the network is directed, false otherwise.
* `weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* `selfloops`: true if we allow links between a node and itself, false otherwise.
* `algorithms`: a Yaml configuration file for reading the community detection algorithms we want to apply (see `Configuration file`_ below).
* `output`: a directory in which to store the structural properties.

Configuration file
~~~~~~~~~~~~~~~~~~

In order to select a suitable set of metrics, the program receives, as input, a configuration file, specifying the different properties we 
want to measure and analyze. This is a Yaml file with the following format:

.. code:: yaml

    algorithms:
      algorithm_name1:
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
      algorithm_name2:
        ...

Output file
~~~~~~~~~~~
Once the community detection algorithm is generated, the community partition is stored into a file. Such file has the following format, where each line is tab-separated:

.. code::

    node-id comm-id

where `node-id` is the identifier of the user in the network, and `comm-id` is the community number.
