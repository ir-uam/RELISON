Distributions
=======================
For a group of elements, distribution measure their individual properties in a given moment of the simulation. This framework includes the following distributions:

* `Features`_
* `Information`_

Features
~~~~~~~~~~~~~~~~~~~~~~
This distributions measure properties of the different features. We differentiate three distributions here:

* **Information feature**: this distribution measures the amount of times that information pieces containing this feature have been received by the users in the simulation (or, in different words, how many times each feature has been received).
* **User feature**: this distribution measures times pieces created by people with these features have been received.
* **Mixed feature**: this distribution considers how many times information pieces containing a certain information feature and created by a user with a certain user feature have been received.

Parameters
^^^^^^^^^^
In the case of the individual feature cases (both the information and the user cases), the configuration receives just one parameter:

* :code:`feature`: the name of the user/information feature to use.

In the case of the mixed feature case, we take two:

* :code:`userFeature`: the name of the user feature we are going to use.
* :code:`infoFeature`: the name of the information pieces feature we are going to use.

Configuration file
^^^^^^^^^^^^^^^^^^
For the information feature distribution, we have the following configuration file:

.. code:: yaml

    Information features:
      feature:
        type: string
        value: feature_name

for the user feature distribution, it is:

.. code:: yaml

    User features:
      feature:
        type: string
        value: feature_name

and, for the mixed case:

.. code:: yaml

    Mixed features:
      userFeature:
        type: string
        value: feature_name
      infoFeature:
        type: string
        value: feature_name

Information
~~~~~~~~~~~~~~~~~~~~~~~~
For each information piece, this distribution measures how many times it has been received.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Information:

Users
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
For each user in the network, this distribution measures how many information pieces they have received.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Users: