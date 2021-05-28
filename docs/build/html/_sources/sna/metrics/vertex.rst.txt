Vertex metrics
==============

SoNALiRe integrates the following vertex metrics.


* `Betweenness`_
* `Closeness`_
* `Coreness`_
* `Degree`_
* `Eccentricity`_
* `Eigenvector centrality`_
* `Harmonic centrality`_
* `HITS`_
* `Katz centrality`_
* `Local clustering coefficient`_
* `PageRank`_
* `Reciprocity rate`_
* `Length`_

Betweenness
~~~~~~~~~~~
The betweenness centrality measures, for each node, the number of shortest paths which pass through the vertex.

.. math::

	\mbox{Betweenness}(u) = \sum_{(v,w): u\neq v,w} \frac{\sigma_{vw}(u)}{\sigma_{vw}}

where :math:`\sigma_{vw}` is the number of shortest paths between :math:`v` and :math:`w`, and  :math:`\sigma_{vw}(u)` is the number of
shortest paths which pass through node :math:`u`.

**Reference:** M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)

Parameters
^^^^^^^^^^

* *normalize:* true if we want to normalize the coefficient, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Betweenness:
      type: vertex
      params:
        normalize:
          type: boolean
          values: [true, false]

Closeness
~~~~~~~~~
This metric computes how close the studied user is to the rest of the nodes in the network. It is computed as:

.. math::

	\mbox{Closeness}(u) = \frac{|\mathcal{U}|-1}{\sum_{v \neq u}d(u,v)}

where :math:`d(u,v)` represents the distance between the two nodes.

If the graph is not fully connected, the value of this metric would always be equal to zero, so we have restricted
its computation to the strongly connected component that user :math:`u` belongs to.

**References:**
 * M.E.J. Newman. Networks: an introduction (2010).
 * L.C. Freeman. Centrality in Networks: I. Conceptual clarification, Social Networks 1, 1979, pp.215-239.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Closeness:
      type: vertex

Coreness
~~~~~~~~
A :math:`k`-core in a network is the maximal subgraph of the network such that each vertex in the subgraph has, at least,
degree equal to :math:`k`. The coreness of a node is equal to :math:`k` if the node belongs to the :math:`k`-core, but
not to the :math:`k+1`-core.

The coreness of a node measures the maximum k-core it belongs to. A k-core is a subgraph of the network

**References:**
 * Seidman, S.B. Network structure and minimum degree. Social Networks 5(3), pp. 269-287 (1983).
 * Batagelj, V., Zaversnik. An O(m) Algorithm for Cores Decomposition of networks. arXiv (2003).

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we want to use for computing the degrees. In undirected neighbors, the value of the degree does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: for using the in-degree.
    * OUT: for using the out-degree.
    * UND: for using the degree :math:`\mbox{degree}(u) = \mbox{in-degree}(u) + \mbox{out-degree}(u)`
    * MUTUAL: for using the number of reciprocated links.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the degree is:

.. code:: yaml

    Coreness:
      type: vertex
      params:
        orientation:
            type: orientation
            values: [IN/OUT/UND/MUTUAL]

Degree
~~~~~~
This metric measures the number of adjacent links of the studied node. It is computed as:

.. math::

    \mbox{degree}(u) = |\Gamma(u)| = |\{(u,v) \in E\}|

where :math:`\Gamma(u)` is the neighborhood of the node. Note that, in multigraphs, a user can appear
several times in :math:`\Gamma(u)`.

Related to this measure, we allow two additional measures: the **inverse degree**, which measures 
the multiplicative inverse of the degree:

.. math::

    \mbox{inv-degree}(u) = \frac{1.0}{\mbox{degree}(u)}

and the degree of the node in the complementary graph (what we call the **complementary degree**):
    
.. math::

    \mbox{compl-degree}(u) = |\mathcal{U}| - \mbox{degree}(u)    

Note that we cannot compute this metric (and its inverse) in multigraphs, since the complementary of such graph
is not properly defined.

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we want to use for computing the degree. In undirected neighbors, the value of the degree does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: for computing the in-degree.
    * OUT: for computing the out-degree.
    * UND: :math:`\mbox{degree}(u) = \mbox{in-degree}(u) + \mbox{out-degree}(u)`
    * MUTUAL: only counts reciprocated links.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the degree is:

.. code:: yaml

    Degree:
      type: vertex
      params:
        orientation:
            type: orientation
            values: [IN/OUT/UND/MUTUAL]

for the inverse degree is:

.. code:: yaml

    Inverse degree:
      type: vertex
      params:
        orientation:
            type: orientation
            values: [IN/OUT/UND/MUTUAL]

for the complementary degree:

.. code:: yaml

    Complementary degree:
      type: vertex
      params:
        orientation:
            type: orientation
            values: [IN/OUT/UND/MUTUAL]

and for its inverse

.. code:: yaml

    Complementary inverse degree:
      type: vertex
      params:
        orientation:
            type: orientation
            values: [IN/OUT/UND/MUTUAL]

