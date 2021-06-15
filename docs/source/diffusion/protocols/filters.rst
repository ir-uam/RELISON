Information filters
====================
The information filters allow the preprocessing of the information used in the diffusion simulations. RELISON provides the following ones:

* `Basic`_
* `Creator`_
* `Empty feature`_
* `Information feature`_
* `Information feature selection`_
* `Minimum information feature frequency`_
* `Num. information pieces`_
* `Only repropagated pieces`_
* `Relevant edges`_


Basic
~~~~~~
This filter does not modify the data. It returns the same, original data.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  Basic:

Creator
~~~~~~~~
This filter only keeps information pieces with information about the user who created them.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  Creator:

Empty feature
~~~~~~~~~~~~~~
This filter adds an empty feature for each information piece without features.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

  Empty feature:

Information feature
~~~~~~~~~~~~~~~~~~~~
This filter only leaves those information pieces containing a given feature.

Parameters
^^^^^^^^^^
* :code:`feature`: the name of the feature

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Information feature:
      feature:
        type: string
        value: feature_name

Information feature selection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This filter only keeps some information feature values, and the information pieces containing those values.

Parameters
^^^^^^^^^^
* :code:`feature`: the name of the feature
* :code:`file`: a file containing the feature values to keep (one each line)


Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Information feature selection:
      feature:
        type: string
        value: feature_name 
      file:
        type: string
        value: file_route

Minimum information feature frequency
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This filter only keeps the information features which appear, at least, in a fixed number of information pieces. It only keeps
information pieces containing those features.

Parameters
^^^^^^^^^^
* :code:`feature`: the name of the feature
* :code:`minValue`: a file containing the feature values to keep (one each line)


Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Minimum information feature frequency:
      feature:
        type: string
        value: feature_name 
      minValue:
        type: int
        value: value

Num. information pieces
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This filter only keeps a limited number of information pieces for each user. In case a user has more user-generated contents, it selects the given amount randomly.

Parameters
^^^^^^^^^^
* :code:`numPieces`: the maximum number of information pieces to keep for each user.


Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Num. information pieces:
      numPieces:
        type: int
        value: value 

Only repropagated pieces
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This filter keeps only those information pieces which have been propagated by users different than their creator in a previous diffusion process.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Only repropagated:


Relevant edges
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
If recommendation links have been added to the original graph, we only keep those recommended edges which were relevant.

Configuration file
^^^^^^^^^^^^^^^^^^
.. code:: yaml

    Relevant edges:

