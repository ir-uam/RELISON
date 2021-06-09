Path-based algorithms
============================================
These algorithms consider the paths between different nodes in the network to find the recommendation scores. The framework contains the following algorithms within this family.

* `Global Leicht-Holme-Newman index`_
* `Katz`_
* `Local path index`_
* `Matrix forest`_
* `Pseudo inverse cosine`_
* `Shortest path distance`_

Global Leicht-Holme-Newman index
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm considers that two users are similar if their immediate neighbors in the network are themselves similar.

**Reference:** E.A. Leicht, P. Holme, M.E.J. Newman. Vertex Similarity in Networks. Physical Review E 73(2): 026120 (2006).

Parameters
^^^^^^^^^^
* :code:`phi`: decay factor for the similarity as we use longer paths between the users. 
* :code:`orientation`: the method to choose the adjacency matrix. This parameter does not influence in undirected networks.
    * :code:`IN`: coordinate :math:`A_{ij}` shows the weight of the :math:`(j,i)` edge.
    * :code:`OUT`: coordinate :math:`A_{ij}` shows the weight of the :math:`(i,j)` edge.
    * :code:`UND`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges.
    * :code:`MUTUAL`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges (but only if both exist).
* :code:`q`: the exponent of the similarity.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Global LHN:
      phi:
        type: double
        values: [0.01,0.1,1,10,100]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]

Katz
~~~~
This algorithm weights the possible paths between the target and candidate users, giving more weight to those at closer distances.

**References:** 
    * L. Katz. A new status index derived from sociometric analysis. Psychometrika 18(1), 39-43 (1953)
    * D. Liben-Nowell, D., J. Kleinberg. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7) (2007)

Parameters
^^^^^^^^^^
* :code:`b`: decay factor for the greater distance paths.
* :code:`orientation`: the method to choose the adjacency matrix. This parameter does not influence in undirected networks.
    * :code:`IN`: coordinate :math:`A_{ij}` shows the weight of the :math:`(j,i)` edge.
    * :code:`OUT`: coordinate :math:`A_{ij}` shows the weight of the :math:`(i,j)` edge.
    * :code:`UND`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges.
    * :code:`MUTUAL`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges (but only if both exist).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Katz:
      b:
        type: double
        values: [0.01,0.1,1,10,100]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]

Local path index
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm weights the possible paths between the target and candidate users, giving more weight to those at closer distances.

**References:** 
    * L. Lü, C. Jin, T. Zhou. Similarity Index Based on Local Paths for Link Prediction of Complex Networks. Physical Review E 80(4): 046122 (2009)
    * L. Lü, T. Zhou. Link Prediction in Complex Networks: A survey. Physica A 390(6), 1150-1170 (2011)

Parameters
^^^^^^^^^^
* :code:`b`: decay factor for the greater distance paths.
* :code:`k`: the maximum distance between the target and candidate users (greater or equal than 3).
* :code:`orientation`: the method to choose the adjacency matrix. This parameter does not influence in undirected networks.
    * :code:`IN`: coordinate :math:`A_{ij}` shows the weight of the :math:`(j,i)` edge.
    * :code:`OUT`: coordinate :math:`A_{ij}` shows the weight of the :math:`(i,j)` edge.
    * :code:`UND`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges.
    * :code:`MUTUAL`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges (but only if both exist).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Local path index:
      b:
        type: double
        values: [0.01,0.1,1,10,100]
      k: 
        type: int
        values: [3,4,5,6]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]

Matrix forest
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm takes as score the ratio of the number of spanning divergent forests such that the target and candidate belong to the same tree, rooted in the
target user.

**References:** 
    * L. Lü, T. Zhou. Link Prediction in Complex Networks: A survey. Physica A 390(6), 1150-1170 (2011)

Parameters
^^^^^^^^^^
* :code:`alpha`: importance of the Laplacian matrix.
* :code:`orientation`: the method to choose the adjacency matrix. This parameter does not influence in undirected networks.
    * :code:`IN`: coordinate :math:`A_{ij}` shows the weight of the :math:`(j,i)` edge.
    * :code:`OUT`: coordinate :math:`A_{ij}` shows the weight of the :math:`(i,j)` edge.
    * :code:`UND`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges.
    * :code:`MUTUAL`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges (but only if both exist).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Matrix forest:
      alpha:
        type: double
        values: [0.01,0.1,1,10,100]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]

Pseudo inverse cosine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm represents each user by the u-th row of the pseudo-inverse of the Laplacian matrix of the network. Then, the score is just the cosine similarity between these two vectors.

**References:** 
    * F. Fouss, A. Pirotte, J-M. Renders, M. Saerens. Random-walk computation of similarities between nodes of a graph with application to collaborative recommendation. IEEE TKDE 19(3), pp. 355-369 (2007).


Parameters
^^^^^^^^^^
* :code:`orientation`: the method to choose the adjacency matrix. This parameter does not influence in undirected networks.
    * :code:`IN`: coordinate :math:`A_{ij}` shows the weight of the :math:`(j,i)` edge.
    * :code:`OUT`: coordinate :math:`A_{ij}` shows the weight of the :math:`(i,j)` edge.
    * :code:`UND`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges.
    * :code:`MUTUAL`: coordinate :math:`A_{ij}` shows the sum of the weights of the :math:`(i,j)` and :math:`(j,i)` edges (but only if both exist).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Pseudo-inverse cosine:
      alpha:
        type: double
        values: [0.01,0.1,1,10,100]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]


Shortest path distance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The shortest path distance recommends people in the network who are close to the target user (the closest, the highest the score shall be)-

**Reference:** D. Liben-Nowell and J. Kleinberg.  The link prediction problem for social networks. 12th International Conference on Information and Knowledge Management (CIKM  2003), ACM, 556-559 (2003).

Parameters
^^^^^^^^^^
* :code:`orientation`: the orientation to choose for the edges.
    * :code:`IN`: it considers the distance between the candidate user and the target user.
    * :code:`OUT`: it considers the distance between the target and the candidate user.
    * :code:`UND`: it considers the distance between the target and candidate users if the network was undirected.
    * :code:`MUTUAL`: in this case, we consider just the most natural distance (the :code:`OUT` case).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Distance:
    orientation:
      type: orientation
      values: [IN,OUT,UND]