.. _eccentricity_label: 

Eccentricity
~~~~~~~~~~~~
The eccentricity of a node measures the maximum (finite) distance between the node and any other vertex in the network.

.. math::

    \mbox{Eccentricity}(u) = \max_{v: d(u,v)<\infty} d(u,v)

where :math:`d(u,v)` represents the distance between the two nodes.

**Reference:** P. Dankelmann, W. Goddard, C. Swart. The average eccentricity of a graph and its subgraphs. Utilitas Mathematica 65(May), pp. 41-51 (2004)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Eccentricity:
      type: vertex



Eigenvector centrality
~~~~~~~~~~~~~~~~~~~~~~
The eigenvector centrality measures the importance of a node based on the importance of its neighbors. The value of the eigenvector centrality 
is the :math:`u`-th coordinate of the vector :math:`x`, where :math:`x` is the solution to the equation :math:`Ax=\lambda x`, where :math:`\lambda`
is the largest eigenvalue o the adjacency matrix :math:`A`. Or, in other words, the eigenvector centrality of a node is the corresponding coordinate
of the eigenvector associated to the largest eigenvalue of the adjacency matrix.

**Reference:** Bonacich, P.F. Power and centrality: A family of measures. American Journal of Sociology 92 (5), pp. 1170-1182 (1987)

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we use for determining the adjacency matrix. In undirected neighbors, the adjacency matrix does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: :math:`A_uv = w(v,u)`
    * OUT: :math:`A_uv = w(u,v)`
    * UND: :math:`A_uv = w(u,v) + w(v,u)`
    * MUTUAL: :math:`A_uv = w(u,v) + w(v,u)`, but only if :math:`w(u,v)\cdot w(v,u) > 0`

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Eigenvector:
      type: vertex
      params:
        orientation:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]


Harmonic centrality
~~~~~~~~~~~~~~~~~~~

The harmonic centrality can be studied as an alternative definition of `Closeness`_. This metric defines the centrality
as the harmonic mean of the distances between the node and the rest of the users in the network. It allows infinite 
distances (as they sum up just 0).

.. math::

    \mbox{Harmonic}(u) = \frac{1}{|\mathcal{U}|-1}\sum_{v \neq u}\frac{1}{d(u,v)}

where :math:`d(u,v)` represents the distance between the two nodes.

If the graph is not fully connected, the value of this metric would always be equal to zero, so we have restricted
its computation to the strongly connected component that user :math:`u` belongs to.

**References:**
 * M.E.J. Newman. Networks: an introduction (2010).
 * L.C. Freeman. Centrality in Networks: I. Conceptual clarification, Social Networks 1, 1979, pp.215-239.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Harmonic:
      type: vertex

HITS
~~~~

The Hyperlink-Induced Topic Search (HITS) algorithm determines the importance of the nodes in the network according
to a random walk that. This random walk first traverses an outgoing link, and, afterwards, an incoming one. 
The algorithm gives two scores to each node: an authority score (indicating the value of the
node, according to those who point to him), and a hub score (indicating the value of the node according to the pointed
nodes). A good hub is a node which points to good authorities, and a good authority is a node which is pointed by good
hubs.

The scores are computed, iteratively, as:

.. math::

    \mbox{auth}(u) = \sum_{v \in \Gamma_{out}(u)} \mbox{hub}(v)

.. math::
    
    \mbox{hub}(u) = \sum_{v \in \Gamma_{in}(v)} \mbox{auth}(v)

where :math:`\Gamma_{in}(v),\Gamma_{out}(v)` are, respectively, the set of incoming and outgoing neighbors of :math:`v`.
After each iteration, the scores are normalized.

**Reference:** J.M. Kleinberg. Authoritative sources in a hyperlink environment. Journal of the ACM 46(5), PP. 604-632 (1999).

Parameters
^^^^^^^^^^

* *mode:* indicates whether we want to compute the authority scores (true) or the hub scores (false)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    HITS:
      type: vertex
      params:
        mode:
          type: boolean
          values: [true, false]


Katz centrality
~~~~~~~~~~~~~~~

The Katz centrality of a node estimates the importance of the nodes according to the paths between the node 
and the rest of vertices in the network. It considers all the possible paths involving the user (not only
the shortest ones, but all the possible ones). Scores are computed as:

.. math::

    Katz(u) = \sum_{k=1}^\infty \sum_{v \in \mathcal{U}} \alpha^k A_{uv}^k


where :math:`A` is the adjacency matrix of the graph. In Katz centrality, the importance of the paths is weighted,
so lower distance paths are more important than those at large distances. Parameter :math:\alpha is used to 
determine how important long paths are.

**Reference:** Katz, L. A new status index derived from sociometric analysis. Psychometrika 18(1), pp. 33-43 (1953).

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we use for determining the adjacency matrix. In undirected neighbors, the adjacency matrix does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: :math:`A_{uv} = w(v,u)`
    * OUT: :math:`A_{uv} = w(u,v)`
    * UND: :math:`A_{uv} = w(u,v) + w(v,u)`
    * MUTUAL: :math:`A_{uv} = w(u,v) + w(v,u)`, but only if :math:`w(u,v)\cdot w(v,u) > 0`
