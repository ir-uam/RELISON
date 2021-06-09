Link prediction experiments
=================================================
The RELISON framework provides a program for predicting the next links to appear in the social network. After the binary for the library is generated,
we can use the program with the following terminal command:

.. code:: bash

	java -jar RELISON.jar prediction train-network test-network multigraph directed weighted selfloops readtypes config output rec-length (-users test/all -print true/false -reciprocal true/false -distance max -feat-data file index -comms commfile)

where

* :code:`train-network`: a file containing the training social network, which is taken as input to recommenders.
* :code:`test-network` : a file containing the test social network, which is used to evaluate the effectiveness of the recommendation algorithms.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`readtypes`: true if we want to read the types of the edges, false otherwise.
* :code:`config`: a Yaml configuration file for reading the people recommendation algorithms and the evaluation metrics we want to apply (see `Configuration file`_ below).
* :code:`output`: a directory in which to store the structural properties.
* Optional arguments:
    * :code:`-users test/all`: indicates whether to generate predictions for all the users (`all`) or just for those who have links in the test set (`test`). By default: `all`.
    * :code:`-print true/false`: indicates whether we want to print the prediction or not. By default: `true`.
    * :code:`-reciprocal true/false`: in directed networks, this parameter indicates whether we want to predict reciprocal edges or not. By default: `false`.
    * :code:`-distance max directed`: `max` indicates the maximum distance between the target and the candidate users. `directed` indicates whether we want to consider link orientation when computing such distance. By default, it does not limit the distance.
    * :code:`-feat-data file index`: in case we want to compute feature-based metrics, `file` specifies the location of a feature file (user-feature-value tab-separated triplets), or an index containing the features. If `index` is equal to true, we consider that `file` points to an index.
    * :code:`-comms commfile`: route to a file containing a community partition of the network.

Configuration file
~~~~~~~~~~~~~~~~~~

In order to select a suitable set of metrics, the program receives, as input, a configuration file, specifying the different people recommendation methods we 
want to apply and evaluate. This is a Yaml file with the following format:

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
        param_name1:
          type: int/double/boolean/string/long/orientation/object
          <...>
        param_name2:
          <...>
      metric_name2:
      <...>

where :code:`algorithms` shows the part of the configuration file dedicated to the parameter grid of the people recommendation algorithms, whereas the :code:`metric` tag shows the start of the evaluation metric section of the Yaml file.

Output files
~~~~~~~~~~~~
This program produces two outcomes: the evaluation file and the reranked recommendations.

Evaluation file
^^^^^^^^^^^^^^^^
This file contains the evaluation metrics for each algorithm. The first line contains the header, whereas the rest
show the metric values for a single algorithm. Each line has the following (tab-separated) format:

.. code::

    Variant Fraction metric1 metric2 <...> metricN

where fraction represents the number of the algorithm (divided by the total number of algorithms in the comparison).

For example:

.. code::

    Variant Fraction AUC  F1-score@0.5
    Random  0.5 0.5 0.5
    Popularity  1.0 0.8 0.78

Link prediction file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This file contains the predictions produced by the algorithm. It does not have a header, and each line has the following (tab-separated) format:

.. code::
    
    TargetUserId  CandidateUserId value

where the target-candidate user pairs are sorted by b) the score (in descending order).

Example:

.. code::

    883345842 10671602  0.7839427836033016
    113213211 242101122 0.7510278151340579
    342433442 230377004 0.6487410202793975
    234324232 19604744  0.6219403238554378
    567578745 398306220 0.6129622813222247
    234232333 181561712 0.525116653773563
    <...>