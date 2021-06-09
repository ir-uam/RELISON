Feature vector samplers
=========================
In order to select the possible target-candidate user pairs for generating feature vectors for supervised methods, the RELISON library provides several methods. We summarize them below:

* `All`_
* `Distance two`_
* `Distance two link prediction`_
* `Link prediction`_
* `Recommender`_


All
~~~~
This sampler selects all the possible target-candidate user pairs (it just removes pairs in the training test).

Configuration file
^^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
    All:

Distance two
~~~~~~~~~~~~~~
This sampler just selects all the candidate users who share at least a common neighbor with the target user.

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.
    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.
    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Distance two:
    uSel:
      type: orientation
      value:  IN/OUT/UND/MUTUAL
    vSel:
      type: orientation
      value: IN/OUT/UND/MUTUAL

Distance two link prediction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This sampler just selects candidate users who share at least a common neighbor with the target user. It selects all the target-candidate user pairs in that collection, along with an equal number of them.

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.
    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.
    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Distance two link prediction:
    uSel:
      type: orientation
      value:  IN/OUT/UND/MUTUAL
    vSel:
      type: orientation
      value: IN/OUT/UND/MUTUAL

Link prediction
~~~~~~~~~~~~~~~~
This sampler just selects all the target-candidate user pairs in the test set, along with an equal number of negative links (not in the training set).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Link prediction:

Recommender
~~~~~~~~~~~~
For each target user, it takes the top :math:`k` recommended people as the sampled individuals.

Parameters
^^^^^^^^^^
* :code:`k`: the maximum number of target-candidate user pairs to retrieve for each target user.
* :code:`rec`: the recommendation algorithm.

Configuration file:
^^^^^^^^^^^^^^^^^^^^

.. code:: yaml
    
    Recommender:
      k:
        type: int
        value: 1000
      rec:
        type: object
        object:
          name: recommender_name
          params:
            parameter_name1:
              type: parameter_type
              value: parameter_value
            parameter_name2:
            <...>

Any recommendation algorithm can be used here, so, take a look at the algorithm configuration to determine the best option.