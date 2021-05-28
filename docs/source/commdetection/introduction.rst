Introduction
============
In social network environments, it is common to group users. A common way to do this is community detection. Community detection finds set of users tightly connected to each other, but sparsely connected to the rest of the network. In the SoNALiRe framework, we provide a set of community detection algorithms, which can be used for this. We provide a program to easily compute this community partitions, and also, functionality for developing new approaches, or integrating the included ones in Java libraries.

Community detection algorithms are included in the SNA module of the framework, which can be imported into any Java library using Maven as follows:

.. code:: xml

    <dependency>
      <groupId>es.uam.eps.ir</groupId>
      <artifactId>SocialRankSys-sna</artifactId>
      <version>1.0.0</version>
    </dependency>

This section is divided as follows:
 
.. toctree::
   :maxdepth: 1
   :caption: Community detection

   config
   code
   algorithms