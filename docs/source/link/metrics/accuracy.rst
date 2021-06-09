Accuracy metrics
=================
In order to determine how good people recommendation algorithms are, it is common to evaluate them using ranking metrics. This framework allows the use of the following ones (although more of them can be defined, using the metric interfaces provided by the RankSys framework (https://ranksys.github.io)):

* `Average precision`_
* `nDCG`_
* `Precision`_
* `Recall`_

Average precision
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This ranking metric gives more importance to the correctly recommended users at the top of the ranking. It just represents the area under the precision-recall curve: a plot of the precision of a recomendation as a function of the recall. It is defined as:

.. math::

  \mbox{AP}(u) = \frac{\sum_{k=1}^{|R_u|} P@k(u) \cdot 1_{\mbox{relevant}(u)}(v_k)}{|\mbox{relevant}(u)}

where :math:`|R_u|` represents the length of the recommendation ranking for user :math:`u`, :math:`P@k` represents the precision at cutoff :math:`k` (see `Precision`_), :math:`\mbox{relevant}(u)` is the set of links which have :math:`u` as origin in the evaluation set, and :math:`v_k` is the :math:`k`-th recommended user in the ranking.

When it is averaged over the different users in the network, this metric receives the **mean average precision (MAP)** name.

Parameters
^^^^^^^^^^
* :code:`cutoff`: the (maximum) number of recommended users to consider in the computation of the metric.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    MAP:
      cutoff:
        type: int
        values: [1,5,10]


nDCG
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The cumulative discounted cumulative gain is a metric that considers that a recommendation is better if the relevant links appear at the top of the ranking.
For this, it appears a discount term that depends on the position of the user. It also allows considering several degrees of relevance for the recommended links
(for example, defined by the weights of the edges).

This metric is defined as:

.. math::
  
    \mbox{nDCG}(u) = \frac{\mbox{DCG}(u)}{\mbox{IDCG(u)}}

where the discounted cumulative gain (DCG) for the user is:

.. math::

    \mbox{DCG}(u) = \sum_{k=1}^{|R_u|} \frac{g_u(v_k)}{\log_2(1+k)}

where :math:`|R_u|` represents the length of the recommendation ranking for user :math:`u`, :math:`g_u(v_k)` represents the grade of relevance of user :math:`v_k` for user :math:`u` and :math:`v_k` is the :math:`k`-th recommended user in the ranking.

Then, the :math:`\mbox{IDCG}(u)` term acts as a normalization, and it's the best possible :math:\mbox{DCG}(u) value.

**Reference:** K. Jarvelin, J. Kekälainen. Cumulated Gain-Based Evaluation of IR Techniques. ACM Transactions on Information Systems, 20, 422–446 (2002).

Parameters
^^^^^^^^^^
* :code:`cutoff`: the (maximum) number of recommended users to consider in the computation of the metric.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    nDCG:
      cutoff:
        type: int
        values: [1,5,10]

Precision
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The precision metric measures the proportion of correctly recommended links in a recommendation:

.. math::
  
    \mbox{P}(u) = \frac{|R_u \cap \mbox{relevant}(u)|}{|R_u|}

where :math:`R_u` represents the recommendation ranking for user :math:`u`, and :math:`\mbox{relevant}(u)` is the set of links which have :math:`u` as origin in the evaluation set.

**Reference:** R. Baeza-Yates and B. Ribeiro-Neto. Modern Information Retrieval: The Concepts and Technology behind Search, 2nd ed. (2011).

Parameters
^^^^^^^^^^
* :code:`cutoff`: the (maximum) number of recommended users to consider in the computation of the metric.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Precision:
      cutoff:
        type: int
        values: [1,5,10]

Recall
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The precision metric measures the proportion of correctly recommended links which have been discovered in a recommendation:

.. math::
  
    \mbox{R}(u) = \frac{|R_u \cap \mbox{relevant}(u)|}{|\mbox{relevant}(u)|}

where :math:`R_u` represents the recommendation ranking for user :math:`u`, and :math:`\mbox{relevant}(u)` is the set of links which have :math:`u` as origin in the evaluation set.

**Reference:** R. Baeza-Yates and B. Ribeiro-Neto. Modern Information Retrieval: The Concepts and Technology behind Search, 2nd ed. (2011).

Parameters
^^^^^^^^^^
* :code:`cutoff`: the (maximum) number of recommended users to consider in the computation of the metric.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Recall:
      cutoff:
        type: int
        values: [1,5,10]        