Random walks
============================================
These algorithms consider the properties of random walks to determine the recommendation score. We include the following approaches:

* `Commute time`_
* `Hitting time`_
* `HITS`_
* `PageRank`_
* `Personalized PageRank`_
* `Personalized HITS`_
* `Personalized SALSA`_
* `PropFlow`_
* `SALSA`_

Commute time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Average time that a random walker needs to go from the target to the origin user and come back. We consider two variants, depending
on the underlying random walk algorithm:

* **PageRank**
* **Personalized PageRank**

**Reference:** D. Liben-Nowell, D., J. Kleinberg. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7) (2007).

Parameters
^^^^^^^^^^
* :code:`r`: the PageRank teleport rate.

Configuration file
^^^^^^^^^^^^^^^^^^

The non-personalized PageRank version is selected as:

.. code:: yaml

    Commute time PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

and the personalized one as:

.. code:: yaml

    Commute time personalized PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

Hitting time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Average time that a random walker needs to go from the target to the candidate user. We consider two variants, depending
on the underlying random walk algorithm:

* **PageRank**
* **Personalized PageRank**

**Reference:** D. Liben-Nowell, D., J. Kleinberg. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7) (2007).

Parameters
^^^^^^^^^^
* :code:`r`: the PageRank teleport rate.

Configuration file
^^^^^^^^^^^^^^^^^^

The non-personalized PageRank version is selected as:

.. code:: yaml

    Hitting time PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

and the personalized one as:

.. code:: yaml

    Hitting time personalized PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

HITS
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Algorithm based on the Hyperlink-Induced Topic Search algorithm.

**Reference:** J.M. Kleinberg. Authoritative Sources in a Hyperlinked Environment. Journal of the ACM 46(5), 604-642 (1999).

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    HITS:
      mode:
        type: boolean
        values: [true,false]

PageRank
~~~~~~~~~~
This algorithm takes the non-personalized PageRank algorithm (initially designed for estimating the importance of web pages) as a recommendation / prediction method.

**Reference:**  S. Brin, L. Page. The Anatomy of a Large-Scale Hypertextual Web Search Engine. 7th Annual International Conference on World Wide Web (WWW 1998), 107-117 (1998).

Parameters
^^^^^^^^^^
* :code:`r`: teleport rate of the random walk.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

Personalized HITS
~~~~~~~~~~~~~~~~~~
Personalized version of the HITS algorithm, where a teleport probability to the target user of the recommendation has been added.

**Reference:** A. Goel. The Who-To-Follow System at Twitter: Algorithms, Impact and Further Research. 32rd Annual International Conference on World Wide Web (2014), industry track (2014)

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.
* :code:`alpha:` teleport rate.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Personalized HITS:
      alpha:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      mode:
        type: boolean
        values: [true, false]

Personalized PageRank
~~~~~~~~~~~~~~~~~~~~~~
Personalized version of the PageRank algorithm, where the random walk always teleports to the target user. Also known as rooted PageRank.

**Reference:**  S. White, P. Smyth. Algorithms for Estimating Relative Importance in Networks. 9th Annual ACM SIGKDD International Conference on Knowledge Discovery and Data Mining (KDD 2003).

Parameters
^^^^^^^^^^
* :code:`r`: teleport rate of the random walk.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Personalized PageRank:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

Personalized SALSA
~~~~~~~~~~~~~~~~~~
Personalized version of the SALSA algorithm, where a teleport probability to the target user of the recommendation has been added.

**Reference:** A. Goel, P. Gupta, J. Sirois, D. Wang, A. Sharma, S. Gurumurthy. The who-to-follow system at Twitter: Strategy, algorithms and revenue impact. Interfaces 45(1), 98-107 (2015)

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.
* :code:`alpha:` teleport rate.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Personalized SALSA:
      alpha:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      mode:
        type: boolean
        values: [true, false]


PropFlow
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm considers the probability that a random walker starting in the target user reaches the candidate user in less than few steps, using
the edge weights as transition probabilities.

**Reference:** R. Lichtenwalter, J. Lussier, N. Chawla. New perspectives and methods in link prediction. 16th ACM SIGKDD International Conference on Knowledge Discovery and Data Mining (KDD 2010), 243-252 (2010).

Parameters
^^^^^^^^^^
* :code:`maxLength`: importance of the Laplacian matrix.
* :code:`orientation`: the method to choose the orientation of the paths.

    * :code:`IN`: the walker advances through incoming edges.
    * :code:`OUT`: the walker advances through outgoing edges.
    * :code:`UND`: the walker advances through any edge.
    * :code:`MUTUAL`: the walker advances through reciprocal edges.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    PropFlow:
      maxLength:
        type: int
        values: [3,4,5,6]
      orientation:
        type: orientation
        values: [IN,OUT,UND,MUTUAL]
      (weighted:
        type: boolean
        values: [true,false])


SALSA
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Adaptation of the Stochastic Approach for Link-Structure Analysis (SALSA) algorithm.

**Reference:** R. Lempel, S. Moran. SALSA: The Stochastic Approach for Link-Structure Analysis. ACM TOIS 19(2), 131-160 (2001)

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    SALSA:
      mode:
        type: boolean
        values: [true,false]
