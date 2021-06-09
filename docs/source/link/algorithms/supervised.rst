Supervised algorithms
============================================
In addition to the rest of algorithms included in the comparative, we include some supervised algorithms (either classification approaches or learning to rank algorithms).
All these methods receive, as input, two sets of feature vectors: a set of feature vectors for training the machine learning approaches, and a set of feature vectors to
compute the definitive scores. We provide information on how to find such feature vectors in SEE HOW TO REFER TO THIS.

We add the following algorithms:

* `LambdaMART`_
* `Weka classifiers`_

LambdaMART
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
LambdaMART represents a learning to rank algorithm based on gradient boosted regression trees, with a great success on information retrieval.

**Reference:** Y. Ganjisaffar, R. Caruana, C. Lopes. Bagging Gradient-Boosted Trees for High Precision, Low Variance Ranking Models. 34th Annual International ACM SIGIR conference on Research and development in Information Retrieval (SIGIR 2011), 85-94 (2011).


Parameters
^^^^^^^^^^
* :code:`train`: the file containing the training vectors for the training set.
* :code:`valid`: the file containing the validation vectors for training the algorithm.
* :code:`test` : the file containing the definitive feature vectors.
* :code:`config` : route of the configuration file for the JForests LambdaMART (see https://github.com/yasserg/jforests)
* :code:`tmp`: route to a temporary folder.

Configuration file
^^^^^^^^^^^^^^^^^^

The non-personalized PageRank version is selected as:

.. code:: yaml

    LambdaMART:
      train:
        type: string
        values: [file1,...,fileN]
      valid:
        type: string
        values: [file1,...,fileN]
      test:
        type: string
        values: [file1,...,fileN]
      config:
        type: string
        values: [file1,...,fileN]
      tmp:
        type: string
        values: [dir1,...,dirN]

Weka classifiers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
As classifiers, we have integrated in our code the use of classifiers from Weka. We can only access some of them in the original program, but all of them can be used
if the :code:`MachineLearningWekaRecommender` is used from code.

**Reference:** E. Frank, M. A. Hall, and I. H. Witten. The WEKA Workbench. Online Appendix for "Data Mining: Practical Machine Learning Tools and Techniques", 4th edition (2016).


Parameters
^^^^^^^^^^
* :code:`train`: the file containing the training vectors for the training set.
* :code:`test` : the file containing the definitive feature vectors.
* :code:`classifier` : classifier configurations.

Classifiers
^^^^^^^^^^^
From the recommendation program, we allow the use of three different classifiers: logistic regression, naive Bayes, and random forests.
The first two do not have any additional parameters. The random forests adds the number of decision stumps.


Configuration file
^^^^^^^^^^^^^^^^^^

We configure these approaches as:

.. code:: yaml

    Weka:
      train:
        type: string
        values: [file1,...,fileN]
      test:
        type: string
        values: [file1,...,fileN]
      classifier:
        type: object
        objects:
          logistic:
          naive-bayes:
          random-forest:
            iterations: 
              type: int
              values: [5,10,15,20,25]
