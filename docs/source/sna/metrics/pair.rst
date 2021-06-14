Pair metrics
==============

RELISON integrates a set of metrics which can be computed for any pair of users. All the metrics which can be applied to links are included here, but they can be computed for any possible set of users.

RELISON integrates the following edge metrics:

* `Betweenness`_
* `Clustering coefficient increment`_
* `Distance`_
* `Distance without link`_
* `Embeddedness`_
* `Extended neighbor overlap`_
* `Geodesics`_
* `Neighbor overlap`_
* `Reciprocity`_
* `Preferential attachment`_
* `Shrinking average shortest path length`_
* `Shrinking diameter`_
* `Weighted neighbor overlap`_
* `Weight`_


Betweenness
~~~~~~~~~~~
The betweenness centrality measures, for each pair of users, the number of shortest paths which pass through the edge joining the pair of users. If the edge does not exist, it returns 0.

.. math::

    \mbox{Betweenness}(u,v) = \sum_{(w,x)} \frac{\sigma_{wx}(u,v)}{\sigma_{wx}}

where :math:`\sigma_{wx}` is the number of shortest paths between :math:`w` and :math:`x`, and  :math:`\sigma_{wx}(u,v)` is the number of
shortest paths which pass through edge :math:`(u,v)`.

**Reference:** M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)

Parameters
^^^^^^^^^^

* :code:`normalize`: true if we want to normalize the coefficient, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Betweenness:
      type: edge
      params:
        normalize:
          type: boolean
          values: [true, false]

Clustering coefficient increment
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric computes how much the global clustering coefficient of the network (see **ADDLINK**) would increase if a link between the two users was added to the network. If the link already exists, this value is equal to zero. It just considers the option where we select distance-2 paths as triads.

**Reference:** J. Sanz-Cruzado. Contact recommendation in social networks: algorithmic models, diversity and network evolution. PhD thesis (2021).

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Clustering coefficient increment:
      type: pair


Distance
~~~~~~~~
The distance measures the minimum number of steps we have to take for travelling between two nodes. It is also known as the **shortest path length**.

In addition to this metric, we also compute the **efficiency** or **reciprocal shortest path length**, which computes the inverse of the distance between two users:

.. math::

	\mbox{RSL}(u,v) = \frac{1}{\delta(u,v)}

where :math:`\delta(u,v)` represents the distance between users :math:`u` and :math:`v`.


**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.

Configuration file
^^^^^^^^^^^^^^^^^^
For the original distance, this is computed as:

.. code:: yaml

    Distance:
      type: pair

whereas for the reciprocal distance:

.. code:: yaml

    Reciprocal shortest path length:
      type: pair

Distance without link
~~~~~~~~~~~~~~~~~~~~~
The distance measures the distance between two users in the network if we removed the edge between them. If the link does not exist, the distance between the users is the usual one.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Distance without link:
      type: pair

Embeddedness
~~~~~~~~~~~~

The embeddedness of pair of users in a network measures the proportion of the neighbors of the two nodes who are common to both of them. It indicates how redundant a link between the two nodes would be in the network. In case the link exists, it can be considered a measure of the strength of the link.

.. math::

    \mbox{Embeddedness}(u,v) = \frac{|\Gamma(u)\setminus\{v\} \cap \Gamma(v)\setminus\{u\}|}{|\Gamma(u)\setminus\{v\} \cup \Gamma(v)\setminus\{u\}|}

where :math:`\Gamma(u)` is the neighborhood of user :math:`u`.

In our framework, we can compute two related measures: the first one, the **weakness**, measures the opposite: the number of neighbors of the pair of users who are not common to both:

.. math::

    \mbox{Weakness}(u,v) = 1 - \mbox{Embeddedness}(u,v)

The second one is just the value of the metric in the complementary graph:

.. math::
    
    \mbox{Compl. Embeddedness}(u,v) = \frac{|\mathcal{U}| - |\Gamma(u) \cup \Gamma(v)|}{|\mathcal{U}| - |\Gamma(u) \cap \Gamma(v)|}


**References:**
 * D. Easley, J.M. Kleinberg. Networks, crowds and markets (2010).
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.

Parameters
^^^^^^^^^^
All the variants share the same two parameters:

* :code:`uSel`: selection of the orientation for the neighborhood of the starting node of the edge. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

* :code:`vSel`: selection of the orientation for the neighborhood of the ending node of the edge. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

The natural configuration for the embeddedness of a links takes :code:`uSel = OUT` and :code:`wSel = IN`.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration for the original embeddedness metric is:

.. code:: yaml

    Embeddedness:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

for the weakness one is:

.. code:: yaml

    Weakness:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

and for the metric in the complementary graph:

.. code:: yaml

    Complementary embeddedness:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]


Extended neighbor overlap
~~~~~~~~~~~~~~~~~~~~~~~~~
This metric counts the intersection of the users at distance two from the first user with the neighborhood of the second:

.. math::

    \mbox{ECN}(u,v) = \left|\left(\Gamma(u) \cup \bigcup_{w \in \Gamma(u)} \Gamma(w)\right) \cap \Gamma(v)\right|

Another version, we name **extended neighbor counted overlap**, instead of straightforwardly counting the number of common neighbors, they count the number of times they appear in the intersection:

.. math::

    \mbox{ECCN}(u,v) = |\Gamma(u) \cap \Gamma(v)| + \sum_{w \in \Gamma(u)} |\Gamma(w) \cap \Gamma(v)|

