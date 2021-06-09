Matrix factorization
============================================
Matrix factorization approaches consider that the adjacency matrix of the social network can be factorized in a group of two or three matrices of smaller dimension.
We include the following approaches:

* `Implicit matrix factorization`_
* `Fast implicit matrix factorization`_

Implicit matrix factorization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Matrix factorization algorithm designed to deal with implicit feedback data in recommender systems.

**Reference:** Y. Hu, Y. Koren, C. Volinsky. Collaborative filtering for implicit feedback datasets, International Conference on Data Mining (ICDM 2008) (2008).

Parameters
^^^^^^^^^^
* :code:`lambda`: regulates the importance of the error and the norm of the latent vectors.
* :code:`alpha`: weights the confidence on the weight of the edges.
* :code:`k`: the number of latent factors for each user.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  iMF:
    lambda:
      type: double
      values: [0.1,1,10,100,150]
    alpha:
      type: double
      values: [1,10,40,100]
    k:
      type: int
      range:
        - start: 10
          end: 300
          step: 10
    (weighted:
      type: boolean
      values: [true,false])

Fast implicit matrix factorization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Fast matrix factorization algorithm designed to deal with implicit feedback data in recommender systems.

**Reference:**  I. Pilászy, D. Zibriczky and D. Tikk. Fast ALS-based Matrix Factorization for Explicit and Implicit Feedback Datasets. 4th ACM Conference on Recommender Systems (RecSys 2010),71–78 (2010).

Parameters
^^^^^^^^^^
* :code:`lambda`: regulates the importance of the error and the norm of the latent vectors.
* :code:`alpha`: weights the confidence on the weight of the edges.
* :code:`k`: the number of latent factors for each user.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Fast iMF:
    lambda:
      type: double
      values: [0.1,1,10,100,150]
    alpha:
      type: double
      values: [1,10,40,100]
    k:
      type: int
      range:
        - start: 10
          end: 300
          step: 10
    (weighted:
      type: boolean
      values: [true,false])