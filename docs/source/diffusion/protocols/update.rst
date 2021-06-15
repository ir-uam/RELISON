Update mechanisms
=================
During the diffusion process, it is likely for a user to receive the same information several times. Each iteration, the received information piece contains the following information: the identifiers of the users who have sent him the information in the corresponding iteration and the timestamp of the interaction. 

However, when the user has already received the information piece in previous iterations, it is necessary to combine them into just one piece. This is the task of the update mechanism: select which information is actually used in the diffusion process.

RELISON provides the following mechanisms:

* `Merger`_
* `Newest`_
* `Oldest`_

Merger
~~~~~~~~
In this case, all the information pieces are merged into one: the user can access all the people who sent him the information piece during the diffusion process. The timestamp of the reception is the timestamp of the last iteration he/she received the piece.

.. code:: yaml

    update:
      name: Merger

Newest
~~~~~~
In this case, the user only knows information about the newest received information piece (i.e. it can access the people who have sent this information piece in the last iteration, and the timestamp of reception is the one of this last iteration).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  update:
    name: Newest

Oldest
~~~~~~
In this case, the user only knows information about the oldest received information piece (i.e. it can access the people who have sent this information piece first, and the timestamp of reception is the one of the first time he/she received it).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  update:
    name: Oldest