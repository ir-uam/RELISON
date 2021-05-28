Individual community metrics
============================

Given a community partition, it is possible to compute several metrics considering the different clusters of users. Individual community metrics evaluate the structural properties for each community on its own. SoNALiRe integrates the following metrics:

* `Degree`_
* `Size`_

Degree
~~~~~~
This metric measures the number of external adjacent links to a given community. It is also known as the cut of the community when the out-degree is used.

.. math::

  \mbox{degree}(c) = |\{(u,v) \in E : u \in c \wedge v \notin c\}|

where :math:`c` is the studied community.

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we want to use for computing the degrees of the communities. In undirected neighbors, the value of the degree does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: for using the in-degree.
    * OUT: for using the out-degree.
    * UND: for using the degree :math:`\mbox{degree}(c) = \mbox{in-degree}(c) + \mbox{out-degree}(c)`
    * MUTUAL: for using the number of reciprocated links.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Degree:
      type: indiv. community
      params:
        orientation:
          type: orientation
          values: [IN,OUT,UND,MUTUAL]

Size
~~~~
This metric measures the number of nodes in a community
.. math::

  \mbox{degree}(c) = |c|

where :math:`c` is the studied community.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Size:
      type: indiv. community

Volume
~~~~~~
This metric measures the sum of the degrees of the nodes in a community.
.. math::

  \mbox{vol}(c) = \sum_{u\in c} |\Gamma(u)|

where :math:`c` is the studied community.

Parameters
^^^^^^^^^^

* *orientation:* selection of the neighborhood of the node we want to use for computing the degrees of the communities. In undirected neighbors, the value of the degree does not change when this parameter does. This is only useful in directed networks. This allows the following parameters:
    * IN: for using the in-degree.
    * OUT: for using the out-degree.
    * UND: for using the degree :math:`\mbox{degree}(c) = \mbox{in-degree}(c) + \mbox{out-degree}(c)`
    * MUTUAL: for using the number of reciprocated links.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Volume:
      type: indiv. community
      params:
        orientation:
          type: orientation
          values: [IN,OUT,UND,MUTUAL]