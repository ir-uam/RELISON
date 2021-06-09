Metrics
=========
In order to evaluate link prediction approaches, it is common to use some machine learning and classification metrics. We include the following ones:

* `Accuracy`_
* `Area under the ROC curve`_
* `F1 Score`_
* `Precision`_
* `Recall`_

Accuracy
~~~~~~~~~~
The accuracy measures the number of correctly classified links.

.. math::
  
  \mbox{Accuracy} = \frac{\mbox{TP} + \mbox{TN}}{\mbox{TP} + \mbox{TN} + \mbox{FP} + \mbox{FN}}

where :math:`TP` is the number of true positives, :math:`FP` is the number of false positives, :math:`TN` is the number of 
true negatives, and :math:`FN` is the number of false negatives.

Parameters
^^^^^^^^^^
We have two options here (mutually exclusive):

* :code:`cutoff`: the (maximum) number of predicted links to consider (all the remaining links shall be considered as negatively predicted links).
* :code:`threshold`: the minimum score to consider as positive (all the remaining links shall be considered as negatively predicted links).

When both appear in the configuration file, they will be considered separately.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Accuracy:
      cutoff:
        type: int
        values: [1,5,10]
      threshold:
        type: double
        values: [0.2,0.5,1.0]

Area under the ROC curve
~~~~~~~~~~~~~~~~~~~~~~~~~~
The area under the receiver operating characteristic curve (AUC), as its name indicates, measures the area under a curve. 
Such curve shows the rate of true positives as a function of the rate of false positives.

**Reference:** T. Fawcett. An introduction to ROC analysis. Pattern Recognition Letters, 27(8), 861–874 (2006). 

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    AUC:

F1 Score
~~~~~~~~~~
The F1 score combines precision and recall (see `Precision`_ and `Recall`_, respectively) in a single value. It is the harmonic mean of the two measures:

.. math::
  
  \mbox{F1-score} = \frac{2\cdot\mbox{TP}}{2\cdot\mbox{TP} + \mbox{FN} + \mbox{FP}}

where :math:`TP` is the number of true positives, :math:`FP` is the number of false positives and :math:`FN` is the number of false negatives.

Parameters
^^^^^^^^^^
We have two options here (mutually exclusive):

* :code:`cutoff`: the (maximum) number of predicted links to consider (all the remaining links shall be considered as negatively predicted links).
* :code:`threshold`: the minimum score to consider as positive (all the remaining links shall be considered as negatively predicted links).

When both appear in the configuration file, they will be considered separately.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    F1-score:
      cutoff:
        type: int
        values: [1,5,10]
      threshold:
        type: double
        values: [0.2,0.5,1.0]

Precision
~~~~~~~~~~
The precision measures the proportion of correctly predicted links among those the algorithm has label as positive.

.. math::
  
  \mbox{Precision} = \frac{\mbox{TP}}{\mbox{TP} + \mbox{FP}}

where :math:`TP` is the number of true positives and :math:`FP` is the number of false positives.

Parameters
^^^^^^^^^^
We have two options here (mutually exclusive):

* :code:`cutoff`: the (maximum) number of predicted links to consider (all the remaining links shall be considered as negatively predicted links).
* :code:`threshold`: the minimum score to consider as positive (all the remaining links shall be considered as negatively predicted links).

When both appear in the configuration file, they will be considered separately.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Precision:
      cutoff:
        type: int
        values: [1,5,10]
      threshold:
        type: double
        values: [0.2,0.5,1.0]        

Recall
~~~~~~~~~~
The recall measures the proportion of correctly predicted links which have been labeled as positive

.. math::
  
  \mbox{Precision} = \frac{\mbox{TP}}{\mbox{TP} + \mbox{FN}}

where :math:`TP` is the number of true positives and :math:`FN` is the number of false negatives.

Parameters
^^^^^^^^^^
We have two options here (mutually exclusive):

* :code:`cutoff`: the (maximum) number of predicted links to consider (all the remaining links shall be considered as negatively predicted links).
* :code:`threshold`: the minimum score to consider as positive (all the remaining links shall be considered as negatively predicted links).

When both appear in the configuration file, they will be considered separately.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Recall:
      cutoff:
        type: int
        values: [1,5,10]
      threshold:
        type: double
        values: [0.2,0.5,1.0]                