Selection mechanisms
====================
The selection mechanism identifies the information pieces which the users will propagate each iteration. RELISON provides the following mechanisms:


* `Count`_
* `Independent cascade model`_
* `Only own`_
* `Pull-push`_
* `Real propagated`_
* `Recommender`_
* `Threshold`_
* `Timestamp-based`_
* `Timestamp-ordered`_

Count
~~~~~~
This mechanism selects, at a given time, a fixed number of pieces created by the user, a fixed number of the received pieces and a fixed number of the already propagated pieces to send to other users. Pieces are randomly selected.

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`numRec`: the number of received pieces to propagate.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  selection:
    name: Count
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      numRec:
        type: int
        value: number_of_received_pieces
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)

Independent cascade model
~~~~~~~~~~~~~~~~~~~~~~~~~~
This mechanism selects, at a given time, for each user, a fixed number of pieces created by the user and a fixed number of the already propagated pieces to send to other users. Then, propagates received pieces with a probability that depends only on the user who sent him the conent.

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`prob`: the probability of propagating an information piece.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

Note: when used in code, there is another option, which takes as input a graph containing the probabilities. However, this option is not (at the moment) available from a configuration file.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  selection:
    name: Independent cascade model
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      prob:
        type: double
        value: probability
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)        

Only own
~~~~~~~~~~
This mechanism only propagates information pieces created by the user. It selects a fixed number of them (chosen randomly).

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  selection:
    name: Only own
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)

Pull-push
~~~~~~~~~~
This mechanism makes a user share all the information he knows (i.e. all the received information and all the information he has propagated in the past). Along with this, it selects a fixed number of their own (not already propagated) contents.

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of user-created information pieces to propagate.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  selection:
    name: Push-pull
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces

Real propagated
~~~~~~~~~~~~~~~~~~~~
This mechanism selects, at a given time, a fixed number of the pieces created by the user, a fixed number of already propagated pieces, and the user repropagates a selection of the received information pieces that he/she forwarded in a previous diffusion process (for example, real retweets extracted from Twitter).

Depending on how many of the received information pieces that the user propagates, we differentiate two variants:
* **all**: each time the user receives a piece he / she forwarded in a previous diffusion process, he / she propagates it.
* **count**: it just selects up to a maximum number of those pieces.

Parameters
^^^^^^^^^^
Both variants share the following parameters
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

The **count** version has an additional parameter:
* :code:`numRec`: the number of received pieces to propagate.


Configuration file
^^^^^^^^^^^^^^^^^^
The version that propagates all the received pieces passing the filter has the following configuration file:
.. code:: yaml

.. code:: yaml

  selection:
    name: All real propagated
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again) 

while the version that propagates only some of them:

.. code:: yaml

  selection:
    name: Count real propagated
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)        



Recommender
~~~~~~~~~~~~~~~~~~~~
This mechanism selects, at a given time, a fixed number of the pieces created by the user. At the moment of selecting which pieces to propagate from the received ones, it also selects (at most) a fixed number, but considers the origin of those pieces. With a given probability :math:`p`, it chooses a piece received from a link created via a recommendation. With :math:`1-p`, it selects a piece from one of the original links. 

Depending on how the selection is done, we differentiate three variants:

