Graph generation algorithms
============================
The RELISON framework contains the following graph generators:

* `Complete`_
* `Empty`_
* `No links`_
* `Random`_
* `Preferential attachment`_
* `Watts-Strogatz`_

All the graph generators are configured in the following way:

.. code:: Java

    GraphGenerator<U> gen = new GraphGenerator<>() // Substitute here the corresponding generator
    gen.configure(arg1,arg2,...,argN)

Then, the graphs are generated with:

.. code:: Java

    Graph<U> graph = gen.generate()

Complete
~~~~~~~~
The complete graph generator creates a network where every pair of nodes is connected. It is defined in the
:code:`CompleteGraphGenerator` class.

Arguments
^^^^^^^^^^
The arguments for configuring a complete graph are:

* :code:`directed`: if the graph to generate is directed.
* :code:`numNodes`: the number of nodes in the network.
* :code:`selfloops`: true if we also add links between a node and itself.
* :code:`generator`: a node generator.

Empty
~~~~~~
The empty graph generator creates a network without links. It is defined in the
:code:`EmptyGraphGenerator` class (and the :code:`EmptyMultiGraphGenerator` for multigraphs). 

Arguments
^^^^^^^^^^
The arguments for configuring an empty graph are:

* :code:`directed`: if the graph to generate is directed.
* :code:`weighted`: the number of nodes in the network.

No links
~~~~~~~~
The empty graph generator creates a network without links. It is defined in the
:code:`NoLinkGraphGenerator` class. 

Arguments
^^^^^^^^^^
The arguments for configuring a graph without links are:

* :code:`directed`: if the graph to generate is directed.
* :code:`numNodes`: the number of nodes in the network.
* :code:`generator`: a node generator.

Preferential attachment
~~~~~~~~~~~~~~~~~~~~~~~~
This generator starts with a fully connected set of nodes. Then, iteratively, a new node appears in the network, and creates
a fixed amount of links to the nodes already in the network. The link destinations are selected with a probability proportional
to their in-degree.

**Reference:** A-L. Barabási, R. Albert. Emergence of Scaling in Random Networks. Science 286(5439), pp. 509-512 (1999)

Arguments
^^^^^^^^^^
The arguments for configuring a preferential attachment network are:

* :code:`directed`: true if the graph we want to generate is directed, false otherwise.
* :code:`initialNodes`: the number of initial nodes.
* :code:`numIter`: the number of iterations, i.e. the number of additional nodes to add.
* :code:`numEdges`: the number of edges to add each iteration.
* :code:`generator`: a node generator.

Random
~~~~~~
This generator creates a network where the nodes are connected with a certain probability.

**Reference:** P. Erdös, A. Rényi. On Random Graphs. I, Publicationes Mathematicae Debrecen 6(1), pp. 290-297 (1959)

Arguments
^^^^^^^^^^
The arguments for configuring a random Erdös-Renyi network are:

* :code:`directed`: true if the graph we want to generate is directed, false otherwise.
* :code:`numNodes`: the number of nodes in the network.
* :code:`prob`: the probability that two nodes are joined with a link.
* :code:`generator`: a node generator.

Watts-Strogatz
~~~~~~~~~~~~~~
This generator first creates a ringed network, where each user is connected to other n nodes. Then, with certain probability, each 
link is randomly reconnected to other node.

**Reference:** D.J. Watts, S.H. Strogatz. Collective dynamics of 'small-world' networks. Nature 393(6684), pp. 440-442 (1998)

Arguments
^^^^^^^^^^
The arguments for configuring a random Erdös-Renyi network are:

* :code:`directed`: true if the graph we want to generate is directed, false otherwise.
* :code:`numNodes`: the number of nodes in the network.
* :code:`meanDegree`: the average degree of each node in the initial network (only int values).
* :code:`beta`: the probability of rewiring an edge.
* :code:`generator`: a node generator.