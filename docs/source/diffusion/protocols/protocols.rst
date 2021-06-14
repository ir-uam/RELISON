Protocols
=========
RELISON provides some pre-configured diffusion protocols which can be straightforwardly applied. We list them here and provide a brief description of them:

* `Independent cascade model`_
* `Pull`_
* `Push`_
* `Rumor spreading`_
* `Simple`_
* `Temporal`_
* `Threshold`_

Independent cascade model
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
When diffusion works under this protocol, each time a user receives an information piece, he/she propagates this content to their followers according to a given probability. This probability depends only on the previous user who propagated the information. If the information is not repropagated, then
it is discarded.

**Reference:** J. Goldenberg, B. Libai, and E. Muller. Talk of the Network: A Complex Systems Look at the Underlying Process of Word-of-Mouth, Marketing Letters, 12(3), pp. 211–223 (2001).

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`prob`: the probability of sharing a received piece of information (the same for all the users).

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Independent cascade model:
    prob:
      type: double
      value: value
    numOwn:
      type: int
      value: value

Pull
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this protocol, each iteration, the user selects one of his followees, and obtains the information from that user. The neighbor just propagates a fixed number of their created contents and a fixed number of the received contents.

**Reference:** A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987).

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`numRec`: the number of received contents to propagate each iteration.
* :code:`numWait`: number of iterations that the user has to wait before selecting the same neighbor again. 

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Pull:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    numWait:
      type: int
      value: value   

Push
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this protocol, each iteration, the user selects one of his followers, and propagates him/her information. He/she just propagates a fixed number of their created contents and a fixed number of the received contents.

**Reference:** A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987).

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`numRec`: the number of received contents to propagate each iteration.
* :code:`numWait`: number of iterations that the user has to wait before selecting the same neighbor again. 

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Push:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    numWait:
      type: int
      value: value   

Rumor spreading
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Also called push-pull protocol, this is a combination of the `Pull`_ and `Push`_ protocols, where each user selects a neighbors, and he/she just propagates a fixed number of their created contents and a fixed number of the received contents to the neighbor, and the neighbor sends it to him.

We consider a second version, named *bidirectional rumor-spreading model*, which does not consider the orientation of the edges for selecting the nodes from which receive information and towards whom propagate information.

**Reference:** A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987).

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`numRec`: the number of received contents to propagate each iteration.
* :code:`numWait`: number of iterations that the user has to wait before selecting the same neighbor again. 

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^
For the basic rumor spreading model, the configuration is the following:

.. code:: yaml

  Rumor spreading model:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    numWait:
      type: int
      value: value   

whereas, for the bidirectional version, it is:

.. code:: yaml

  Bidirectional rumor spreading model:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    numWait:
      type: int
      value: value 

Simple
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In the simple protocol, each iteration, the user propagates some of his own contents and (at most) a fixed number of the received contents (selected at random) to his followers.

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`numRec`: the number of received contents to propagate each iteration.

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Simple:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value

Temporal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The temporal protocol just runs, step by step, how information was propagated in an earlier diffusion: it only propagates a piece at the timestamp it was propagated during the previous diffusion and it only repropagates a piece if the user originally repropagated it.

Parameters
^^^^^^^^^^
* :code:`pure`: if true, the user-generated contents are only repropagated if they user received them before the date he repropagated it in the previous diffusion. If he receives it later, he does not propagate it.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Temporal:
    pure:
      type: boolean
      value: true/false


Threshold
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In the threshold model, the users decide to propagate a received piece of content only if a certain threshold of users has sent it to them.
We differentiate two variants:

* **Proportion threshold:** the user decides to propagate a piece after more than a given proportion of the users have sent him the content.
* **Count threshold:** the user decides to propagate a piece after more than a given number of the users have sent him the content.

**Reference:** D. Kempe, J. Kleinberg, and E. Tardos. Maximizing the spread of influence through a social network, KDD 2003, pp. 137–146 (2003).

Parameters
^^^^^^^^^^
For the probability threshold version, the arguments are the following ones:

* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`threshold`: the minimum proportion of users who must send a user-generated content to the user before it can be propagated.

and, for the count threshold version:

* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`threshold`: the minimum number of users who must send a user-generated content to the user before it can be propagated.

Configuration file
^^^^^^^^^^^^^^^^^^
For the proportion threshold version, the configuration is the following:

.. code:: yaml

  Proportion threshold:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    threshold:
      type: double
      value: value   

whereas, for the other version, it is:

.. code:: yaml

  Count threshold:
    numOwn:
      type: int
      value: value
    numRec:
      type: int
      value: value
    threshold:
      type: int
      value: value  