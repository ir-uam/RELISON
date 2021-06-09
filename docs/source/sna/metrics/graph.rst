Graph metrics
==============

RELISON integrates the following graph metrics.


* `Average shortest path length`_
* `Average reciprocal shortest path length`_
* `Clustering coefficient`_
* `Clustering coefficient complement`_
* `Degree assortativity`_
* `Degree Gini complement`_
* `Degree Pearson correlation`_
* `Density`_
* `Diameter`_
* `Edge Gini complement`_
* `Infinite distances`_
* `Radius`_
* `Reciprocal average eccentricity`_
* `Reciprocal diameter`_
* `Reciprocity rate`_


Average shortest path length
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
As its name indicates, this metric just computes the average shortest path length of a graph. In order to deal with infinite distance edges, we propose two options:

* **Option 1:** We only average over those pairs of users at finite distance.
* **Option 2:** We obtain the average shortest path length for each strongly connected component of the network, and then, we average the result for the different components.

Parameters
^^^^^^^^^^

* *mode:* "Non infinite distances" for option 1, "Components" for option 2.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    ASL:
      type: graph
      params:
        mode:
          type: string
          values: ["Non infinite distances", "Components"]

Average reciprocal shortest path length
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric computes the harmonic mean of the distances between pairs of users:

.. math::

	\mbox{ARSL} = \frac{1}{|\mathcal{U}|(|\mathcal{U}|-1)} \sum_{u,v} \frac{1}{\delta(u,v)}

where :math:`\delta(u,v)` represents the distance between a pair of users.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    ARSL:
      type: graph
      params:
        mode:
          type: string
          values: ["Non infinite distances", "Components"]

Clustering coefficient
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The global clustering coefficient of a social network graph measures the proportion of closed triads in the network:

.. math::

	\mbox{CC}(\mathcal{G}) = \frac{|\{(u,v,w) | u \neq w \wedge (u,v),(v,w),(u,w) \in E\}|}{|\{(u,v,w) | u \neq w \wedge (u,v),(v,w) \in E\}|}


Parameters
^^^^^^^^^^

* *uSel*: selection of the orientation for the first of the two links to consider in the triad (the (u,v) one). 
    * IN: we take the (v,u) link.
    * OUT: we take the (u,v) link.
    * UND: we take either the (u,v) or the (v,u) link (the one that exists)
    * MUTUAL: we only consider that the pair (u,v) exists if both (u,v) and (v,u) links exist.
* *vSel*: selection of the orientation for the second of the two links in the triad (the (v,w) one).
    * IN: we take the (v,w) link
    * OUT: we take the (w,v) link.
    * UND: we take either the (w,v) or the (v,w) link (the one that exists)
    * MUTUAL: we only consider that the pair (u,v) exists if both (u,v) and (v,u) links exist.

The natural clustering coefficient can be chosen by taking uSel = OUT and vSel = IN.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Clustering coefficient:
      type: graph
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Clustering coefficient complement
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric is the complement of the clustering coefficient, as it takes measures the proportion of the open triads in the network.

.. math::

	\mbox{CCC}(\mathcal{G}) = \frac{|\{(u,v,w) | u \neq w \wedge (u,w) \notin E \wedge (u,v),(v,w) \in E\}|}{|\{(u,v,w) | u \neq w \wedge (u,v),(v,w) \in E\}|}


Parameters
^^^^^^^^^^

* *uSel*: selection of the orientation for the first of the two links to consider in the triad (the (u,v) one). 
    * IN: we take the (v,u) link.
    * OUT: we take the (u,v) link.
    * UND: we take either the (u,v) or the (v,u) link (the one that exists)
    * MUTUAL: we only consider that the pair (u,v) exists if both (u,v) and (v,u) links exist.
* *vSel*: selection of the orientation for the second of the two links in the triad (the (v,w) one).
    * IN: we take the (v,w) link
    * OUT: we take the (w,v) link.
    * UND: we take either the (w,v) or the (v,w) link (the one that exists)
    * MUTUAL: we only consider that the pair (u,v) exists if both (u,v) and (v,u) links exist.

