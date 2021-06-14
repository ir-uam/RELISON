Expiration mechanisms
=====================
During the simulations, users receive many information pieces. However, they do not share them all with their contacts in the same moment they receive them: sometimes, they are never propagated; other times, they are propagated many iterations later (according to other factors, like the amount of people who has sent them the information). Expiration mechanisms decide whether a piece which has been received but not propagated can still be shared in the upcoming iterations. 

RELISON has implemented many expiration mechanisms, which can be used to build custom protocols. We provide them a brief explanation below.

* `All not propagated`_
* `All not real propagated`_
* `All not real propagated with timestamp`_
* `Exponential decay`_
* `Infinite time`_
* `Timed`_

All not propagated
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this expiration mechanism, every user-generated content which has been received but not repropagated is discarded.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: All not propagated

All not real propagated
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this expiration mechanism, the only information pieces that can still be shared in the future are those which were actually shared during a previous diffusion process.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: All not real propagated

All not real propagated with timestamp
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This expiration mechanism follows the `All not real propagated` one, but with a twist. Under this expiration mechanism, the only information pieces that can still be shared in the future are those which were actually shared during a previous diffusion process if and only if the timestamp of the 
repropagation has not yet passed.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: All not real propagated timestamp

Exponential decay
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this expiration mechanism, each information piece has a probability of staying on the list over time, following and exponential distribution: The probability that an information piece can still be propagated after :math:`t` iterations is:

.. math::
  
  p(i) = e^{-\lambda t}

where :math:`\lambda` is what we call the "half-life" of the content, i.e. the time required for the piece to have a 50% probability of being removed.

Parameters
^^^^^^^^^^
* :code:`half-life`: the time required for the piece to have a 50% probability of being discarded.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Exponential decay
    params:
      half-life:
        type: double
        value: value

Infinite time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this expiration mechanism, information pieces are never discarded: once the user has received them, they remain in the received list until the user decides to share it.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Infinite time


Timed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
When this expiration mechanism is used, a received information piece has to be shared before a fixed time has passed. Otherwise, it is discarded.

Parameters
^^^^^^^^^^
* :code:`max-time`: the number of iterations before an information piece is discarded.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  expiration:
    name: Timed
    params:
      max-time:
        type: int
        value: value
