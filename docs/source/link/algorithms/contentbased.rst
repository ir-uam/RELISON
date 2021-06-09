Content-based algorithms
============================================
This family of people recommendation algorithms considers feature information about the users (user-generated contents, communities, etc. to
produce the recommendations).

* `Centroid CB`_
* `Twittomender`_

Centroid CB
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Content-based recommendation algorithm, based on a tf-idf scheme. Each piece of content published by a single user
is consdered as its content. Then, using some of these contents, a centroid using tf-idf weights is computed for
each user. The recommendation score of a link is just the cosine similarity between the centroids of two separated
users.

**Reference:** J. Sanz-Cruzado, P. Castells. Enhancing Structural Diversity in Social Networks by Recommending Weak Ties. 12th ACM Conference on Recommender Systems (RecSys 2018),  233-241 (2018).

Parameters
^^^^^^^^^^
* :code:`index`: a directory containing a Lucene index. In order to obtain it, it is necessary to execute the :code:`CBIndexGenerator` program over the set of contents of the users. This index stores the whole set of user-generated contents (each content is considered a separate document), and each piece is identified by its author. 
* :code:`orientation`: (*OPTIONAL*) if this parameter is not available, we generate the centroid using just the set of pieces created by the user. If we include this parameter, we create it by using the pieces of her neighbors (selected according to this orientation value). It can take the possible values:
    * :code:`IN`: for using the contents of the incoming neighbors.
    * :code:`OUT`: for using the contents of the outgoing neighbors.
    * :code:`UND`: for using the contents of both the incoming and outgoing neighbors.
    * :code:`MUTUAL`: for using the contents of those networks who share a reciprocal link with the user.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  centroidCB:
    index:
      type: String
      values: [file1,file2,...,fileN]
    (orientation:
      type: orientation
      values: [IN,OUT,UND,MUTUAL])


Twittomender
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Content-based recommendation algorithm, based on a tf-idf scheme. Each user in the network is represented by the concatenation
of a set of user-generated contents, stored in an index. Then, the algorithm represents the user as a tf-idf vector, and finds
the recommendation score by computing the cosine similarity between the vectors of the target and candidate users.

**Reference:** J. Hannon, M. Bennet, B. Smyth. Recommending Twitter Users to Follow Using Content and Collaborative Filtering Approaches. 4th Annual International ACM Conference on Recommender Systems (RecSys 2010), 199-206 (2010).

Parameters
^^^^^^^^^^
* :code:`index`: a directory containing a Lucene index. In order to obtain it, it is necessary to execute the :code:`TwittomenderIndexGenerator` program over the set of contents of the users. This index stores each user as a document.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

    Twittomender:
      index:
        type: String
        values: [file1,...,fileN]