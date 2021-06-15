Sight mechanisms
================
Once some information pieces have been received, users do not pay the same attention to all of them: they just read some of them, who they might be interested in. This mechanism models this: it allows to select which information pieces are observed by the user. We provide the following sight mechanisms in RELISON:


* `All`
* `All not discarded`_
* `All not propagated`_
* `All not discarded nor propagated`_
* `All recommended`_
* `All train`_
* `Count`_

All
~~~~
This mechanism makes users see all the information pieces they have received.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All

All not discarded
~~~~~~~~~~~~~~~~~~
The users read all the received user-generated contents which have not been discarded earlier by the expiration mechanism.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All not discarded

All not propagated
~~~~~~~~~~~~~~~~~~~~
The users read all the received user-generated contents which have not been propagated earlier by the user.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All not propagated

All not discarded nor propagated
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
A combination of the `All not discarded`_ and `All not propagated`_ mechanisms: a user sees all the information pieces who he has never propagated or discarded in earlier iterations.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All not propagated nor discarded

All recommended
~~~~~~~~~~~~~~~~
When this mechanism is used, each user sees all the information pieces coming from recommended users, and which have not previously propagated by the user.

Parameters
^^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All recommended
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL

All train
~~~~~~~~~~~~~~~~
When this mechanism is used, each user sees all the information pieces not coming from recommended users, and which have not previously propagated by the user.

^^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  sight:
    name: All train
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL


Count
~~~~~~
Each user sees, at most, a pre-determined number of the received information pieces.

Parameters
^^^^^^^^^^
* :code:`numSight`: the maximum number of pieces each user sees.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  sight:
    name: Count
    params:
      nuSight: 
        type: int
        value: number_of_pieces

Recommended
~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism defines two probabilities: a probability for seeing pieces coming from recommended links and a probability for seeing pieces coming from the rest of the users.

Parameters
^^^^^^^^^^
* :code:`probRec`: the probability of observing coming from links created via a recommendation
* :code:`orientation`: the neighborhood from which information pieces come. It is used to determine which of the users come from a recommendation and which of them do not.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`probTrain`: the probability of observing an information piece from the original links in the network.

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  selection:
    name: Recommended
    params:
      probRec: 
        type: double
        value: probability
      probTrain:
        type: double
        value: probability
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL