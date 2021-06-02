.. role:: java(code)
	:language: Java


Integrate communities in a Java project
==============================================

By importing the SNA package, it is possible to use the community partitions of the network in a Java project, as well as detect these partitions, or define novel community partition approaches. In this section we clarify how this can be done.

* `Communities`_
* `Dendograms`_
* `Community detection algorithms`_

Communities
^^^^^^^^^^^
The communities in a network represent a partition of the users. A node can only belong to one of these groups. In order to manipulate community partitions, we include different interfaces in the framework. 

The most important is the :java:`Communities` class, which stores one of these community partitions. It identifies the different communities with integer numbers, and has the following methods:

.. code:: Java

	int getNumCommunities()

This method obtains the number of clusters in the partition. 

.. code:: Java

	IntStream getCommunities()

This method obtains an stream with the identifiers of the considered community partitions. 

.. code:: Java

	int getCommunity(U user)

Given a user in the network, it obtains the identifier of the community it belongs to.

**Arguments:**
	* *user*: the identifier of the user.
**Returns**
	* the identifier of the community it belongs to, or -1 if the user does not belong to any community.

.. code:: Java

	Stream<U> getUsers(int community)

**Arguments:**
	* *community*: the community identifier.
**Returns:**
	* an stream containing the users in the community.

.. code:: Java
	
	void addCommunity()

This method adds a community to the partition.

.. code:: Java

	boolean add(U user, int comm)

This function a user to a community.

**Arguments:**
	* *user*: the user.
	* *comm*: the community identifier.
**Returns:**
	* :java:`true` if the user is added, :java:`false` otherwise.

.. code:: Java

	int getCommunitySize(int comm)

Finally, this method counts the number of users in a given community.

**Arguments:**
	* *comm*: the community identifier.
**Returns:**
	* the number of users in the community.


Dendograms
^^^^^^^^^^
Sometimes, community detection algorithms do not only find a community partition, but what we call a dendogram: a tree containing different community partitions, where the leaves represent the nodes in the network, and intermediate nodes consider the union of the communities represented by their children. 

We provide the class :java:`Dendogram` for reading and working with these structures, allowing to get community partitions by the number of communities, or by obtaining communities of a given size.

It has the following methods:

.. code:: Java

	Tree<U> getTree()

This method obtains the underlying tree of the dendogram.

**Returns:**
	* the underlying tree of the dendogram.

.. code:: Java

	Communities<U> getCommunitiesByNumber(int n)

Obtains a community partition containing, at most, :java:`n` communities.

**Arguments:**
	* *n*: the maximum number of communities.
**Returns**
	* the desired community partition, or :java:`null` if something failed.

.. code:: Java

	Map<Integer, Communities<U>> getCommunitiesByNumber()

It obtains all the possible community partitions by number. 

**Returns**
	* a map, indexed by the number of communities, containing the different community partitions.

.. code:: Java

	Communities<U> getCommunitiesBySize(int size)

It obtains a partition of the network where the maximum number of users on each community is provided.

**Arguments:**
	* *size*: the maximum number of users on each community.
**Returns**
	* the community partition if everything goes well, :java:`null` otherwise.

.. code:: Java

	Map<Integer, Communities<U>> getCommunitiesBySize()

It obtains all the possible community partitions by size. 

**Returns**
	* a map, indexed by the maximum community size, containing the different community partitions.

Community detection algorithms
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
In order to detect communities in a network, we can use many different algorithms. Community detection algorithms inherit the :java:`CommunityDetectionAlgorithm` interface. This interface must be implemented in case we want to develop novel approaches. It has the following methods:

.. code::

	Communities<U> detectCommunities(Graph<U> graph)

This method, given a network, identifies the community partition according to this
algorithm.

**Arguments:**
	* *graph*: the social network graph for detecting communities.
**Returns**
	* the community partition if everything goes well, :java:`null` otherwise.


Then, if the algorithm can find a dendogram, it also inherits the :java:`DendogramCommunityDetectionAlgorithm` interface, which adds the following method:

.. code::

	    Dendogram<U> detectCommunityDendogram(Graph<U> graph)

which finds the dendogram of the network.

**Arguments:**
	* *graph*: the social network graph for detecting communities.
**Returns**
	* the dendogram if everything goes well, :java:`null` otherwise.

