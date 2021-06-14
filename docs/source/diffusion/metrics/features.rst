Feature metrics summary
==================================================
This group of metrics measures properties of the information diffusion in terms of the features of the users and the information pieces.

* `External feature Gini complement`_
* `External feature rate`_
* `External feature recall`_
* `Feature entropy`_
* `Feature Gini complement`_
* `Feature KL divergence`_
* `Feature recall`_

External feature rate
~~~~~~~~~~~~~~~~~~~~~~
This metric considers the proportion of the received features which are unknown to the users. We differentiate two cases:

* **Individual**: we consider the proportion of the received features which are unknown to the each user, and then, we average over the set of people in the network.
* **Global**: we globally count the number of received feature unknown to the users.

In order to determine which features are unknown to the users, we do the following:
* **Information features**: we consider that a feature is unknown to the user if it does not appear in any of the information pieces created by the user.
* **User features**: we consider that a feature is unknown to the user if the user does not have that feature.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual feature rate, we have the following configuration file:

.. code:: yaml

    Individual external feature rate:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

and, for the global version:

.. code:: yaml

    Global external feature rate:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

External feature recall
~~~~~~~~~~~~~~~~~~~~~~~~
For each user in the network, this metric computes the proportion of unknown features in the network which have been received. Then, this values are averaged to obtain the metric.

In order to determine which features are unknown to the users, we do the following:
* **Information features**: we consider that a feature is unknown to the user if it does not appear in any of the information pieces created by the user.
* **User features**: we consider that a feature is unknown to the user if the user does not have that feature.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    External feature recall:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false

External feature Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The external feature Gini complement measures how balanced is the number of times that each feature has been propagated. We also count the cases where the feature was unknown to the receiving user. We differentiate two variants:

* **Individual:** we measure the number of times each user has received each feature, and find the Gini complement of that distribution. The final result is the average over all the users in the network.
* **Global:** for each feature, we count the number of times that it has reached a user in the network. Then, the Gini complement is computed over that distribution. 

In order to determine which features are unknown to the users, we do the following:
* **Information features**: we consider that a feature is unknown to the user if it does not appear in any of the information pieces created by the user.
* **User features**: we consider that a feature is unknown to the user if the user does not have that feature.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual external feature Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

    Global external feature Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

Feature entropy
~~~~~~~~~~~~~~~~~~~~~~~~
The feature entropy measures the entropy of the distribution that counts the times that each feature has been propagated. We differentiate two variants:

* **Individual:** we measure the number of times each user has received each feature, and find the entropy of that distribution. The final result is the average over all the users in the network.
* **Global:** for each creator, we count the number of times that each feature has reached a user in the network. Then, the entropy is computed over that distribution. 

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual feature entropy:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global feature entropy:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false


Feature Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~
The feature Gini complement measures how balanced is the number of times that each feature has been propagated. We differentiate two variants:

* **Individual:** we measure the number of times each user has received each feature, and find the Gini complement of that distribution (independently for each user). The final result is the average over all the users in the network.
* **Global:** for each feature, we count the number of times that it has reached a user in the network. Then, the Gini complement is computed over that distribution. 

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.


Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual feature Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global feature Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

Feature KL divergence
~~~~~~~~~~~~~~~~~~~~~~~~
This metric uses the Kullback-Leibler divergence to compare a) the distribution of times each feature has been propagated during the simulation (the P distribution) and b) an approximation of the actual distribution of the features, estimated by counting the number of pieces created by each user and containing the corresponding features (the Q distribution):

.. math::

    KLD = - \sum_{f \in F} P(f) \log \left( \frac{P(f)}{Q(f)}\right)

We provide two different options:

* **Individual:** we measure the KL-divergence for each user separately, and then we average.
* **Global:** we measure the distributions for the whole network.

The metric measures the information gain achieved by the estimated distribution. 

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.


Configuration file
^^^^^^^^^^^^^^^^^^
For the individual version, the configuration file is the following:

.. code:: yaml

    Individual feature KLD:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

whereas, for the global version, it is:

.. code:: yaml

    Global feature KLD:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false

Feature recall
~~~~~~~~~~~~~~~~~~~~~~~~
For each user in the network, this metric computes the proportion of features in the network which have been received. Then, this values are averaged to obtain the metric.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Feature recall:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false


Feature user entropy
~~~~~~~~~~~~~~~~~~~~~~~~
For each possible value of a feature, this metric counts the number of users who have received it, and computes the entropy of the distribution.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Feature user entropy:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false

Feature user Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
For each possible value of a feature, this metric counts the number of users who have received it, and computes the Gini complement of the distribution, to determine how balanced the distribution is.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Feature user Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false

User-feature count
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric measures the number of different (user,feature) pairs which are observed during the diffusion. The (user, feature) pair is only observed if the user receives a content with the corresponding feature.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    User-feature count:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false

User-feature Gini complement
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric measures the number of times that different (user,feature) pairs which are observed during the diffusion. The (user, feature) pair is only observed if the user receives a content with the corresponding feature. Then, this metric determines how balanced the distribution over those pairs is.

Parameters
^^^^^^^^^^
Both metrics receive the same parameters:

* :code:`feature`: the name of the features we are going to use.
* :code:`userFeature`: true if the studied features are user features, false if they are information pieces features.
* :code:`unique`: true if we just count each time a user receives a piece once, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    User-feature Gini complement:
      feature:
        type: string
        value: feature_name
      userFeature:
        type: boolean
        value: true/false
      unique:
        type: boolean
        value: true/false