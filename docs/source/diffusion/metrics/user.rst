Creator metrics
================
This group of metrics measures properties of the information diffusion by considering the creators of the contents which are propagated through the network. RELISON includes the following metrics:

* `Creator entropy`_
* `Creator Gini complement`_
* `Creator recall`_

Creator entropy
~~~~~~~~~~~~~~~~~~~~~~~~
The creator entropy measures the entropy of the distribution that counts the times that information created by each user has been propagated. We differentiate two variants:

* **Individual:** we measure the number of times each user has received information from each creator, and find the entropy of that distribution. The final result is the average over all the users in the network.
* **Global:** for each creator, we count the number of times that one of his/her created contents has reached a user in the network. Then, the entropy is computed over that distribution. 

Parameters
^^^^^^^^^^
* :code:`unique`: true if we only count the first time an information piece has been received by a user, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual creator entropy:
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global creator entropy:
      unique:
        type: boolean
        value: true/false


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

    Individual creator Gini complement:
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global creator Gini complement:
      unique:
        type: boolean
        value: true/false


Creator recall
~~~~~~~~~~~~~~~~
For each user in the network, this metric computes the proportion of users in the network who have created 
an information piece which has been received by the user. This metric indicates the fraction of people in the
network that each user has discovered through user-generated contents. The value is then averaged over all the user.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Creator recall:

