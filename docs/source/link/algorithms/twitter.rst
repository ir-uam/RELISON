Twitter
============================================
These algorithms have been reported to be used at some point (at least, in experiments) for the Twitter Who-to-follow service.

* `Closure`_
* `Cosine similarity`_
* `Love`_
* `Money`_

Closure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Algorithm that recommends reciprocal edges according to the number of common neighbors between the already existing edge endpoints.

* **PageRank**
* **Personalized PageRank**

**Reference:** P. Gupta, A. Goel, J. Lin, A. Sharma, D. Wang, R. Zadeh. WTF: The Who to Follow Service at Twitter. 22nd Annual International Conference on World Wide Web (WWW 2013), 505-514 (2013).

Configuration file
^^^^^^^^^^^^^^^^^^

The non-personalized PageRank version is selected as:

.. code:: yaml

    Closure:

Cosine similarity
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This considers several variants of the cosine similarity in the experiments: we consider three:

* Average cosine similarity: it takes the average similarity of the candidate user with the authorities the target user is currently following.
* Centroid cosine similarity: for each user, a centroid is built, using the vectors of the followed users. The score is the cosine of two vectors.
* Maximum cosine similarity: it takes the maximum similarity between the authorities that the target user is currently following.

Parameters
^^^^^^^^^^
* :code:`r`: the PageRank teleport rate, for computing the circles of trust.
* :code:`circlesize`: the size of the circles of trust. If negative or zero, we take the maximum possible size.

Configuration file
^^^^^^^^^^^^^^^^^^

The Yaml code for the average cosine algorithm is:

.. code:: yaml

    Average cosine:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      circlesize:
        type: int
        values: [0,10,100,1000]

for the centroid cosine similarity variant, it is:

.. code:: yaml

    Centroid cosine:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      circlesize:
        type: int
        values: [0,10,100,1000]

and, for the maximum cosine similarity:

.. code:: yaml

    Maximum cosine:
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      circlesize:
        type: int
        values: [0,10,100,1000]

Love
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Variant of the personalized HITS algorithm, computed over a circle of trust.

**Reference:** A. Goel. The Who-To-Follow System at Twitter: Algorithms, Impact and Further Research. 32rd Annual International Conference on World Wide Web (2014), industry track (2014).

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.
* :code:`alpha`: teleport rate for the personalized HITS algorithm.
* :code:`r`: the PageRank teleport rate, for computing the circles of trust.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Love:
      mode:
        type: boolean
        values: [true,false]
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      alpha:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1

Money
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Variant of the personalized SALSA algorithm, computed over a circle of trust.

**Reference:** A. Goel, P. Gupta, J. Sirois, D. Wang, A. Sharma, S. Gurumurthy. The who-to-follow system at Twitter: Strategy, algorithms and revenue impact. Interfaces 45(1), 98-107 (2015).

Parameters
^^^^^^^^^^
* :code:`mode`: true if we want to use the authorities scores, false if we want to use the hubs scores.
* :code:`alpha`: teleport rate for the personalized HITS algorithm.
* :code:`r`: the PageRank teleport rate, for computing the circles of trust.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Money:
      mode:
        type: boolean
        values: [true,false]
      r:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1
      alpha:
        type: double
        range:
        - start: 0.1
          end: 0.99
          step: 0.1