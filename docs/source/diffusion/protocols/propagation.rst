Propagation mechanisms
======================
The propagation mechanism selects the people in the network who shall receive each separate information piece.
RELISON provides the following propagation mechanisms:

* `All neighbors`_
* `All recommended neighbors`_
* `Pull`_
* `Pull-push`_
* `Pull-push pure recommended`_
* `Pull-push recommended`_
* `Push`_


All neighbors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism sends the information pieces to all the neighbors of the user.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  propagation:
    name: All neighbors
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL

All recommended neighbors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism sends the information pieces to all the recommended neighbors of the user (i.e. it only propagates information through links created by a link recommendation).

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  propagation:
    name: All recommended neighbors
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL

Pull
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this mechanism, each user selects one of his/her neighbors. Then, the selected neighbor sends him/her the information. After a neighbor is selected, some time needs to pass before it can be selected again.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`waitTime`: the amount of time that needs to pass before a neighbor can be selected again.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Pull
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      waitTime:
        type: int
        value: num_iter


Pull-push
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this mechanism, we combine the `Push`_ and the `Pull`_ mechanisms: here, each user selects a neighbor. Then, he/she sends the information pieces to that neighbor, and the neighbor sends the information to him/her. After a neighbor is selected, some time needs to pass before it can be selected again.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`waitTime`: the amount of time that needs to pass before a neighbor can be selected again.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Push-pull
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      waitTime:
        type: int
        value: num_iter        

Pull-push pure recommended
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism is just a version of the `Pull-push`_ propagation mechanism, that limits the neighbor selection to the links created by a recommendation algorithm.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`waitTime`: the amount of time that needs to pass before a neighbor can be selected again.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Pull-push pure recommended
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      waitTime:
        type: int
        value: num_iter

Pull-push recommended
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism is just a version of the `Pull-push`_ propagation mechanism, that, with a given probability :math:`p`, selects a neighbors between the connections by a recommendation algorithm, and, with probability :math:`1-p`, selects one of the original neighbors of the user.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`waitTime`: the amount of time that needs to pass before a neighbor can be selected again.
* :code:`prob`: the probability of selecting a recommended neighbor.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  propagation:
    name: Pull-push recommended
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      waitTime:
        type: int
        value: num_iter
      prob:
        type: double
        value: probability



Push
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this mechanism, each user selects one of his/her neighbors. Then, he/she sends all the information pieces to that neighbor. After a neighbor is selected, some time needs to pass before it can be selected again.

Parameters
^^^^^^^^^^
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`waitTime`: the amount of time that needs to pass before a neighbor can be selected again.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  propagation:
    name: Push
    params:
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      waitTime:
        type: int
        value: num_iter        