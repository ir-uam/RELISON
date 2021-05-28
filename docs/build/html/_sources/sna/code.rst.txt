.. role:: java(code)
	:language: Java


Integrate structural metrics in a Java project
==============================================

By importing the SNA package, it is possible to use different structural metrics in your project, as well as provide novel metrics which can be used within the framework.
In this section, we summarize the main methods and functionalities for each of the metric types.

* `Vertex metrics`_
* `Edge or pair metrics`_
* `Graph metrics`_
* `Individual community metrics`_
* `Global community metrics`_

Vertex metrics
^^^^^^^^^^^^^^

Vertex metrics allow the computation of metrics for each individual user in the network: properties like degree, local clustering coefficient, etc. All vertex metrics inherit from the :java:`VertexMetric` interface. This metric interface has the following methods, which have to be implemented to develop new node metrics:

.. code:: Java
	
	double compute(Graph<U> graph, U user)

This method just finds the metric value for a single node in the network. 

**Arguments:**
	* *graph*: the network we want to study.
	* *user*: the identifier of the user.
**Returns**
	* the metric value for the given user in the network.

It receives the graph we want to compute the metric for, and the user identifier.

.. code:: Java

	Map<U, Double> compute(Graph<U> graph)

This method is useful for computing distributions, as it returns the metric value for each single user in the network. Such values are stored in a map, indexed by the user identifier.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* a map, indexed by user identifier, containing the metric values for all the nodes in the network.

.. code:: Java

	Map<U, Double> compute(Graph<U> graph, Stream<U> users)

This method is similar to the previous one, but it limits the metric to the users in the provided stream. Again, it returns a map, indexed by the user identifier.

**Arguments:**
	* *graph*: the network we want to study.
	* *users*: an stream containing the identifiers of a set of users.
**Returns**
	* a map, indexed by user identifier, containing the metric values for all the users in the stream.

.. code:: Java

	double averageValue(Graph<U> graph)

This method averages the metric over the whole set of users in the network.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* the average value of the metric.

.. code:: Java

	double averageValue(Graph<U> graph, Stream<U> users)

This method averages the metric over an specified set of users.

**Arguments:**
	* *graph*: the network we want to study.
	* *users*: an stream containing the identifiers of a set of users.

**Returns**
	* the average value of the metric for the provided users.

Edge or pair metrics
^^^^^^^^^^^^^^^^^^^^
This family of metrics takes different pairs of users in the network, regardless of whether a link between them exist or not. Examples of these metrics include distance between users or embeddedness. All these metrics inherit the :java:`PairMetric` interface, which has the following methods:

.. code:: Java
	
	double compute(Graph<U> graph, U u, U v)

This method just finds the metric value for a single pair of users in the network. 

**Arguments:**
	* *graph*: the network we want to study.
	* *u*: the first user in the pair.
	* *v*: the second user in the pair.
**Returns**
	* the metric value for the given pair of users in the network.

It receives the graph we want to compute the metric for, and the user identifier.

.. code:: Java

	Map<Pair<U>, Double> compute(Graph<U> graph)

This method is useful for computing distributions, as it returns the metric value for each pair of users in the network. Such values are stored in a map, indexed by the pair of user identifiers.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* a map, indexed by a pair of user identifiers, containing the metric values for all the pairs in the network.

.. code:: Java

	Map<U, Double> computeOnlyLinks(Graph<U> graph)

This method is similar to the previous one, but it limits the metric to the set of links in the network. 

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* a map, indexed by a pair of user identifiers, containing the metric values for all the pairs in the network.

.. code:: Java

	Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)

This method is similar limits the metric a provided set of user pairs.

**Arguments:**
	* *graph*: the network we want to study.
	* *pairs*: an stream containing the pairs of user identifiers.
**Returns**
	* a map, indexed by a pair of user identifiers, containing the metric values for all the user pairs in the stream.

.. code:: Java

	Function<U, Double> computeOrig(Graph<U> graph, U orig)

This method generates a function for obtaining the values of all pairs which have,
as first node, a provided one.

**Arguments:**
	* *graph*: the network we want to study.
	* *orig*: the first node of the pairs.
**Returns**
	* a function that allows to obtain the values for all pairs which have *orig* as first node.

.. code:: Java

	Function<U, Double> computeDest(Graph<U> graph, U dest)

This method generates a function for obtaining the values of all pairs which have,
as second node, a provided one.

**Arguments:**
	* *graph*: the network we want to study.
	* *dest*: the second node of the pairs.
**Returns**
	* a function that allows to obtain the values for all pairs which have *orig* as first node.

.. code:: Java

	double averageValue(Graph<U> graph)

This method averages the metric over all the possible pairs of users in the network.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* the average value of the metric.

.. code:: Java

	double averageValueOnlyLinks(Graph<U> graph)

This method averages the metric over all the links in the network.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* the average value of the metric.	

.. code:: Java

	double averageValue(Graph<U> graph, Stream<Pair<U>> pairs)

This method averages the metric over an specified set of user pairs.

**Arguments:**
	* *graph*: the network we want to study.
	* *pairs*: an stream containing the pairs of user identifiers to consider.

**Returns**
	* the average value of the metric for the provided user pairs.

Graph metrics
^^^^^^^^^^^^^
This family of metrics analyzes structural properties of the whole network. They inherit from the :java:`GraphMetric` interface, which has the following methods:

.. code:: Java

	double compute(Graph<U> graph)

This method finds the value of the structural metric for the given network.

**Arguments:**
	* *graph*: the network we want to study.
**Returns**
	* the value of the metric.	


Individual community metrics
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Given a community partition of the network, this family of metrics allows the analysis of the properties of the different communities. They inherit from the :java:`IndividualCommunityMetric` interface, which has the following methods:

.. code:: Java

	double compute(Graph<U> graph, Communities<U> comms, int indiv)

This method computes the metric for an individual community.

**Arguments:**
	* *graph*: the network we want to study.
	* *comms*: the community partition.
	* *indiv*: the identifier of the community we want to study.
**Returns**
	* the value of the metric for the given community.		

.. code:: Java

	Map<Integer, Double> compute(Graph<U> graph, Communities<U> comms)

This method computes the metric for all the communities in a partition. It returns the value in a map indexed by community identifier.

**Arguments:**
	* *graph*: the network we want to study.
	* *comms*: the community partition.
**Returns**
	* a map, indexed by community identifier, containing the value of the metric for each community.	

.. code:: Java

	double averageValue(Graph<U> graph, Communities<U> comms)

This method computes the average value of the metrics over the set of communities.

**Arguments:**
	* *graph*: the network we want to study.
	* *comms*: the community partition.
**Returns**
	* the average value of the metric.

Global community metrics
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Given a community partition of the network, this family of metrics allows the analysis of the properties of the whole network which consider the community partition of the graph. They inherit from the :java:`CommunityMetric` interface, which has the following methods:	

.. code:: Java

	double compute(Graph<U> graph, Communities<U> comms)

This method finds the value of the structural metric for the given network.

**Arguments:**
	* *graph*: the network we want to study.
	* *comms*: the community partition.
**Returns**
	* the value of the metric.	
