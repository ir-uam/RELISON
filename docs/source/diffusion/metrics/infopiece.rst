Information pieces metrics
===========================
This group of metrics measures properties of the information diffusion by considering the user-generated contents (information pieces) which are propagated through the network.

* `Information count`_
* `Information Gini complement`_
* `Real propagated recall`_
* `Speed`_

Information count
~~~~~~~~~~~~~~~~~~
This metric measures the average number of information pieces which the users have received over time.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Information count:

Information Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric measures the number of times each information piece has been received, and it finds how balanced the distribution is, by computing the Gini complement.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Information Gini complement:    

Real propagated recall
~~~~~~~~~~~~~~~~~~~~~~~~
This metric measures the proportion of information pieces which have shared by other users in a previous diffusion process which have been received.
We differentiate two variants:

* **Individual:** we measure the proportion of the information pieces propagated by each user in the previous process which have already been received by the user.
* **Global:** we measure the global proportion of the information pieces propagated in the previous process which have already been received by the corresponding users.

Parameters
^^^^^^^^^^
* :code:`unique`: true if we only count the first time an information piece has been received by a user, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual real propagated recall:

whereas, for the global version, it is:

.. code:: yaml

    Global real propagated recall:

Creator Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~
The creator Gini complement measures how balanced is the number of times that the information created by each user has been propagated. We differentiate two variants:

* **Individual:** we measure the number of times each user has received information from each creator, and find the Gini complement of that distribution. The final result is the average over all the users in the network.
* **Global:** for each creator, we count the number of times that one of his/her created contents has reached a user in the network. Then, the Gini complement is computed over that distribution. 

Parameters
^^^^^^^^^^
* :code:`unique`: true if we only count the first time an information piece has been received by a user, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual creator Gini:
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global creator Gini:
      unique:
        type: boolean
        value: true/false



Speed
~~~~~~~~~~~~~~~~
The speed of a simulation measures the number of contents which have been propagated and seen during the simulation (considering that 
each information piece is received only once by a user).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Speed:

