Evaluation of the information diffusion process
================================================
Once the information diffusion has been run, we can evaluate the different properties of the simulation. For this, RELISON provides a program.
In order to execute this program, the following command line has to be used:

.. code:: bash

  java -jar RELISON.jar diffusion-eval configuration network multigraph directed weighted selfloops readtypes user-index info-index input-folder output-folder info (-rec rec-file -n n -test-graph test -userfeats file1,file2,...,fileN -infofeats file1,...,fileN -realprop file)

where:

* :code:`configuration`: a YAML file containing the parameters for the evaluation (See `Configuration file`_ below).
* :code:`network`: path to a file containing the graph.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`readtypes`: true if we want to read the types of the edges, false otherwise.
* :code:`user-index`: a path containing the identifiers of the users (one on each line).
* :code:`info-index`: a path containing the identifiers of the information pieces / user-generated contents (one on each line)
* :code:`input-folder`: a directory containing the simulations to evaluate.
* :code:`output-folder`: a directory for storing the evaluations.
* :code:`info`: a path containing the user-generated contents.
* Optional arguments:
    * :code:`-rec rec-file`: path to a recommendation file, whose edges will be added to the network. Only used if identifying the recommendation edges is important for evaluating the diffusion.
    * :code:`-n` n: the number of links (per user) to add from the recommendation (if any). By default: 10.
    * :code:`-test-graph file`: route to a network file containing additional edges (and shall be used for filtering the recommended edges to add).
    * :code:`-userfeats file1,file2,...,fileN`: a comma-separated list of files containing the features for the users in the network (e.g. communities).
    * :code:`-infofeats file1,file2,...,fileN`: a comma-separated list of files containing the features for the information pieces (e.g. hashtags).
    * :code:`-realprop file`: a file indicating which information pieces have been repropagated by users in another information diffusion process.

Configuration file
~~~~~~~~~~~~~~~~~~~~
The configuration file has the following format:

.. code:: yaml

    metrics:
      metric_name1:
        param_name1:
          type: int/double/boolean/string/long/orientation/object
            value: value
            object: 
              name: object_name
              params:
                param_name1: <...>
          param_name2:
          <...>
      metric_name2:
      <...>
    distributions:
      distribution_name:
        params:
          param_name1:
          <...>
        times: [iter1,...,iterN]
    filters:
      filter_name:
        param_name1:
        <...>

As we can see in the previous code, we have to read three different elements:

* **metrics:** contains the metrics to compute in the simulation. The program computes them every iteration, and then outputs them in a file (see `Output files`_).
* **distributions:** in addition to metrics, we can find distributions (e.g. the number of times each piece has been received). Differently from metrics, the user has to indicate for which iterations the program should find these distributions. This is indicated in the :code:`times` field.
* **filter:** modifies the input data. For instance, it just considers information pieces created before a given timestamp. It should be the same as in the diffusion procedure.

Input files
~~~~~~~~~~~~

Information pieces file
^^^^^^^^^^^^^^^^^^^^^^^^
The information pieces (individual user-generated contents) file needs to have the following format (CSV divided by tabs):

.. code::

    infoId  userId  text  reprCount  likeCount  created  truncated

where

* :code:`infoId`: identifier of the information piece.
* :code:`userId`: identifier of the creator.
* :code:`text`: the content of the information piece.
* :code:`reprCount`: number of times the piece has been repropagated.
* :code:`likeCount`: number of likes the piece has been received.
* :code:`created`: UNIX timestamp indicating the date of creation.
* :code:`truncated`: whether we are taking the complete text, or just a small part.

The text must be in UTF-8 format, and user-generated contents are separated by line skips. Fields (like text) which might have tabs or line skips inside must be properly escaped, and surrounded by "".

Real propagated information file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The file indicating which information pieces where repropagated in another diffusion procedure has the following format (divided by tabs)

.. code::

  userId  infoId  timestamp

where

* :code:`userId`: identifier of the user who repropagated a piece.
* :code:`infoId`: identifier of the repropagated content.
* :code:`timestamp`: UNIX timestamp of the propagation.

Feature files
^^^^^^^^^^^^^^
The files containing information about user / information features have the following format. Each line, they include one user/piece - feature pair, separated by tab.

.. code::

  userId/infoId  featureId

Output files
~~~~~~~~~~~~
We differentiate two types of files: metrics and diffusion files.

Metric files
^^^^^^^^^^^^
For a simulation, the format of each line of these files is the following (tab separated):
All the metrics for the simulation are displayed in the same file. A file per simulation is generated.

.. code::

    numIter metric1 metric2 metric3 ... metricN

In the first line, a header will be displayed, indicating which value corresponds to which metric.

Distribution files
^^^^^^^^^^^^^^^^^^
The format for the distribution files is (tab-separated):

.. code::

    element value