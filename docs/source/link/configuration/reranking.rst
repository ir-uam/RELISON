Reranking experiments
=================================================
After some recommendations have been generated, we might want to rerank them, so we can enhance some structural property. We show here how we can do this using the framework. Once the binary has been generated, we just have to execute the following command:

.. code:: bash

	java -jar RELISON.jar reranking train-network test-network multigraph directed weighted selfloops readtypes rec-folder comm-file config output rec-length max-length (-users test/all -print true/false -reciprocal true/false -distance max -feat-data file index -comms commfile)

where

* :code:`train-network`: a file containing the training social network, which is taken as input to recommenders.
* :code:`test-network` : a file containing the test social network, which is used to evaluate the effectiveness of the recommendation algorithms.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`readtypes`: true if we want to read the types of the edges, false otherwise.
* :code:`rec-folder`: a recommendation file / a directory containing recommendation files.
* :code:`comm-file`: route to a file containing a community partition of the network.
* :code:`config`: a Yaml configuration file for reading the reranking algorithms and the evaluation metrics we want to apply (see `Configuration file`_ below).
* :code:`output`: a directory in which to store the reranked recommendations and the evaluation metrics.
* :code:`rec-length`: the maximum number of links to recommend to each user.
* :code:`max-length`: the maximum number of links on each user recommendation to consider during the reranking phase (min. value should be :code:`rec-length`).
* Optional arguments:
    * :code:`-reciprocal true/false`: in directed networks, this parameter indicates whether we allowed recommending reciprocal edges for the original recommendations or not. By default: `false`.
    * :code:`-distance max directed`: `max` indicates the maximum distance between the target and the candidate users. `directed` indicates whether we wanted to consider link orientation when computing such distance. By default, it does not limit the distance.
    * :code:`-feat-data file index`: in case we want to compute feature-based metrics, `file` specifies the location of a feature file (user-feature-value tab-separated triplets), or an index containing the features. If `index` is equal to true, we consider that `file` points to an index.

Configuration file
~~~~~~~~~~~~~~~~~~

In order to select a suitable set of metrics, the program receives, as input, a configuration file, specifying the different people recommendation methods we 
want to apply and evaluate. This is a Yaml file with the following format:

.. code:: yaml

    rerankers:
      reranker_name1:
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
      reranker_name2:
        <...>
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

    Variant Fraction P@10  R@10  nDCG@10
    Random  0.5 0.001 0.001 0.0013
    Popularity  1.0 0.4 0.23  0.3482

Reranked recommendation file
^^^^^^^^^^^^^^^^^^^^~^^^^^^^
This file contains the reranked recommendations produced for the different users. It does not have a header, and each line has the following (tab-separated) format:

.. code::
    
    TargetUserId  CandidateUserId value

where the target-candidate user pairs are sorted by a) the target user and b) the score (in descending order). Order between users might be arbitrary.

Example:

.. code::

    883345842 10671602  0.7839427836033016
    883345842 242101122 0.7510278151340579
    883345842 230377004 0.6487410202793975
    883345842 19604744  0.6219403238554378
    883345842 398306220 0.6129622813222247
    883345842 181561712 0.525116653773563
    883345842 176566242 0.525116653773563
    883345842 105119490 0.525116653773563
    883345842 11254812  0.5196496019742988
    883345842 11348282  0.5094869396470944
    883609597 430916286 3.08258431711799
    883609597 756033804 2.7629745300415265
    883609597 11254812  2.629591896712651
    <...>