The natural clustering coefficient can be chosen by taking uSel = OUT and vSel = IN.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Clustering coefficient complement:
      type: graph
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Degree assortativity
~~~~~~~~~~~~~~~~~~~~
The degree assortativity measures to what extent users create links towards similar users in terms of their degree (i.e. if users with small degree create links towards
users with small degrees and users with large degree create links towards users with large degree) or not.

In undirected networks, it is computed as:

.. math::

	\mbox{Assortativity}(\mathcal{G}) = \frac{2\cdot|E|\cdot \sum_(u,v) |\Gamma(u)||\Gamma(v)| - \left(\sum_u |\Gamma(u)|^2\right)^2}{4m \sum_{u} |\Gamma(u)|^3 - \left(\sum_u |\Gamma(u)|^2\right)^2}


**Reference** :  M.E.J. Newman. Mixing patterns in networks. Physical Review E, 67 026126 (2003)

Parameters
^^^^^^^^^^

* *orientation*: selection for the degree to use.
    * IN: we take the incoming neighbors of the users.
    * OUT: we take the outgoing neighbors of the users.
    * UND: we take the incoming and outgoing neighbors of the users.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Degree assortativity:
      type: graph
      params:
        orientation:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]


Degree Pearson correlation
~~~~~~~~~~~~~~~~~~~~~~~~~~
The degree assortativity measures the Pearson correlation of the degrees between the origin and destination endpoints of the nodes.

.. math::

	\mbox{Pearson}(\mathcal{G}) = \frac{\sum_{(u,v) \in E} |\Gamma(u)||\Gamma(v)|}{\sqrt{\sum_{u} |\Gamma(u)|^2 \cdot \sum_{v} |\Gamma(v)|^2}}

Parameters
^^^^^^^^^^

* *uSel*: selection of the orientation for the neighborhood of the starting node of the edges. This allows the following values:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the outgoing neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.
* *vSel*: selection of the orientation for the neighborhood of the ending node of the edges. This allows the following values:
    * IN: we take the incoming neighbors of the user.
    * OUT: we take the outgoing neighbors of the user.
    * UND: we take the incoming and outgoing neighbors of the user.
    * MUTUAL: we take those neighbors who are both incoming and outgoing at the same time.


Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Degree Pearson:
      type: graph
      params:
        uSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]
        vSel:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Degree Gini complement
~~~~~~~~~~~~~~~~~~~~~~
The degree Gini complement indicates how balanced the degree distribution of the network is. Values close to one indicate that the degree distribution is flat, whereas values close to 0 show that a few users concentrate all the links in the network.

.. math::

	\mbox{DegreeGiniCompl}(\mathcal{G}) = 1 - \frac{1}{|\mathcal{U}|-1} \sum_{i = 1}^{|\mathcal{U}|} (2i - |\mathcal{U}| - 1) \frac{|\Gamma(u_i)|}{\sum_v |\Gamma(v)|}

where :math:`\Gamma(u)` is the neighborhood of user :math:`u` and :math:`u_i` is the i-th node in the network with the smaller degree. 

Parameters
^^^^^^^^^^

* *orientation*: selects the type of degree we use (only affects directed networks).
    * IN: we take the in-degree of the users.
    * OUT: we take the out-degree of the users.
    * UND: we take the undirected degree of the users (in-degree + out-degree)
    * MUTUAL: we take as the degree the number of mutual links.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Degree Gini Complement:
      type: graph
      params:
        orientation:
          type: orientation
          values: [IN/OUT/UND/MUTUAL]

Density
~~~~~~~
The density of a network measures the proportion of the possible number of edges between nodes which exist in the network. This metric does not consider selfloops.

.. math::

	\mbox{Density}(\mathcal{G}) = \frac{|E|}{|\mathcal{U}|(|\mathcal{U}|-1)}

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Density:
      type: graph

