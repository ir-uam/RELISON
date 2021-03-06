Networks
=========
The RELISON library allows the creation, reading, writing and manipulation of networks. Mathematically, a network is modelled as a graph, 
:math:`G = \langle \mathcal{U}, E \rangle`, where :math:`\mathcal{U}` is the set of users in the network, and :math:`E \in \mathcal{U}^2` is the set of links in the network. For each user :math:`u \in \mathcal{U}`, we define his neighborhood, :math:`\Gamma(u)` as the set of people in the network sharing a link with him.

In order to use the basic graph structures, the following package must be included in the pom.xml file in your Maven project.

.. code:: xml

    <dependency>
      <groupId>es.uam.eps.ir</groupId>
      <artifactId>RELISON-core</artifactId>
      <version>1.0.0</version>
    </dependency>

Properties of a graph
======================
The RELISON library allows different types of network, depending on its properties. We have to select three options:

**Number of edges between users**

The first option to consider in a network is the number of edges we allow between the same pair of users. If we consider many of them, we are talking about *multigraphs*, whereas, if we just consider one, we are using a *simple* graph.

**Direction of the edges**

The second consideration defines whether all the edges are reciprocal or not. If they are, there is no difference between the :code:`(u,v)` and :code:`(v,u)`, and we are talking about an *undirected* network. This is the case of the Facebook friendship network, where a link has to be accepted by both users before it is created. In case the direction of the edges matters, we are talking about a *directed* network, as the Twitter follows network.

**Direction of the edges**

The third and final consideration is referred to the properties of the edges. Sometimes, it is possible to define a function :math:`w`, which assigns a weight to each of the edges in the network. This weight might define very different properties: the frequency of interaction between the users, the cost of travelling from one node to another, etc. If the network considers these weights, we consider it a *weighted* network, while an *unweighed* network just takes binary weights (weight equal to 1 if the edge exists, 0 otherwise).

In the table below, we include the basic interfaces for defining any of those graphs, depending on the properties of the network.

.. list-table:: Basic classes for the definition of network graphs

	* - Multigraph
	  - Directed
	  - Weighted
	  - Class
	* - 
	  - 
	  - 
	  - UndirectedUnweightedGraph
	* - 
	  -
	  - ???
	  - UndirectedWeightedGraph
	* -
	  - ???
	  - 
	  - DirectedUnweightedGraph
	* -
	  - ???
	  - ???
	  - DirectedWeightedGraph
	* - ???
	  - 
	  - 
	  - UndirectedUnweightedMultiGraph
	* - ???
	  -
	  - ???
	  - UndirectedWeightedMultiGraph
	* - ???
	  - ???
	  - 
	  - DirectedUnweightedMultiGraph
	* - ???
	  - ???
	  - ???
	  - DirectedWeightedMultiGraph

Creation of empty graphs
========================
When integrated in another Java library, the RELISON library provides two options for generating empty graphs.

The first option considers the straightforward use of the class constructors. RELISON provides an efficient implementation for each of the network types described above. The obtain this implementation, it is just necessary to add the prefix :code:`Fast` to the interface names 
described earlier. No additional argument needs to be provided to the constructors. For instance, if we want to create a directed unweighted and simple graph, we would just create it as:

.. code:: Java

	Graph<U> graph = new FastDirectedUnweightedGraph();

The second option uses graph generators: the :code:`EmptyGraphGenerator` class builds empty simple graphs, whereas the :code:`EmptyMultiGraphGenerator` class does the same for multigraphs. The constructor has to be configured with two parameters, indicating whether the graph is directed and/or weighted. Following the previous example, we would do the following:

.. code:: Java

	Graph<U> graph;
	boolean directed = true;
	boolean weighted = false;

	GraphGenerator<U> ggen = new EmptyGraphGenerator();
	ggen.configure(directed, weighted);
	graph = ggen.generate();

