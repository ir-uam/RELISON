Nearest neighbors algorithms
============================================
Nearest neighbors recommendation algorithms select the users in the network that better represent either the target or the candidate users. We include the following
collaborative filtering variants of these algorithms:

* `User-based kNN`_
* `Item-based kNN`_
* `Similarities`_

User-based kNN
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The user-based kNN algorithm chooses the most similar users to the target user of the recommendation.

**Reference:** X. Ning, C. Desrosiers, G. Karypis. A Comprehensive Survey of Neighborhood-Based Recommendation Methods. Recommender Systems Handbook, 2nd. Ed., 37-76 (2015).

Parameters
^^^^^^^^^^
* :code:`k`: the maximum number of neighbors to choose.
* :code:`similarity`: the method to choose (see `Similarities`_)
* :code:`q`: the exponent of the similarity.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  UB kNN:
    k:
      type: int
      range:
        - start: 10
          end: 300
          step: 10
    q:
      type: int
      values: 1
    similarity:
      type: object
      objects:
        similarity_name1:
          similarity_param1:
          <...>
    (weighted:
      type: boolean
      values: [true,false])

Item-based kNN
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The item-based kNN algorithm chooses the most similar users to the candidate user of the recommendation.

**Reference:** X. Ning, C. Desrosiers, G. Karypis. A Comprehensive Survey of Neighborhood-Based Recommendation Methods. Recommender Systems Handbook, 2nd. Ed., 37-76 (2015).

Parameters
^^^^^^^^^^
* :code:`k`: the maximum number of neighbors to choose.
* :code:`similarity`: the method to choose (see `Similarities`_)
* :code:`q`: the exponent of the similarity.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  IB kNN:
    k:
      type: int
      range:
        - start: 10
          end: 300
          step: 10
    q:
      type: int
      values: 1
    similarity:
      type: object
      objects:
        similarity_name1:
          similarity_param1:
          <...>
    (weighted:
      type: boolean
      values: [true,false])

Similarities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The set of all the similarities which can be applied for kNN is large. This number even grows in contact recommendation in social networks,
as we can use any possible standalone algorithm as a neighborhood selection method. We allow this in our experiments: any people recommendation
algorithm can be selected. The specific grid for these similarities is the same as in the original method.