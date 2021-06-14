Friends of friends
============================================
Friends of friends algorithms represent one of the most well-known and commonly used family of link recommendation and prediction algorithms.
They are algorithms which consider the common neighbors between the target and the candidate users to produce the recommendation scores. RELISON
includes the following algorithms:

* `Adamic-Adar`_
* `Cosine similarity`_
* `Hub depressed index`_
* `Hub promoted index`_
* `Jaccard`_
* `Local Leicht-Holme-Newman index`_
* `Most common neighbors`_
* `Resource allocation`_
* `Sørensen`_

Adamic-Adar
~~~~~~~~~~~~
The Adamic-Adar algorithm promotes users with a high number of common friends, but giving more importance to those friends with low degree (as they are more unique to both friendship circles than popular users).

**References:**
    * L.A. Adamic, E. Adar: Friends and neighbors on the Web. Social Networks, 25(3), 211–230 (2003)
    * D. Liben-Nowell and J. Kleinberg.  The link prediction problem for social networks. 12th International Conference on Information and Knowledge Management (CIKM  2003), ACM, 556-559 (2003).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
* :code:`wSel`: the neighborhood selection for the common neighbor.

    * :code:`IN`: it considers the incoming neighborhood of the common neighbor.
    * :code:`OUT`:  it considers the outgoing neighborhood of the common neighbor.
    * :code:`UND`: it considers the all the possible neighbors of the common neighbors (:math:`\Gamma_{out}(w) \cup \Gamma_{in}(w)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the common neighbor (:math:`\Gamma_{out}(w) \cap \Gamma_{in}(w)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Adamic-Adar:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    wSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Cosine similarity
~~~~~~~~~~~~~~~~~~
Also known as the Salton index, this algorithms represents each user as a vector, where each other user in the network is a coordinate. The weights of the edges between the 
original user and the rest of the network are taken as the coordinate values. Then, the score is computed as the cosine between those vectors.

**Reference:** L. Lü,  T. Zhou. Link Prediction in Complex Networks: A survey. Physica A: Statistical Mechanics and its Applications, 390(6), 1150-1170 (2011).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Cosine:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])

Hub depressed index
~~~~~~~~~~~~~~~~~~~~~
Friends of friends approach for favoring the recommendation nodes with smaller degree than the target user.  

**References:** 
    * L. Lü,  T. Zhou, Y. Zhang. Predicting missing links via local information. European Physical Journal B 71, 623-630 (2009).
    * E. Ravasz, A.L. Somera, D.A. Mongru, Z.N. Oltvai, A-L. Barabasi. Hierarchical Organization in Metabolic Networks, Science 297 (2002)

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Hub depressed index:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Hub promoted index
~~~~~~~~~~~~~~~~~~~~~
Friends of friends approach for favoring the recommendation nodes with higher degree than the target user.  

**References:** 
    * L. Lü,  T. Zhou, Y. Zhang. Predicting missing links via local information. European Physical Journal B 71, 623-630 (2009).
    * E. Ravasz, A.L. Somera, D.A. Mongru, Z.N. Oltvai, A-L. Barabasi. Hierarchical Organization in Metabolic Networks, Science 297 (2002)

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Hub promoted index:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Jaccard
~~~~~~~~~~~~~~~~~~~~~
The Jaccard algorithm uses as a recommendation score the probability that any neighbor of the target and candidate user is common to both.

**References:** 
    * P. Jaccard. Etude comparative de la distribution florale dans une portion des Alpes et des Jura. Bulletin de la Societe Vaudoise des Sciences Naturelles 37(142),547–579 (1901)
    * D. Liben-Nowell and J. Kleinberg.  The link prediction problem for social networks. 12th International Conference on Information and Knowledge Management (CIKM  2003), ACM, 556-559 (2003).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Jaccard:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Local Leicht-Holme-Newman index
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This algorithm assigns high similarity to node pairs that have many neighbors in common in comparison to the expected number of common neighbors in a configuration
model.

**Reference:** E.A. Leicht, P. Holme, M.E.J. Newman. Vertex Similarity in Networks. Physical Review E 73(2): 026120 (2006).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Local LHN:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Most common neighbors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The most common neighbors algorithm just takes the number of common neighbors between the target and candidate users as the recommendation score.

**Reference:** D. Liben-Nowell and J. Kleinberg.  The link prediction problem for social networks. 12th International Conference on Information and Knowledge Management (CIKM  2003), ACM, 556-559 (2003).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  MCN:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Resource allocation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Based on the physical resource allocation process, this method finds the amount of a resource that would reach 

**Reference:** L. Lü,  T. Zhou, Y. Zhang. Predicting missing links via local information. European Physical Journal B 71, 623-630 (2009).

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
* :code:`wSel`: the neighborhood selection for the common neighbor.

    * :code:`IN`: it considers the incoming neighborhood of the common neighbor.
    * :code:`OUT`:  it considers the outgoing neighborhood of the common neighbor.
    * :code:`UND`: it considers the all the possible neighbors of the common neighbors (:math:`\Gamma_{out}(w) \cup \Gamma_{in}(w)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the common neighbor (:math:`\Gamma_{out}(w) \cap \Gamma_{in}(w)`)

Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Resource allocation:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    wSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

Sørensen
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This method is based on an statistic index for comparing how similar to samples are (here, how similar the neighbors of two users are).

**References:** 
    * L. Lü,  T. Zhou. Link Prediction in Complex Networks: A survey. Physica A: Statistical Mechanics and its Applications, 390(6), 1150-1170 (2011).
    * T. Sørensen.  A method of establishing groups of equal amplitude in plant sociology based on similarity of species content and its application to analyses of the vegetation on Danish commons. Biologiske Skrifter 5(4), pp. 1-34 (1948)

Parameters
^^^^^^^^^^
* :code:`uSel`: the neighborhood selection for the target user.

    * :code:`IN`: it considers the incoming neighborhood of the target user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the target user.
    * :code:`UND`: it considers the all the possible neighbors of the target users (:math:`\Gamma_{out}(u) \cup \Gamma_{in}(u)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the target user (:math:`\Gamma_{out}(u) \cap \Gamma_{in}(u)`)
* :code:`vSel`: the neighborhood selection for the candidate user.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
    
Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Sorensen:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