Reading / writing graphs
=========================
Most times, the networks which shall be used in the library are stored in an external file. RELISON provides classes for reading this type of networks. When we want to read the network, we use a :code:`GraphReader`. This works as follows:

.. code:: Java

	String file = "file.txt"; // the route of the file
	boolean readWeights = true; // if we want to read the weights.
	boolean readTypes = false; // if we want to read the types
	GraphReader<U> greader; // we assume here that it has been configured
	Graph<U> graph = greader.read(file, readWeights, readTypes);

To write a graph into a file, we use a :code:`GraphWriter`. Once it is created, this works as follows:

.. code:: Java

	String file = "file.txt"; // the route of the file
	Graph<U> graph; // the graph to write
	GraphWriter<U> gwriter; // the graph we want to write
	boolean writeWeights = true; // if we want to write the edge weights.
	boolean writeTypes = false; // if we want to write the edge types
	gwriter.write(graph, file, writeWeights, writeTypes);

Basic format
~~~~~~~~~~~~
The basic format prints, on each line, an edge of the network. The format of a line is the following (separated by a delimiter):

.. code:: 

	node1 node2 (weight edgeType)

where :code:`node1` and :code:`node2` are the identifiers of the nodes, :code:`weight` is the weight value (a double value) and :code:`type` contains an integer value classifying the edge. The last values are optional. The weight should only be provided when we want to read the weights of the network, and the types must be provided when the user wants to read them.

 For instance, let's suppose that we have a Twitter network with 10,000 users and 100,000 edges. The first two users have, as nicknames, "JavierSanzCruza" and "pcastells", respectively, and the first follows the second. Then, if we use "," as delimiters the file would look as:

.. code::

	JavierSanzCruza,pcastells,1.0
	<...>

If we want to read graphs from this format, we use the :code:`TextGraphReader` and :code:`TextMultiGraphReader` classes, depending on whether we want to read a simple network or a multigraph. These classes receive the following arguments:

* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if the network is weighted, false otherwise.
* :code:`selfloops`: true if we want to read edges from a node to itself, false otherwise.
* :code:`delimiter`: the delimiter separating the different fields in the file. In the main programs of RELISON, it is a tab.
* :code:`uParser`: a parser for reading the type of the nodes from text.

When we want to write graphs into this format, we use the :code:`TextGraphWriter` class, which just receives the delimiter in the constructor.

We wanted to note here that this is the format available in the programs provided by the RELISON library, with the fields separated by tabs (the :code:`\t` character).

Pajek format
~~~~~~~~~~~~
This format allows reading and writing networks in the Pajek format (more information in the following `link <https://gephi.org/users/supported-graph-formats/pajek-net-format/>`_ ). These graphs have the following format (space separated):

.. code::

	*Vertices numVertices
	vertexId1 "vertexLabel1"
	vertexId2 "vertexLabel2"
	<...>
	vertexIdN "vertexLabelN"
	*Edges numEdges
	vertexId1.1. vertexId1.2. weight1
	<...>

Here, for each node, we differentiate two value: the :code:`vertexId` is a numerical value identifying the user, and the :code:`vertexLabel` is the actual identifier of the user. For instance, let's suppose that we have a Twitter network with 10,000 users and 100,000 edges. The first two users have, as nicknames, "JavierSanzCruza" and "pcastells", respectively, and the first follows the second. Then, the Pajek file would be the following:
.. code::

	*Vertices 10000
	1 "JavierSanzCruza"
	2 "pcastells"
	<...>
	*Edges 100000
	1 2 1.0
	<...>

If we want to read graphs from this format, we use the :code:`PajekGraphReader` class. This class receives the following arguments:

* :code:`multigraph`: true if the network is modelled after a multigraph, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if the network is weighted, false otherwise.
* :code:`selfloops`: true if we want to read edges from a node to itself, false otherwise.
* :code:`uParser`: a parser for reading the type of the nodes from text.

