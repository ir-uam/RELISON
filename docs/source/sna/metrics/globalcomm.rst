Global community metrics
============================

Given a community partition, it is possible to compute several metrics for the complete network considering such partition. RELISON integrates the following metrics:

* `Destiny community size`_
* `Degree Gini complement`_
* `Edge Gini complement`_
* `Modularity`_
* `Modularity complement`_
* `Number of communities`_
* `Size Gini complement`_
* `Weak ties`_

Destiny community size
~~~~~~~~~~~~~~~~~~~~~~
This metric computes the average size of the destination communities of the links connecting two different communities.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Destiny community size:
      type: global community


Degree Gini complement
~~~~~~~~~~~~~~~~~~~~~~
Estimates how balanced the degree distribution for the different communities is:

.. math::

	 \mbox{DegreeGiniCompl}(\mathcal{G|\mathcal{C}}) = 1 - \frac{1}{|\mathcal{C}|-1} \sum_{i = 1}^{|\mathcal{C}|} (2i - |\mathcal{C}| - 1) \frac{\mbox{Degree}(c_i)}{\sum_{c'} \mbox{Degree}(c')}


where :math:`c_i` is i-th community with smaller degree. We differentiate two variants:

* **Inter degree Gini complement**: it does not consider links from a community to itself.
* **Complete degree Gini complement**: it does consider links from a community to itself.

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we want to use for computing the degrees of the communities. In undirected neighbors, the value of the degree does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: for using the in-degree.
    * OUT: for using the out-degree.
    * UND: for using the degree :math:`\mbox{degree}(c) = \mbox{in-degree}(c) + \mbox{out-degree}(c)`
    * MUTUAL: for using the number of reciprocated links.

Configuration file
^^^^^^^^^^^^^^^^^^
The configuration for the metric variant which does not consider the links between node in the same community is:

.. code:: yaml

    Inter-community degree Gini:
      type: global community
      params:
        orientation:
          type: orientation
          values: [IN,OUT,UND,MUTUAL]

while the version considering links between the nodes in the same community is:

.. code:: yaml

    Complete community degree Gini:
      type: global community
      params:
        orientation:
          type: orientation
          values: [IN,OUT,UND,MUTUAL]

Edge Gini complement
~~~~~~~~~~~~~~~~~~~~
The community edge Gini complement computes how balanced the number of links between different pairs of commmunities is. The metric formulation is:

.. math::

    \mbox{CommEdgeGini}(\mathcal{G}|\mathcal{C}) = 1 - \frac{1}{|\mathcal{C}|(|\mathcal{C}|-1)} \sum_{i = 1}^{|\mathcal{C}|(|\mathcal{C}|-1)} (2i - |\mathcal{C}|(|\mathcal{C}|-1) - 1) p((c_1,c_2)_i|\mathcal{G},\mathcal{C})

where :math:`(c_1,c_2)_i` is the i-th pair of communities with the smaller number of links between them and:

.. math::

    p((c_1,c_2)|\mathcal{G},\mathcal{C}) = \frac{|\{(u,v) \in E | c(u) = c_1 \wedge c(v) = c_2\}|}{|\{(u,v) \in E | c(u) \neq c(v)\}|}


where :math:`(u,v)_i` is the i-th pair of users with an smaller number of links.

We differentiate three variants:

* **Inter edge Gini complement:** This metric does not consider links nodes inside the same community. It takes the previous equation.
* **Semi-complete edge Gini complement:** This metric stores links between nodes in the same community as a different category for the Gini index.
* **Complete edge Gini complement:** This metric considers links inside communities. In the previous equation, we would just need to substitute :math:`|\mathcal{C}|(|\mathcal{C}|-1)` by :math:`|\mathcal{C}|^2` when it appears, and :math:`|\{(u,v) \in E | c(u) \neq c(v)\}|` by :math:`E`.

**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.
 * J. Sanz-Cruzado, P. Castells. Enhancing Structural Diversity in Social Networks by Recommending Weak Ties. 12th ACM Conference on Recommender Systems (RecSys 2018), pp. 233-241.

Parameters
^^^^^^^^^^

For the semi-complete and complete versions, we have a parameter:

* *selfloops*: true if we want to allow selfloops between the nodes, false otherwise.

Configuration file
^^^^^^^^^^^^^^^^^^

The configuration for the inter edge Gini complement is:

.. code:: yaml

    Inter-community edge Gini complement:
      type: global community

For the semi-complete variant is:

.. code:: yaml
  
    Semi-complete community edge Gini complement:
      type: global community
      params:
        selfloops:
          type: boolean
          values: [true, false]

and, finally, the version considering links inside communities is:

.. code:: yaml
  
    Complete community edge Gini complement:
      type: global community
      params:
        selfloops:
          type: boolean
          values: [true, false]

Modularity
~~~~~~~~~~
The modularity of a network compares the number of links inside communities to the ones we would have in a random graph keeping the degree distribution.  It is correlated to the number of links inside communities. Its formulation is:

.. math:: 

    \mbox{mod}(\mathcal{G}|\mathcal{C}) = \frac{\sum_{u,v} \left(A_{uv} - \frac{|\Gamma(u)||\Gamma(v)|}{|E|} 1_{c(u) = c(v)} \right)}{|E| - \sum_{u,v} \frac{\Gamma(u)||\Gamma(v)|}{|E|} 1_{c(u) = c(v)}}

where :math:`1_x` is equal to 1 when condition :math:`x` is true, 0 otherwise.

**Reference**: M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Modularity:
      type: global community          

Modularity complement
~~~~~~~~~~~~~~~~~~~~~
This metric is computed as the complement of the modularity, so it measures the number of links between communities in the network.  Its formulation is:

.. math:: 

    \mbox{MC}(\mathcal{G}|\mathcal{C}) = \frac{1 - \mbox{mod}(\mathcal{G}|\mathcal{C})}{2}

**References:**
 * J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 9th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018). The Web Conference Companion, pp. 1347–1351. 
 * J. Sanz-Cruzado, P. Castells. Beyond Accuracy in Link Prediction. BIAS 2020: Bias and Social Aspects in Search and Recommendation, pp 79-94.
 * J. Sanz-Cruzado, P. Castells. Enhancing Structural Diversity in Social Networks by Recommending Weak Ties. 12th ACM Conference on Recommender Systems (RecSys 2018), pp. 233-241.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Modularity complement:
      type: global community          

Number of communities
~~~~~~~~~~~~~~~~~~~~~
As its name indicates, this metric just takes the number of communities in the partition.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Num. communities:
      type: global community

Size Gini complement
~~~~~~~~~~~~~~~~~~~~~~
This metric indicates how balanced the distribution of the community sizes is.

.. math::

   \mbox{SizeGiniCompl}(\mathcal{G|\mathcal{C}}) = 1 - \frac{1}{|\mathcal{C}|-1} \sum_{i = 1}^{|\mathcal{C}|} (2i - |\mathcal{C}| - 1) \frac{\mbox{Size}(c_i)}{|\mathcal{U}|}


where :math:`c_i` is i-th community with smaller size.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Complete size Gini:
      type: global community

Weak ties
~~~~~~~~~~~~~~~~~~~~~~
This metric counts the number of links between different communities.

.. math::

    \mbox{WT}(\mathcal{G}|\mathcal{C}) = |\{(u,v) \in E | c(u) \neq c(v)\}|

**Reference:** E. Ferrara, P. de Meo, G. Fiumara, A. Provetti. On Facebook, most ties are weak. Communications of the ACM 57(11), pp. 78-84 (2012)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Weak ties:
      type: global community