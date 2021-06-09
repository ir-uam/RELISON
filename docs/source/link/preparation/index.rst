Content index generator
=================================================
Some algorithms and metrics use, as input, an index containing the information about the user-generated contents associated to each user. In order to generate these indexes, we provide two programs, taking advantage of the Lucene library (https://lucene.apache.org/). We detail here how to execute these programs.

User index generator
~~~~~~~~~~~~~~~~~~~~~~
This is the Lucene index used by the Twittomender algorithm (and the novelty and diversity metrics). Each user has associated a single document, built by concatenating the information pieces published either by the user, or by her neighbors. In order to build this index, we have to execute the following program:

.. code:: bash

  java -jar RELISON.jar index user graph multigraph directed weighted selfloops information-pieces header orientation index-route

where

* :code:`graph`: a file containing the social network.
* :code:`multigraph`: true if the network allows multiple edges between each pair of users, false otherwise.
* :code:`directed`: true if the network is directed, false otherwise.
* :code:`weighted`: true if we want to use the weights of the links, false otherwise (weights will be binary).
* :code:`selfloops`: true if we allow links between a node and itself, false otherwise.
* :code:`information-pieces`: a file containing the information pieces (See `Information pieces file`_ below).
* :code:`header`: true if the file contains a header, false otherwise.
* :code:`orientation`:
  * :code:`own`: uses the pieces created by each user as their representation.
  * :code:`IN`: uses the pieces created by the incoming neighbors of the user as her representation.
  * :code:`OUT`: uses the pieces created by the outgoing neighbors of the user as her representation.
  * :code:`UND`: uses the pieces created by both the outgoing and incoming neighbors of the user as her representation.
  * :code:`MUTUAL`: uses the pieces created by the mutual neighbors of the user as her representation.
* :code:`output`: a directory in which to store the index.

Information pieces index generator
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This is the Lucene index used by the Centroid CB algorithm. An user-generated content is related to each document in the index. A relation between
information pieces and creators is also stored.

.. code:: bash

  java -jar RELISON.jar index infopiece graph multigraph directed weighted selfloops information-pieces header orientation index-route

where

* :code:`information-pieces`: a file containing the information pieces (See `Information pieces file`_ below).
* :code:`header`: true if the file contains a header, false otherwise.
* :code:`output`: a directory in which to store the index.

Information pieces file
~~~~~~~~~~~~~~~~~~~~~~~~
The information pieces (individual user-generated contents) file needs to have the following format (CSV divided by tabs):

.. code:: txt

    infoId  userId  text  reprCount  likeCount  created  truncated

where

* :code:`infoId`: identifier of the information piece.
* :code:`userId`: identifier of the creator.
* :code:`text`: the content of the information piece.
* :code:`reprCount`: number of times the piece has been repropagated.
* :code:`likeCount`: number of likes the piece has been received.
* :code:`created`: UNIX timestamp indicating the date of creation.
* :code:`truncated`: whether we are taking the complete text, or just a small part.

The text must be in UTF-8 format, and user-generated contents are separated by line skips. Fields (like text) which might have tabs or line skips inside must be properly escaped, and surrounded by "".