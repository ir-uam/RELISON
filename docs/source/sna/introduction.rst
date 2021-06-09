Social network analysis
=======================

RELISON provides functionality for analyzing the structural properties of the social network graphs: vertex, links, communities and graph properties can be easily computed thanks to our framework. We provide several ways to determine the topological properties of networks: with a program which straightforwardly allows you to compute any of the included social network properties in the framework by reading a configuration file, and by integrating the framework in your own library.

In this section, we provide some guide on how to do this. 

The social network metrics are included in the SNA module of the framework, which can be imported into any Java library using Maven as follows:

.. code:: xml

    <dependency>
      <groupId>es.uam.eps.ir</groupId>
      <artifactId>Relison-sna</artifactId>
      <version>1.0.0</version>
    </dependency>