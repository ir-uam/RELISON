Stop conditions
===============
The stop conditions determine when the simulation ends, according to the properties of the diffusion. We consider different options for the RELISON framework:

* `Max. timestamp`_
* `No more new`_
* `No more propagated`_
* `No more timestamps`_
* `No more timestamps nor propagated information`_
* `Num. iter`_
* `Total propagated`_

Max. timestamp
~~~~~~~~~~~~~~
The simulation stops a) when a certain timestamp is reached or b) when no more timestamps are available.

Parameters
^^^^^^^^^^
* :code:`max`: the maximum timestamp.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    Max. timestamp:
      max:
        type: long
        value: maximum_timestamp

No more new
~~~~~~~~~~~~~~
The simulation stops when no more new information is propagated (i.e. people might still propagate information, but
the recipients already know that information).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    No more new:

No more propagated
~~~~~~~~~~~~~~~~~~
The simulation stops when no information has been propagated during the last iteration.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    No more propagated:

No more timestamps
~~~~~~~~~~~~~~~~~~
The simulation stops when no more timestamps are available.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    No more timestamps:

No more timestamps nor propagated information
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The simulation stops when no more timestamps are available or when users are not propagating information to others.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    No more timestamps nor propagated:

Num. Iter
~~~~~~~~~~~~~~~~~~
The simulation stops when a fixed number of iterations have passed.

Parameters
^^^^^^^^^^
* :code:`numIter`: the number of simulation iterations to run.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    Num. iter:
      numIter:
        type: int
        value: max_iter

Total propagated
~~~~~~~~~~~~~~~~~~
The simulation stops when a fixed amount of pieces have been propagated.

Parameters
^^^^^^^^^^
* :code:`limit`: the number of pieces which need to be propagated.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml
  
  stop:
    Num. iter:
      limit:
        type: long
        value: num_pieces