Diameter
~~~~~~~~
The diameter of a network measures the maximum (finite) distance between two users in the network.

.. math::

	\mbox{Diameter}(\mathcal{G}) = \max_{(u,v) : \delta(u,v) < \infty} \delta(u,v)

It is equivalent to the maximum eccentricity of the network.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the original method is the following

.. code:: yaml

    Diameter:
      type: graph


Edge Gini complement
~~~~~~~~~~~~~~~~~~~~
The edge Gini complement computes how balanced the number of links between different pairs of user is. This metric has only sense over multigraphs, where multiple links between users are allowed. The metric formulation is similar to:

.. math::

	\mbox{EdgeGini}(\mathcal{G}) = 1 - \frac{1}{|\mathcal{U}|(|\mathcal{U}|-1)} \sum_{i = 1}^{|\mathcal{U}|(|\mathcal{U}|-1)} (2i - |\mathcal{U}|(|\mathcal{U}|-1) - 1) \frac{|\{(u,v)_i \in E\}|}{|E|}

where :math:`(u,v)_i` is the i-th pair of users with an smaller number of links.

We differentiate three variants:

* **Inter edge Gini complement:** This metric does not consider the selfloops between the users. It takes the previous equation (considering that :math:`E` does not have selfloops).
* **Semi-complete edge Gini complement:** This metric stores selfloops as a different category for the Gini index, i.e. we add an element to the sum, counting the total number of selfloops in the network.
* **Complete edge Gini complement:** This metric considers selfloops. In the previous equation, we would just need to substitute :math:`|\mathcal{U}|(|\mathcal{U}|-1)` by :math:`|\mathcal{U}|^2` when it appears.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration for the inter edge Gini complement is:

.. code:: yaml

    Inter edge Gini complement:
      type: graph

For the semi-complete variant is:

.. code:: yaml
	
	Semi-complete edge Gini complement:
	  type: graph

and, finally, the version considering selfloops is:

.. code:: yaml
	
	Complete edge Gini complement:
	  type: graph

Infinite distances
~~~~~~~~~~~~~~~~~~
This metric measures the number of node pairs which do not have a path between the first and the second in the network.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Infinite distances:
      type: graph


Radius
~~~~~~
If we consider the eccentricity values of all the users (i.e. the maximum finite distance between a user and the rest of the network), the radius represents its minimum value. 

.. math::

	\mbox{Radius}(\mathcal{G}) = \min_{u}\left(\max_{v : \delta(u,v) < \infty} \delta(u,v)\right)

where :math:`\delta(u,v)` represents the distance between two users.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Radius:
      type: graph


Reciprocal average eccentricity
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This metric computes the inverse value of the average eccentricity of the network.

If we consider the eccentricity values of all the users (i.e. the maximum finite distance between a user and the rest of the network), the radius represents its minimum value. 

.. math::

	\mbox{RAE}(\mathcal{G}) = \frac{|\mathcal{U}|}{\sum_{u} \mbox{Eccentricity}(u)}

**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Reciprocal average eccentricity:
      type: graph



Reciprocal diameter
~~~~~~~~~~~~~~~~~~~
This metric computes the inverse value of the diameter (see `Diameter`_), so, when distances are reduced among the users in the network, the value of this metric increases.

If we consider the eccentricity values of all the users (i.e. the maximum finite distance between a user and the rest of the network), the radius represents its minimum value. 

.. math::

	\mbox{RD}(\mathcal{G}) = \frac{1}{\mbox{Diameter}(\mathcal{G})}

where :math:`\delta(u,v)` represents the distance between two users.

**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Reciprocal diameter:
      type: graph

Reciprocity rate
~~~~~~~~~~~~~~~~~~~
This metric computes the proportion of the edges in the network which are reciprocal.

.. math::

	\mbox{Reciprocity}(\mathcal{G}) = \frac{|\{(u,v) \in E | (v,u) \in E\}|}{|E|}

where :math:`\delta(u,v)` represents the distance between two users.

**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Reciprocity:
      type: graph      