Link prediction and recommendation
==================================

Networks are not static objects: they are evolving over time. One of the main elements that evolves over time is the set of links in the network. Link prediction and link recommendation have studied the creation of new edges in the network. Link prediction just tries to identify which of them would form naturally in the network in the future, whereas link recommendation (also known as contact or people recommendation in the network) has a more active role, by suggesting users to befriend in networks. In the end, they are both similar tasks.

In this framework, we provide functionalities and metrics for both tasks, which we define in this section.

In order to use these functionalities, it is important to import the following package using Maven:

.. code:: xml

    <dependency>
      <groupId>es.uam.eps.ir</groupId>
      <artifactId>RELISON-linkpred</artifactId>
      <version>1.0.0</version>
    </dependency>
