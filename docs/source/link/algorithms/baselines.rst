Baselines
============================================
This family of people recommendation algorithms includes some important sanity-check baselines to include in the experiments.

* `Random recommendation`_
* `Popularity-based recommendation`_

Random recommendation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm generates random recommendation scores for each candidate user in the network.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

	Random:

Popularity-based recommendation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm recommends the set of most followed users in the network (i.e. those maximizing the in-degree).

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Popularity: