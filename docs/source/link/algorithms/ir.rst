Information retrieval
============================================
This group of approaches includes multiple implementations of information retrieval models, initially devised for searching text documents
in massive information spaces. All the algorithms included here are collaborative filtering approaches (they do not use any feature information,
just the connections in the network). Details about how these models were adapted and their formulations for contact recommendation can be found in the following
references:

**References:**
	* J. Sanz-Cruzado, P. Castells, C. Macdonald, I. Ounis. Effective Contact Recommendation in Social Networks by Adaptation of Information Retrieval Models. Information Processing & Management 57(5) (2020).
	* J. Sanz-Cruzado, C. Macdonald, I. Ounis, P. Castells. Axiomatic Analysis of Contact Recommendation Methods in Social Networks: an IR perspective. 42th European Conference on Information Retrieval (ECIR 2020), 175-90 (2020).

The IR models included in this framework are:

* `Binary independent retrieval`_
* `BM25`_
* `DFRee`_
* `DFReeKLIM`_
* `DLH`_
* `DPH`_
* `Extreme BM25`_
* `Pivoted normalization vector space model`_
* `PL2`_
* `Query likelihood`_
* `Vector space model`_

Binary independent retrieval
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Adaptation of the binary independent retrieval (BIR) model.

**Reference:** K. Sparck Jones, S. Walker, S.E. Robertson. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments. Information Processing and Management 36, 779-808 (part 1), 809-840 (part 2) (2000).

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

  BIR:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]

BM25
~~~~~
Adaptation of the BM25 probabilistic information retrieval model.

**References:** 
    * K. Sparck Jones, S. Walker, S.E. Robertson. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments. Information Processing and Management 36, 779-808 (part 1), 809-840 (part 2) (2000).
    * S.E. Robertson, H. Zaragoza. The Probabilistic Relevance Framework: BM25 and Beyond. Foundations and Trends in Information Retrieval 3, 333–389 (2009).

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
* :code:`dlSel`: the neighborhood selection for computing the document length.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
* :code:`b`: parameter for tuning the effect of the neighborhood size. It takes values between 0 and 1.
* :code:`k`: parameter for tuning the effect of the term frequency in the model. It takes positive values.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  BM25:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    dlSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    b:
      type: double
      range:
        - start: 0.1
          end: 0.99
          step: 0.1
    k:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

DFRee
~~~~~
Adaptation of a parameter-free divergence from randomness model using the average of tw  information measures.

**Reference:** G. Amati, G. Amodeo, M. Bianchi, G. Marcone, F.U. Bordoni, C. Gaibisso, G. Gambosi, A. Celi, C.D. Nicola, M. Flammini.
FUB, IASI-CNR, UNIVAQ at TREC 2011 Microblog Track. 20th Text REtrieval Confer-ence (TREC 2011) (2011).

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

  DFRee:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])

DFReeKLIM
~~~~~~~~~~~
Adaptation of a parameter-free divergence from randomness model using the product of two Kullback-Leibler information measures.

**Reference:** G. Amati, G. Amodeo, M. Bianchi, G. Marcone, F.U. Bordoni, C. Gaibisso, G. Gambosi, A. Celi, C.D. Nicola, M. Flammini.
FUB, IASI-CNR, UNIVAQ at TREC 2011 Microblog Track. 20th Text REtrieval Confer-ence (TREC 2011) (2011).

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

  DFReeKLIM:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])

DLH
~~~~~~~~~~~
Adaptation of a parameter-free divergence from randomness model which considers a hypergeometric distribution as a divergence measure, and Laplace normalization.

**Reference:** G. Amati. Frequentist and Bayesian Approach to Information Retrieval. In: Proceedings of the 28th European Conference on Information Retrieval (ECIR 2006), 13–24 (2006).

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

  DLH:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])      

DPH
~~~~~~~~~~~
Adaptation of a parameter-free divergence from randomness model which considers a hypergeometric distribution as a divergence measure, and Laplace normalization.

**Reference:** G. Amati, E. Ambrosi, M. Bianchi, C. Gaibisso, G. Gambosi: FUB, IASI-CNR and University of Tor Vergata at TREC 2007 Blog Track. 16th Text REtrieval Conference (TREC 2007) (2007)

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

  DPH:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])

Extreme BM25
~~~~~~~~~~~~~
A version of the `BM25`_ algorithm, when parameter :code:`k` tends to infinity.

