
Modularity-based community detection algorithms
============================================
This family of community detection algorithms considers network properties to compute the partition. We have implemented the following community detection algorithms.

* `FastGreedy`_
* `Girvan-Newman`_
* `Infomap`_
* `Label propagation`_
* `Louvain`_
* `Spectral clustering`_

FastGreedy
~~~~~~~~~~~~~
This is an agglomerative clustering method which starts with each node on a different community, and, each step, joins the pair of communities which increases modularity the most (or decreases it the least). From the dendogram, we take the partition maximizing the modularity of the network.

In the framework, we consider three variants of these method in addition to the original version. These additional variants attempt to find not only a set of communities maximizing the modularity of the network, but, at the same time, balancing the number of users on each community. The three additional variants are:

* **Balanced FastGreedy**: fixes a maximum community size, and, it applies again the community detection algorithm over the communities larger than the fixed size.
* **Gini weighted FastGreedy**: joins the pair of communities that, at the same time, increase the modularity and minimizes the Gini index of the community size distribution. Then, it chooses the partition maximizing the modularity (as earlier).
* **Size weighted FastGreedy**: joins the pair of communities that maximize the modularity and minimize the distance between the number of users in the communities to join and the average number of users on each community.

**Reference:** M.E.J. Newman. Fast Algorithm for detecting community structure in networks. Physical Review E 69(6): 066133 (2004)

Parameters
^^^^^^^^^^

The original FastGreedy approach does not consider any parameters, but the balanced and Gini weighted variants do.

For the **balanced FastGreedy** algorithm, we have the following parameters:

* *size*: the maximum allowed community size.

whereas for the **Gini weighted FastGreedy**, we take the following parameter:

* *lambda*: the weight given to the Gini index of the size distribution.

Configuration file
^^^^^^^^^^^^^^^^^^

The original FastGreedy algorithm has the following configuration file:

.. code:: yaml

    FastGreedy:

whereas the balanced variant has this one:

.. code:: yaml

  Balanced FastGreedy:
    size:
      type: int
      values: [1,10,100,1000]

the Gini weighted approach has the following:

.. code:: yaml

  Gini weighted FastGreedy:
    lambda:
      type: double
      values: [0.1,1,10,100]

and the size weighted variant:

.. code:: yaml

  Size weighted FastGreedy:

Girvan-Newman
~~~~~~~~~~~~~
The Girvan-Newman algorithm generates the community partition by gradually removing the edge with the largest betweenness in the network. It generates a dendogram, and the partition that maximizes the modularity of the network is selected.

**Reference:** M. Girvan, M.E.J. Newman. Community structure in social and biological networks, Proc. Natl. Acad. Sci. USA 99, 7821–7826 (2002)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Girvan-Newman:


Infomap
~~~~~~~
The Infomap algorithm computes a community partition of the network by computing the minimum length necessary for describing a random walk in the network. For this it uses a two-level Huffman compressing code: the first one differentiates communities in the network, and the second nodes inside of each community.

To compute this metric, we call to the original implementation of the algorithm, provided by the authors in http://mapequation.org.

**Reference:** M. Rosvall and C. Bergstrom. Maps of random walks on complex networks reveal community structure. Proceedings of the National Academy of Sciences 105(4), pp. 1118-1123 (2008)

Parameters
^^^^^^^^^
* *trials*: the number of iterations of the most external loop of the algorithm.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  Infomap:
    trials:
      type: int
      values: [1,5,10]

Label propagation
~~~~~~~~~~~~~~~~~
The label propagation algorithm starts with all nodes in different communities.
Then, iteratively, each node selects the community of the majority of its neigbors, until everything converges.

**Reference:** U.N. Raghavan, R. Albert, S. Kumara. Near linear time algorithm to detect communities in large-scale networks. Physical Review E 76: 036106 (2007).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  Label propagation:


Louvain
~~~~~~~~~~~~~~~~~
The Louvain algorithm applies a multi-level community detection algorithm. It starts with all the nodes in different communities, and, iteratively, moves a node to another community in the network where the increment in the modularity is maximum.

When the modularity does not vary, it condenses the network, so communities are now 
the nodes, and applies the algorithm over that condensed network.

**Reference:**  V. Blondel, J. Guillaume, R. Lambiotte, E. Lefebvre, Fast unfolding of communities in large networks. Journal of Statistical Mechanics 10 (2008)

Parameters
^^^^^^^^^
* *threshold*: the minimum variance of the modularity. If in an iteration it changes less than this threshold, we end the phase.



Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
    Louvain:
      threshold:
        type: double
        values: [0.001,0.01,0.1,1]

Spectral clustering
^^^^^^^^^^^^^^^^^^^
The spectral clustering algorithm is a community detection technique for finding a balanced set of communities. It uses the max-flow min-cut theory to find a partition such as the number of edges between two sets is minimized, where a cut between two communities is just the number of edges between them. 

We consider two variants of this algorithm:

* **Ratio cut spectral clustering**: Minimizes the ratio cut of the partition, which is defined as:

.. math::

    \mbox{RatioCut}(\mathcal{G}|\mathcal{C}) = \frac{1}{|\mathcal{C}|} \sum_c \frac{|\{(u,v) \in E | u \in c \wedge v \notin c\}|}{|c|}

* **Normalized cut spectral clustering**: Minimizes the normalized cut of the partition, defined as:

.. math::

    \mbox{RatioCut}(\mathcal{G}|\mathcal{C}) = \frac{1}{|\mathcal{C}|} \sum_c \frac{|\{(u,v) \in E | u \in c \wedge v \notin c\}|}{\mbox{vol}(c)}

where

.. math::

  \mbox{vol}(c) = \sum_{v\in c} |\Gamma(v)|

**Reference:** R. Zafarani, M.A. Abassi, H. Liu. Social Media Mining: An Introduction. Chapter 6. 2014

Parameters
^^^^^^^^^
* *k*: the desired number of communities.

Configuration file
^^^^^^^^^^^^^^^^^^

For the ratio cut version, the configuration file would look as:

.. code:: yaml
  
    Ratio cut spectral clustering:
      k:
        type: int
        values: [10,20,30]

and, for the normalized cut version:

.. code:: yaml
  
    Normalized cut spectral clustering:
      k:
        type: int
        values: [10,20,30]        