* *alpha:* a dump factor for giving less importance to long paths. :math:`\alpha \in (0,1)`

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Katz:
      type: vertex
      params:
        orientation:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        alpha:
          type: double
          range:
          - start: 0.1
            end: 0.99
            step: 0.1

Local clustering coefficient
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The local clustering coefficient measures, the proportion of neighbors of the user who are connected. 

.. math::
    
    CC(u) = \frac{\{(v,w) \in E | v \neq w \wedge v \in \Gamma(u) \wedge w \in \Gamma(u)\}}{|\Gamma(u)|(|\Gamma(u)|-1)}


We also allow computing this metric in the complementary graph (what we call **complementary local clustering coefficient**)

**Reference:** D.J. Watts, S.H. Strogatz. Collective dynamics of 'small-world' networks. Nature 393(6684), pp. 440-442 (1998).


Parameters
^^^^^^^^^^

* *vSel*: selection of the orientation for selecting the first neighbor of the user. This allows the following values:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the incoming neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.
* *wSel*: selection of the orientation for selecting the second neighbor of the user. This allows the following values:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the incoming neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

For the original local clustering coefficient metric, the configuration file is:

.. code:: yaml

    Local clustering coefficient:
      type: vertex
      params:
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        wSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]


whereas for the value in the complementary network, it is:

.. code:: yaml

    Complementary local clustering coefficient:
      type: vertex
      params:
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        wSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

PageRank
~~~~~~~~

PageRank is a random walk algorithm designed to estimate the importance of the nodes in a network, according to its graph structure.
The value for a node represents the probability that a random walker who traverses the network randomly choosing the links to follow
travels through the node.

The importance of the node depends on three factors: a) the number of incoming nodes, b) the importance of those nodes and c) the number
of outgoing edges of these nodes. As the node receives more and more attention from other nodes in the network, the importance of the node
increases. For each of these incoming edges, the nodes receive an importance score proportional to the importance of the origin. And, finally,
the transferred importance is decreased proportionally to the number of outgoing edges of that node.

The PageRank of a node is defined, recursively, as:

.. math::
    
    PageRank(u) = \frac{r}{|\mathcal{U}|} + (1-r) \sum_{w \in \Gamma_{in}(u)} \frac{PageRank(v)}{|\Gamma_{out}(v)|} + \frac{1-r}{|\mathcal{U}|} \sum_{w : |\Gamma_{out}(w)|=0} PageRank(w)

where :math:r represents the probability that the random walker chooses to teleport to any node in the network.

Again, we also provide tools to compute the **complementary PageRank**, i.e. the PageRank value of the nodes in the complementary network.

**Reference:** S. Brin, L. Page. The anatomy of a large-scale hypertextual web search engine. 7th International Conference on World Wide Web (WWW 1998), Brisbane, Australia, pp. 107-117 (1998).

Parameters
^^^^^^^^^^

* *r:* the teleport probability. :math:`r \in (0,1)`

Configuration file
^^^^^^^^^^^^^^^^^^

The Yaml file for the original PageRank might be as follows:

.. code:: yaml

    PageRank:
      type: vertex
      params:
        r:
          type: double
          range:
          - start: 0.1
            end: 0.99
            step: 0.1

whereas the file for the complementary variant is:

.. code:: yaml

    Complementary PageRank:
      type: vertex
      params:
        r:
          type: double
          range:
          - start: 0.1
            end: 0.99
            step: 0.1


Reciprocity rate
~~~~~~~~~~~~~~~~

This metric measures the proportion of the edges involving the node which are reciprocal. This measure is only useful for 
directed networks (it has value equal to 1 for all nodes in undirected ones).

Parameters
^^^^^^^^^^

* *orientation*: selection of the set of edges to consider:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the incoming neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time. In this case, the metric is always equal to 1.

Configuration file
^^^^^^^^^^^^^^^^^^

The Yaml file for the original PageRank might be as follows:

.. code:: yaml

    Reciprocity rate:
      type: vertex
      params:
        orientation:
          values: [IN/OUT/UND]


Length
~~~~~~

This metric measures the length of a node, i.e. the sum of the weights of the links between this node and its neighbors:

.. math::

    \mbox{Length}(u) = \sum_{v\in\Gamma(u)} w(u,v)

where :math:\Gamma(u) represents the neighborhood of user :math:u and :math:w(u,v) is the weight of the corresponding link between :math:u and :math:v.

Parameters
^^^^^^^^^^

* *orientation*: selection of the set of edges to consider:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the incoming neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The Yaml file for the original PageRank might be as follows:

.. code:: yaml

    Length:
      type: vertex
      params:
        orientation:
          values: [IN/OUT/UND/MUTUAL]