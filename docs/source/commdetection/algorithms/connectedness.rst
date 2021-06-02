Connectedness community detection algorithms
============================================
This family of community detection algorithms considers whether two users are connected in a network or not to determine the community partition of the network. We differentiate two methods:

* `Strongly connected components`_
* `Weakly connected components`_


Strongly connected components
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this algorithm, two nodes belong to the same component if there is a finite length path between them in both directions. This method considers the network directionality. In undirected networks, it is equivalent to the `Weakly connected components`_ algorithm.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Strongly connected components:

Weakly connected components
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this algorithm, two nodes belong to the same component if there is a finite length path between them without considering the direction of the edges. 

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Weakly connected components: