Experimental configuration
===========================
RELISON provides a program for executing information diffusion simulations.

.. code:: bash

  java -jar RELISON.jar diffusion configuration output numreps network multigraph directed weighted selfloops readtypes user-index info-index info (-rec rec-file -n n -test-graph test -userfeats file1,file2,...,fileN -infofeats file1,...,fileN -realprop file -previous file)

where:

* :code:`configuration`: a YAML file containing the simulation parameters (See `Configuration file`_ below).
* :code:`output`: the directory for storing the outcomes of the simulation.
* :code:`numreps`: the number of executions of each simulation.
* :code:`network`: path to a file containing the graph.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`readtypes`: true if we want to read the types of the edges, false otherwise.
* :code:`user-index`: a path containing the identifiers of the users (one on each line).
* :code:`info-index`: a path containing the identifiers of the information pieces / user-generated contents (one on each line)
* :code:`info`: a path containing the user-generated contents.
* Optional arguments:
    * :code:`-rec rec-file`: path to a recommendation file, whose edges will be added to the network.
    * :code:`-n n: the number of links (per user) to add from the recommendation (if any). By default: 10.
    * :code:`-test-graph file`: route to a network file containing additional edges (and shall be used for filtering the recommended edges to add).
    * :code:`-userfeats file1,file2,...,fileN`: a comma-separated list of files containing the features for the users in the network (e.g. communities).
    * :code:`-infofeats file1,file2,...,fileN`: a comma-separated list of files containing the features for the information pieces (e.g. hashtags).
    * :code:`-realprop file`: a file indicating which information pieces have been repropagated by users in another information diffusion process.
    * :code:`-previous file`: file containing the result of a previous diffusion procedure.

Configuration file
~~~~~~~~~~~~~~~~~~~~
The configuration file has the following format:

.. code:: yaml

    simulations:
    - protocol:
        name: protocol_name
        type: preconfigured
        params:
          param_name1:
            type: int/double/boolean/string/long/orientation/object
            value: value
            object: 
              name: object_name
              params:
                param_name1: <...>
          param_name2:
          <...>
      stop:
        name: stop_condition_name
        params:
          param_name1:
          <...>
      filters:
        filter_name:
          param_name1:
          <...>
    - protocol:
        name: protocol_name
        type: custom
        selection:
          name: selection_name
          params:
            param_name1:
              type: <...>
        expiration:
          name: selection_name
          params:
            param_name1:
              type: <...>
        update:
          name: update_name
          params:
            param_name1:
              type: <...>
        propagation:
          name: propagation_name
          params:
            param_name1:
              type: <...>
        sight:
          name: sight_name
          params:
            param_name1:
              type: <...>        
    - protocol:  <...>

As we can see in the previous code, each element in the list corresponds to a different simulation. Each simulation consists on three different elements:

* **filter:** modifies the input data. For instance, it just considers information pieces created before a given timestamp.
* **stop:** the stop condition of the simulation (after no information is propagated, after a given timestamp is reached...)
* **protocol:** the simulation protocol. Indicates how the information travels through the network. We differentiate two types of protocol:
    *preconfigured*: the protocol is fully implemented in the library
    *custom*: we build a new protocol by combining its different elements.

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
For each simulation, this program generates an output file. However, this output file is binary (and therefore, it cannot be easily read with a text editor). However, it can be read using the provided code.