Feature vector generation
=================================================
In order to execute the supervised people recommendation / link prediction algorithms, it is first necessary to compute the feature vectors for the different links involved in the recommendation. Here, we provide information about how these features can be computed:

.. code:: bash

	java -jar RELISON.jar featuregen train-network test-network multigraph directed weighted selfloops readtypes config output rec-length (-users test/all -print true/false -reciprocal true/false -distance max -feat-data file index -comms commfile)

where

* :code:`train-instance-network`: a file containing a network for retrieving the feature vectors for the training set.
* :code:`train-class-network`: a file containing a network for retrieving the relevance of each training instance.
* :code:`test-instance-network`: a file containing a network for retrieving the feature vectors for the test set.
* :code:`test-class-network`: a file containing a network for retrieving the relevance of each test instance.
* :code:`directed`: true if the network to study is directed, false otherwise.
* :code:`weighted-sampling`: true if we use edge weights for the sampler selecting a suitable set of target-candidate user pairs.
* :code:`weighted-classes`: true if we use edge weights as class outputs (otherwise, binary classes).
* :code:`weighted-features`: true if we use edge weights when computing the features.

* :code:`config`: a Yaml file containing the information about a) samplers, b) metrics and algorithms for computing features (see `Configuration file`_ below).
* :code:`train-sampling`: a Yaml configurator containing the information about the sampling method for training target-candidate user pairs.
* :code:`test-sampling`: a Yaml configurator containing the sampling method for the test target-candidate user pairs.
* :code:`train-network` : a file containing the test social network, which is used to evaluate the effectiveness of the recommendation algorithms.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`readtypes`: true if we want to read the types of the edges, false otherwise.
* :code:`config`: a Yaml configuration file for reading the people recommendation algorithms and the evaluation metrics we want to apply (see `Configuration file`_ below).
* :code:`output`: a directory in which to store the structural properties.
* :code:`rec-length`: the maximum number of links to recommend to each user.
* Optional arguments:
    * :code:`-users test/all`: indicates whether to generate recommendations for all the users (`all`) or just for those who have links in the test set (`test`). By default: `all`.
    * :code:`-print true/false`: indicates whether we want to print the recommendations or not. By default: `true`.
    * :code:`-reciprocal true/false`: in directed networks, this parameter indicates whether we want to recommend reciprocal edges or not. By default: `false`.
    * :code:`-distance max directed`: `max` indicates the maximum distance between the target and the candidate users. `directed` indicates whether we want to consider link orientation when computing such distance. By default, it does not limit the distance.
    * :code:`-feat-data file index`: in case we want to compute feature-based metrics, `file` specifies the location of a feature file (user-feature-value tab-separated triplets), or an index containing the features. If `index` is equal to true, we consider that `file` points to an index.
    * :code:`-comms commfile`: route to a file containing a community partition of the network.

Configuration file
~~~~~~~~~~~~~~~~~~

In order to select a suitable set of metrics, the program receives, as input, a configuration file, specifying the features we want to use. These features can be either a) the score of a recommendation for the link or b) the result of an structural metric. The configuration file is as it follows:

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
          - start: <...>
          objects:
            name_of_the_object:
              param_name1:
                type: int/double/boolean/string/long/orientation/object
                <...>
              param_name2:
                type: int/double/boolean/string/long/orientation/object
                <...>
      algorithm_name2:
        ...
    metrics:
      metric_name1:
        type: vertex/pair
          params:
            param_name1:
              type: int/double/boolean/string/long/orientation/object
              values: [value1,value2,...,valueN] / value
              range:
              - start: startingValue
                end: endingValue
                step: stepValue
              - start: <...>
              objects:
                name_of_the_object:
                  param_name1:
                    type: int/double/boolean/string/long/orientation/object
                    <...>
                  param_name2:
                    type: int/double/boolean/string/long/orientation/object
                    <...>
      metric_name2:
        <...>

where :code:`algorithms` shows the part of the configuration file dedicated to the parameter grid of the people recommendation algorithms, whereas the :code:`metric` tag shows the start of the structural metrics to use. At the moment, this program does only admit metrics working over users (they are applied over both
the target and candidate users) and over pairs of users.

Finally, there are two more configuration files to consider, which apply a sampling strategy to select a suitable set of target-user candidate pairs for generating the features. These configuration files are considered as follows:

.. code:: yaml

     samplers:
        sampler_algorithm:
          param_name1:
            type: int/double/boolean/string/long/orientation/object
            value: value
            object:
              name_of_the_object:
                name: object_name
                params:
                  object_param_name1:
                    type: int/double/boolean/string/long/orientation/object
                    value: value
                  object_param_name2:
                    type: int/double/boolean/string/long/orientation/object
                    <...>
                  <...>

Output files
~~~~~~~~~~~~
This program produces as outcome the LETOR feature files. For more information about this format, see http://terrier.org/docs/v4.0/learning.html