**References:** 
    * J. Sanz-Cruzado, P. Castells, C. Macdonald, I. Ounis. Effective Contact Recommendation in Social Networks by Adaptation of Information Retrieval Models. Information Processing & Management 57(5) (2020).
    * J. Sanz-Cruzado, C. Macdonald, I. Ounis, P. Castells. Axiomatic Analysis of Contact Recommendation Methods in Social Networks: an IR perspective. 42th European Conference on Information Retrieval (ECIR 2020), 175-90 (2020).

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
* :code:`dlSel`: the neighborhood selection for computing the document length.

    * :code:`IN`: it considers the incoming neighborhood of the candidate user.
    * :code:`OUT`:  it considers the outgoing neighborhood of the candidate user.
    * :code:`UND`: it considers the all the possible neighbors of the candidate users (:math:`\Gamma_{out}(v) \cup \Gamma_{in}(v)`)
    * :code:`MUTUAL`: it considers as neighbors those who share a reciprocal link with the candidate user (:math:`\Gamma_{out}(v) \cap \Gamma_{in}(v)`)
* :code:`b`: parameter for tuning the effect of the neighborhood size. It takes values between 0 and 1.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  EBM25:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    dlSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    b:
      type: double
      range:
        - start: 0.1
          end: 0.99
          step: 0.1
    k:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

Pivoted normalization vector space model
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Adaptation of the vector space model information retrieval model with pivoted normalization.

**Reference:** A. Singhal, J. Choi, D. Hindle, D.D. Lewis, F.C.N. Pereira: AT and T at TREC-7. 7th Text Retrieval Conference (TREC 1998), 186-198 (1998)
    
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
* :code:`s`: parameter for tuning the importance of the candidate user length.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  Pivoted normalization VSM:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    s:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

PL2
~~~~~~~~~~~~~
Adaptation of a divergence from randomness model, where the distribution of terms in the document and the collection is measured using a Poisson distribution,
a Laplace aftereffect estimation is used as a first normalization, and, term frequency is normalized using Normalisation 2.

**References:** 
    * G. Amati, C.J. Van Rijsbergen. Probabilistic Models of Information Retrieval Based on Measuring the Divergence from Randomness. ACM Transactions on Information Systems 20(4), 357–389 (2002).
    * G. Amati. Probability Information Models for Retrieval based on Divergence from Randomness. Ph.D. thesis. University of Glasgow. (2003).
    
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
* :code:`c`: parameter for tuning the importance of the candidate user length.
* :code:`weighted`: (*OPTIONAL*) true to use the weights of the edges, false to consider them binary.


Configuration file
^^^^^^^^^^^^^^^^^^

.. code:: yaml

  PL2:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    c:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

Query likelihood
~~~~~~~~~~~~~~~~~
Adaptation of a language model algorithm known as query likelihood. We differentiate three variants, depending on the applied smoothing:

* Dirichlet smoothing (QLD)
* Jelinek-Mercer smoothing (QLJM)
* Laplace additive smoothing (QLL)

**References:** J.M. Ponte, W.B. Croft. A language modeling approach to information retrieval. 21st Annual International ACM SIGIR Conference on Research and Development in Information Retrieval (SIGIR 1998), 275-281 (1998)
    
Parameters
^^^^^^^^^^
The general parameters are the following:

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

Then, each variant has its own parameters. For the **QLD** version:

* :code:`mu`: parameter controlling the trade-off between the regularization term and the original term. It takes values greater than 0.

for the **QLJM** version:

* :code:`lambda`: parameter controlling the trade-off between the regularization term and the original term. It takes values between 0 and 1.

and, for the **QLL** version:

* :code:`phi`: parameter controlling the trade-off between the regularization term and the original term. It takes values greater than 0.


Configuration file
^^^^^^^^^^^^^^^^^^

The configuration file for the query likelihood algorithm with Dirichlet smoothing is:

.. code:: yaml

  QLD:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    mu:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

The configuration file for the query likelihood model with Jelinek-Mercer smoothing is:

.. code:: yaml

  QLJM:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    lambda:
      type: double
      range:
        - start: 0.1
          end: 0.99
          step: 0.1
    (weighted:
      type: boolean
      values: [true,false])

And, finally, for the query likelihood model with Laplace smoothing:

.. code:: yaml

  QLL:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    phi:
      type: double
      values: [0.01,0.1,1,10,100]
    (weighted:
      type: boolean
      values: [true,false])

Vector space model
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Adaptation of the original vector space model in information retrieval.

**Reference:** G. Salton, A. Wong, C.S. Yang. A vector space for automatic indexing. Communications of the ACM 18(11), 613-620 (1975).

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

  VSM:
    uSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    vSel:
      type: orientation
      values: [IN,OUT,UND,MUTUAL]
    (weighted:
      type: boolean
      values: [true,false])