* **basic**: before choosing an information piece, a coin is tossed to determine from which list the information piece to propagate will be selected.
* **batch**: each iteration, for each user, it is selected whether all the pieces are selected from the set of recommended links or from the set of original links.
* **pure**: here, we take :math:`p = `1`, i.e. pieces always come from the recommended links.

Parameters
^^^^^^^^^^
All variants share the following parameters:
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`orientation`: the neighborhood to consider.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

And the basic and batch variants also have the following one:
* :code:`prob`: probability of choosing pieces which have been sent through recommended links.

Configuration file
^^^^^^^^^^^^^^^^^^
The basic version has the following configuration file:

.. code:: yaml

  selection:
    name: Recommender
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      numRec:
        type: int
        value: number_of_received_pieces
      prob:
        type: double
        value: probability
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)

while the batch version has:

.. code:: yaml

  selection:
    name: Batch recommender
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      numRec:
        type: int
        value: number_of_received_pieces
      prob:
        type: double
        value: probability
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)                

and the pure one:

.. code:: yaml

  selection:
    name: Pure recommender
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      numRec:
        type: int
        value: number_of_received_pieces
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)    


Threshold
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In the threshold selection mechanism, the users decide to propagate a received piece of content only if a certain threshold of users has sent it to them.
We differentiate two variants:

* **Probability threshold:** the user decides to propagate a piece after more than a given proportion of the users have sent him the content.
* **Count threshold:** the user decides to propagate a piece after more than a given number of the users have sent him the content.

Then, for each of them, we consider two possibilities, depending on how many pieces that overcome the threshold are shared:
* **all**: we propagate all the received pieces that pass the filter.
* **limited**: we propagate (at most) a fixed number of them.

**Reference:** D. Kempe, J. Kleinberg, and E. Tardos. Maximizing the spread of influence through a social network, KDD 2003, pp. 137–146 (2003).

Parameters
^^^^^^^^^^
All versions receive the following parameters:
* :code:`numOwn`: the number of contents created by the user to propagate each iteration.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

In the probability thresold version, we have this additional parameters
* :code:`threshold`: the minimum proportion of users who must send a user-generated content to the user before it can be propagated.
* :code:`orientation`: the neighbor selection.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)

whereas we have the following one for the count version:
* :code:`threshold`: the minimum number of users who must send a user-generated content to the user before it can be propagated.

Finally, both limited versions have this parameter:
* :code:`numRec`: the maximum number of received pieces to propagate.

Configuration file
^^^^^^^^^^^^^^^^^^
For the probability threshold version, the configuration is the following:

.. code:: yaml
  
  selection:
    name: Proportion threshold
    params:
      numOwn:
        type: int
        value: value
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      threshold:
        type: double
        value: value   
      (numRepr:
        type: int
        value: value)

whereas, for the count version, it is:

.. code:: yaml

  selection:
    name: Count threshold
    params:
      numOwn:
        type: int
        value: value
      threshold:
        type: int
        value: value 
      (numRepr:
        type: int
        value: value)

Then, the limited version of the probability threshold is: 

.. code:: yaml

  selection:
    name: Limited proportion threshold
    params:
      numOwn:
        type: int
        value: value
      numRec:
        type: int
        value: value
      orientation:
        type: orientation
        value: IN/OUT/UND/MUTUAL
      threshold:
        type: double
        value: value
      (numRepr:
        type: int
        value: value)

and the limited count threshold:

.. code:: yaml

  selection:
    name: Limited count threshold
    params:
      numOwn:
        type: int
        value: value
      numRec:
        type: int
        value: value
      threshold:
        type: int
        value: value
      (numRepr:
        type: int
        value: value)

Timestamp-based
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In these selection mechanisms, a user does only propagate his own content when the timestamp of the simulation corresponds to the timestamp on a previous diffusion process. 

Depending on how the received pieces are shared with other users, we differentiate two cases:
* **Loose**: in this case, a received information piece is shared a) only if the user shared it in a previous process and b) only if the current timestamp is smaller or equal to the timestamp at which the user shared it in that process.
* **Pure**: in this case, a received information piece is shared a) only if the user shared it in a previous process and b) only if the current timestamp is smaller or equal to the timestamp at which the user shared it in that process.

Configuration file
^^^^^^^^^^^^^^^^^^
For the loose version, the configuration is the following:

.. code:: yaml

  Loose timestamp-based:
    numOwn:
    

whereas, for the pure version, it is:

.. code:: yaml

  Pure timestamp-based:

Timestamp-ordered
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This selection mechanism is equivalent to `Count`_, but, pieces are selected according to their creation / reception timestamps.

Parameters
^^^^^^^^^^
* :code:`numOwn`: the number of user-created information pieces to propagate.
* :code:`numRec`: the number of received pieces to propagate.
* :code:`numRepr`: (*OPTIONAL*) the number of already propagated pieces to repropagate again. By default: 0.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  selection:
    name: Timestamp-ordered
    params:
      numOwn: 
        type: int
        value: number_of_own_pieces
      numRec:
        type: int
        value: number_of_received_pieces
      (numRepr:
        type: int
        value: number_of_pieces_to_repropagate_again)