When we want to write graphs into this format, we use the :code:`PajekGraphWriter` class, which does not receive any argument in its constructor.

Differently from the basic format, this one does not allow reading the types of the edges.


Graph manipulation
==================

The RELISON library provides methods for the manipulation of the network (adding nodes, edges, changing the weights of edges, etc.). All this methods are provided in the :code:`Graph` interface, but we summarize them here.

Nodes
~~~~~~~~~~~~~~~~

The simplest way to modify a network is to add or remove one of its edges.

Add nodes
^^^^^^^^^^

If we want to add a node to the network, we use the following method:

.. code:: Java
	
	boolean addNode(U user)

**Arguments:**
	* *user*: the identifier of the user.
**Returns**
	* If the node is added, it returns true. Otherwise, it returns false. A user can only be added once, so, if a node is added twice, the second time, this method will return false.

Remove nodes
^^^^^^^^^^^^

If we want to remove a node from the network, we use:


.. code:: Java
	
	boolean removeNode(U user)

**Arguments:**
	* *user*: the identifier of the user.
**Returns**
	* If the node is removed, it returns true. Otherwise, it returns false. If the user does not exist in the network, this method will return false.


Edges
~~~~~~~~~~~~~~~~

The second group of elements that we can modify in a network is the group of edges in the network. In this case, we have several methods of interest.

Add edges
^^^^^^^^^

To add edges, we can consider several options. We include here the most complete one, although more of them can be seen on the reference of the :code:`Graph` interface, `here <https://ir-uam.github.io/RELISON/javadoc/es/uam/eps/ir/relison/graph/Graph.html>`_.

.. code:: Java
	
	boolean addEdge(U orig, U dest, double weight, int type, boolean insertNodes)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`weight`: the weight of the edge.
	* :code:`type`: the type of the edge.
	* :code:`insertNodes`: true if we want to add the nodes to the network if they do not exist, false otherwise.
**Returns**
	* If the edge is added, it returns true. Otherwise, it returns false. In simple networks, an edge can only be added once.

Update edge weights
^^^^^^^^^^^^^^^^^^^

If we want to modify the weight of an edge, we have to use the following methods. In simple networks, we have to use:

.. code:: Java
	
	boolean updateEdgeWeight(U orig, U dest, double newWeight)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`newWeight`: the new weight of the edge.
**Returns**
	* If the edge weight is updated, it returns true. If the edge does not exist, it cannot be updated.

In multigraphs, we use the following method instead (the previous one just updates the first created edge between the users):

.. code:: Java
	
	boolean updateEdgeWeight(U orig, U dest, double newWeight, int idx)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`newWeight`: the new weight of the edge.
	* :code:`idx`: the number of the edge between the users to modify.
**Returns**
	* If the edge weight is updated, it returns true. If the edge does not exist, it cannot be updated.

Update edge types
^^^^^^^^^^^^^^^^^^^

If we want to modify the type of an edge, we have to use the following methods. In simple networks, we have to use:

.. code:: Java
	
	boolean updateEdgeType(U orig, U dest, int newType)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`newType`: the new type of the edge.
**Returns**
	* If the edge weight is updated, it returns true. If the edge does not exist, it cannot be updated.

In multigraphs, we use the following method instead (the previous one just updates the first created edge between the users):

.. code:: Java
	
	boolean updateEdgeType(U orig, U dest, double newType, int idx)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`newType`: the new type of the edge.
	* :code:`idx`: the number of the edge between the users to modify.
**Returns**
	* If the edge type is updated, it returns true. If the edge does not exist, it cannot be updated.

Remove edges
^^^^^^^^^^^^^^^^^^^

Finally, if we want to remove an edge, we have to use the following method:

.. code:: Java
	
	boolean removeEdge(U orig, U dest)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
**Returns**
	* If the edge weight is remove, it returns true. If the edge does not exist, it cannot be removed.