Parameters
^^^^^^^^^^
* :code:`origin`: true if we take the distance 2 neighborhood of the first user, false if we take the neighborhood of the second.
* :code:`uSel`: selection of the orientation for the neighborhood of the first node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.
* :code:`vSel`: selection of the orientation for the neighborhood of the second node of the pair. This allows the following values:
    
    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Extended neighbor overlap:
      type: pair
      params:
        origin:
          type: boolean
          values: [true/false]
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

while the configuration for the weighted variant is:

.. code:: yaml

    Extended neighbor counted overlap:
      type: pair
      params:
        origin:
          type: boolean
          values: [true/false]
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]



Geodesics
~~~~~~~~~
The geodesics metric measures the number of minimum distance paths between a pair of users. 

.. math::
    
    \mbox{Geodesics}(u,v) = |\{p \in \mbox{paths(u,v)} | \mbox{length}(p) = \delta(u,v)\}|

where :math:`\delta(u,v)` represents the distance between users :math:`u` and :math:`v`, :math:`\mbox{paths}(u,v)` is the set of all paths between them, and :math:`\mbox{length}(p)` is the length of the path :math:`p`.


Configuration file
^^^^^^^^^^^^^^^^^^
For the original distance, this is computed as:

.. code:: yaml

    Geodesics:
      type: pair

whereas for the reciprocal distance:

.. code:: yaml

    Reciprocal shortest path length:
      type: pair

Neighbor overlap
~~~~~~~~~~~~~~~~
This metric just counts the number of common neighbors between two users in the network.

.. math::

    \mbox{CN}(u,v) = |\Gamma(u)\cap\Gamma(v)|

where :math:`\Gamma(u)` is the neighborhood of user :math:`u`.

We can also compute what we call the **complementary neighbor overlap** metric, or, in other words, the value of this metric in the complementary graph. Its equation is the following one:

.. math::

    \mbox{CN}(u,v) = |\mathcal{U}| - |\Gamma(u)\cup\Gamma(v)|

Parameters
^^^^^^^^^^

* :code:`uSel`: selection of the orientation for the neighborhood of the first node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.
* :code:`vSel`: selection of the orientation for the neighborhood of the second node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Neighbor overlap:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

while the configuration for the value of the metric in the complementary graph is:

.. code:: yaml

    Complementary common neighbors:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Preferential attachment
~~~~~~~~~~~~~~~~~~~~~~~
The preferential attachment measures to which extent a link between two pairs of users might appear under the preferential attachment model.

.. math::
    
    \mbox{PA}(u,v) = |\Gamma(u)||\Gamma(v)|

where :math:`\Gamma(u)` represents the neighborhood of the user.

Parameters
^^^^^^^^^^

* :code:`uSel`: selection of the orientation for the neighborhood of the first node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.
* :code:`vSel`: selection of the orientation for the neighborhood of the second node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Preferential attachment:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Reciprocity
~~~~~~~~~~~
Given a pair of users :math:`(u,v)`, this metric just finds whether the link :math:`(v,u)` appears in the network. If it does, it returns value 1.0. Value 0.0 is returned otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Reciprocity:
      type: pair

Shrinking average shortest path length
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The shrinking average shortest path length metric computes the reduction of the average distance between two users in the social network if the pair was added to it. In case the link exists, it returns 0.0.

We have another variation, we name **shrinking neighbors shortest path length** which restricts this calculation to the neighbors of the involved nodes.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Shrinking ASL:
      type: pair

whereas for the limited version:

.. code:: yaml

    Shrinking neighbors ASL:
      type: pair


Shrinking diameter
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The shrinking average shortest path length metric computes the reduction of the diameter of the network if the pair was added to it. In case the link exists, it returns 0.0.

We have another variation, we name **shrinking neighbors diameters** which restricts this calculation to the neighbors of the involved nodes.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Shrinking diameter:
      type: pair

whereas for the limited version

.. code:: yaml

    Shrinking neighbors diameter:
      type: pair

Weakness
~~~~~~~~
See `Embeddedness`_

Weight
~~~~~~
If it is available, it just measures the weight of an edge in the graph.
In unweighted networks, all edges have weight equal to 1. If the weight does not exist, it takes value equal to 0.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Weight:
      type: edge


Weighted neighbor overlap
~~~~~~~~~~~~~~~~~~~~~~~~~
This metric just sums the weights of the common neighbors between two users in the network.

.. math::

    \mbox{Weighted-CN}(u,v) = \sum_{x \in |\Gamma(u) \cap \Gamma(v)} w(v,x)

where :math:`\Gamma(u)` is the neighborhood of user :math:`u`, and :math:`w(v,x)` is the weight of the edge between :math:`w` and :math:`x`.

We have another version, **logarithmic weighted neighbor overlap** which takes the logarithm of the weight instead:

.. math::

    \mbox{Weighted-CN}(u,v) = \sum_{x \in |\Gamma(u) \cap \Gamma(v)} (1 + \log(w(v,x)))


Parameters
^^^^^^^^^^

* :code:`uSel`: selection of the orientation for the neighborhood of the first node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.
* :code:`vSel`: selection of the orientation for the neighborhood of the second node of the pair. This allows the following values:

    * :code:`IN`: we take the incoming neighbors of the user.
    * :code:`OUT`: we take the incoming neighbors of the user.
    * :code:`UND`: we take the incoming and outgoing neighbors of the user.
    * :code:`MUTUAL`: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Weighted neighbor overlap:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

while the configuration for the value of the metric in the complementary graph is:

.. code:: yaml

    Log weighted neighbor overlap:
      type: pair
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