In multigraphs, we use the following method instead (the previous one just removes the first created edge between the users):

.. code:: Java
	
	boolean removeEdge(U orig, U dest, int idx)

**Arguments:**
	* :code:`orig`: the first node of the edge.
	* :code:`dest`: the second node of the edge.
	* :code:`idx`: the number of the edge between the users to remove.
**Returns**
	* If the edge type is updated, it returns true. If the edge does not exist, it cannot be updated.

Also, we provide a method to remove all the edges between two nodes in the multigraph:

.. code:: Java
	
	boolean removeEdges(U orig, U dest)

**Arguments:**
	* :code:`orig`: the first node of the edges.
	* :code:`dest`: the second node of the edges.
**Returns**
	* If the edge weight is remove, it returns true. If there are not edges between the users, they cannot be removed.

Accessing the properties of a network
======================================
In addition to the methods for modifying the structure of a social network, RELISON also provides methods for accessing to the basic information of a network: the nodes in the network, his neighbors, the weights and types of the edges, etc. Here, we summarize how this can be done. 

Users in the network
~~~~~~~~~~~~~~~~~~~~~~
The first element we explain how to access are the nodes in the network. In order to access the complete set of nodes, we can use the following method:

.. code:: Java
	
	Stream<U> getAllNodes()

**Returns**
	* An stream object containing the identifiers of all the nodes in the network.

If, instead of accessing to them, we just need to count them, we can use the following method of the graphs:

.. code:: Java
	
	long getVertexCount()

**Returns**
	* The number of vertices (nodes, users) in the network graph.

Edge properties
~~~~~~~~~~~~~~~
The next element we can access are the edges. If we know the endpoints of the edge, we can access their different properties. We differentiate four methods here:

Edge existence
^^^^^^^^^^^^^^
The first method allows us to know if there is a link between two users. The method signature is:

.. code:: Java
	
	boolean containsEdge(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* true if there is (at least) an edge, false otherwise.

In multigraphs, there is another method which allows us to obtain the number of edges between a pair of users:

.. code:: Java
	
	int getNumEdges(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* the number of edges between the two nodes.

Edge weight
^^^^^^^^^^^^
We might want to access the weight of an edge. For this, in simple graphs, we use the following method:

.. code:: Java

	double getEdgeWeight(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* the weight if it exists, :code:`NaN` otherwise.

In the case of multigraphs, the previous method allows us to obtain the sum of all the edge weights. If we want to obtain the individual weights, we have another method:

.. code:: Java

	List<Double> getEdgeWeights(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* a list containing all the edge weights if there is (at least) one edge between the users, null otherwise.

Edge types
^^^^^^^^^^^^
We might want to access the type of an edge. For this, in simple graphs, we use the following method:

.. code:: Java

	int getEdgeWeight(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* the type if it exists, -1 otherwise.

In the case of multigraphs, we have another method, which allows us to retrieve the weights of all the edges:

.. code:: Java

	List<Integer> getEdgeTypes(U orig, U dest)

**Arguments**
	* :code:`orig`: the first node.
	* :code:`dest`: the second node.

**Returns**
	* a list containing all the edge types if there is (at least) one edge between the users, null otherwise.

Edge number
^^^^^^^^^^^
The last method just allows us to determine how many edges our network has:

.. code:: Java

	long getEdgeCount()

**Returns**
	* the number of edges in the network.

Neighbors of a node
~~~~~~~~~~~~~~~~~~~
As accessing the edges in the network just by running over all the possible pairs of users and checking if the edge exists would be very slow, RELISON provides methods for accessing the neighborhood of a user. We explain them here.

Existence of neighbors
^^^^^^^^^^^^^^^^^^^^^^
The first group of methods study whether a node has (or not) neighbors in the network. Although there are several methods for this, here we only explain the following one:

.. code:: Java

	boolean hasNeighbors(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for identifying if the user has incoming neighbors.
	    * :code:`OUT`: for identifying if the user has outgoing neighbors.
	    * :code:`UND`: for identifying if the user has either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for identifying if the user has neighbors who are both incoming and outgoing.

**Returns**
	* true if the node has neighbors, false otherwise.

Count
^^^^^^^^^^
The second group of methods allows us to identify how many neighbors a user has. 

.. code:: Java

	int getNeighbourhoodSize(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* the number of neighbors of the user.

There is another method, that allows us to study the number of edges connected to a node. This value is the same as the previous one in the case of simple networks, but it changes when it comes to multigraphs:

.. code:: Java

	int degree(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* the number of edges connected to the user (the degree), -1 if the user does not exist.


Neighbors
^^^^^^^^^^
The third group of methods allows us to identify the actual neighbors of a user in the network. The method we introduce here is the following:

.. code:: Java

	Stream<U> get(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* an stream containing the identifier of the networks.


Weights
^^^^^^^^
The fourth group of methods allows us to identify the weights of the nodes. We can use the following method:

.. code:: Java

	Stream<Weight<U, Double>> getNeighbourhoodWeights(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* an stream containing (user, weight) pairs. In multigraphs, a pair is included for each edge.

Additionally, in multigraphs, we can also use the following method:

.. code:: Java

	Stream<Weight<U, Double>> getNeighbourhoodWeightsLists(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* an stream containing (user, list of weights) pairs.

Edge types
^^^^^^^^^^
The fifth and last group of methods allows us to identify the weights of the nodes. We can use the following method:

.. code:: Java

	Stream<Weight<U, Integer>> getNeighbourhoodTypes(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* an stream containing (user, type) pairs. In multigraphs, a pair is included for each edge.

Additionally, in multigraphs, we can also use the following method:

.. code:: Java

	Stream<Weight<U, Integer>> getNeighbourhoodTypesLists(U u, EdgeOrientation orient)

**Arguments**
	* :code:`u`: the node to study.
	* :code:`orient`: the orientation for selecting the neighbors (only useful in directed networks).

	    * :code:`IN`: for retrieving the incoming neighbors.
	    * :code:`OUT`: for retrieving the outgoing neighbors.
	    * :code:`UND`: for retrieving either incoming or outgoing neighbors.
	    * :code:`MUTUAL`: for retrieving the neighbors who are both incoming and outgoing.

**Returns**
	* an stream containing (user, list of types) pairs.

Adjacency matrix
~~~~~~~~~~~~~~~~
In addition to the graph object, we can represent the network using the adjacency matrix of the network. This matrix has in the :math:`(u,v)` coordinate, the weight of the edge between nodes :math:`u` and :math:`v`. We can obtain this matrix using the following method:

.. code:: Java

	double[][] getAdjacencyMatrix(EdgeOrientation orient)

**Arguments**
	* :code:`orient`: the orientation for selecting the matrix (only useful in directed networks).

	    * :code:`IN`: the :math:`(u,v)` coordinate contains the weight of the :math:`(v,u)` weight.
	    * :code:`OUT`: the :math:`(u,v)` coordinate contains the weight of the :math:`(u,v)` weight (natural adjacency matrix)
	    * :code:`UND`: the :math:`(u,v)` coordinate contains the :math:`w(u,v) + w(v,u)`.
	    * :code:`MUTUAL`: the :math:`(u,v)` coordinate contains the :math:`w(u,v) + w(v,u)` value if both edges exist, 0 otherwise.

**Returns**
	* an array containing the matrix.

We should note that the nodes in the network can be represented by any type of identifier (int, long, string, etc.). So, in order to map these identifiers to the indexes in the matrix, we can use the following method:

.. code:: Java

	Index<U> getAdjacencyMatrixMap()

**Returns**
	* an index, mapping user identifiers to indexes in the (0, numNodes-1) rank.