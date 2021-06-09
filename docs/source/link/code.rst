.. role:: java(code)
	:language: Java


Integrate link recommendation / prediction functionalities in a Java project
============================================================================

By importing the linkpred package, anyone can use link recommendation and prediction approaches and integrate them in their code. We explore here how to do it, as well as how to define novel methods.

* `Link recommendation`_
* `Link prediction`_

Link recommendation
^^^^^^^^^^^^^^^^^^^^
If we want to recommend people to any user of the social network, we can use any implemented people recommendation algorithm. These algorithms implement
the :java:`Recommender` interface, originally defined for the RankSys (https://ranksys.github.io) library. This interface has the following methods:

.. code:: Java
	
	Recommendation<U,U> getRecommendation(U user)

This method obtains a recommendation. It does not limit the size of the recommendation, so it returns every recommendable candidate user.

**Arguments:**
	* *user*: the identifier of the user.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

.. code:: Java
	
	Recommendation<U,U> getRecommendation(U user, int maxLength)

This method obtains a recommendation, containing (at most) a fixed number of candidate users.

**Arguments:**
	* *user:* the identifier of the user.
	* *maxLength:* the maximum number of candidate users to recommend.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

.. code:: Java
	
	Recommendation<U,U> getRecommendation(U user, int maxLength, Predicate<U> filter)

This method obtains a recommendation, containing (at most) a fixed number of candidate users. Candidate users are only considered if they pass a filter.

**Arguments:**
	* *user:* the identifier of the user.
	* *maxLength:* the maximum number of candidate users to recommend.
	* *filter:* a filter for selecting the set of candidate users.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

.. code:: Java
	
	Recommendation<U,U> getRecommendation(U user, Predicate<U> filter)

This method obtains a recommendation for a single user. It does not limit the size of the recommendation, so it returns every recommendable candidate user. Candidate users are only recommendable if they pass a filter.

**Arguments:**
	* *user:* the identifier of the user.
	* *filter:* a filter for selecting the set of candidate users.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

.. code:: Java
	
	Recommendation<U,U> getRecommendation(U user, Stream<U> candidates)

This method obtains a recommendation for a single user, and receives the set of candidate users by argument. These set of candidate users is ranked and returned.

**Arguments:**
	* *user:* the identifier of the user.
	* *candidates:* an stream containing the different users.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

Building new recommenders
^^^^^^^^^^^^^^^^^^^^^^^^^^
In case we want to build new recommendation algorithms, we might want to implement all the previous functions. But we can do it much simpler.
Considering that we receive a FastGraph, we can create a method that inherits from the :java:`UserFastRankingRecommender` class.

These methods must use a :java:`FastGraph<U>` in the constructor, and then, you only have to implement the following method:

.. code:: Java

	public Int2DoubleMap getScoresMap(int uidx)

This method generates all the possible scores for the target-candidate user pairs, and returns them in a map, indexed by the identifier of the candidate user.

**Arguments:**
	* *user:* the identifier of the user.
	* *candidates:* an stream containing the different users.
**Returns**
	* the recommendation object (containing the target user and a list of sorted candidate user-score pairs).

This would be, for example, the case of the :java:`Popularity` algorithm, which we show below:

.. code:: Java

    public class Popularity<U> extends UserFastRankingRecommender<U>
    {
        /**
         * Link orientation for selecting the neighbours of the candidate node.
         */
        private final EdgeOrientation vSel;

        /**
         * Constructor for recommendation mode.
         *
         * @param graph graph.
         * @param vSel  link orientation for selecting the neighbours of the candidate node.
         */
        public Popularity(FastGraph<U> graph, EdgeOrientation vSel)
        {
            super(graph);
            this.vSel = vSel;
        }

        /**
         * Constructor for recommendation mode.
         *
         * @param graph graph.
         */
        public Popularity(FastGraph<U> graph)
        {
            super(graph);
            this.vSel = EdgeOrientation.IN;
        }

        @Override
        public Int2DoubleMap getScoresMap(int uidx)
        {
            U u = this.uidx2user(uidx);

            Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
            scoresMap.defaultReturnValue(-1.0);

            this.getAllUsers().forEach(v -> scoresMap.put(this.item2iidx(v), this.getGraph().getNeighbourhoodSize(v, vSel) + 0.0));
            return scoresMap;
        }


Link prediction
^^^^^^^^^^^^^^^^^^^^
Finally, if we want to integrate link prediction methods in the code, we need to use the :java:`LinkPrediction` interface, which contains the following methods:

.. code:: Java
	
	Prediction<U> getPrediction()

This method just finds ranks all the possible user-user pairs.

**Returns**
	* a prediction: a sorted list of user-user-score triplets.

.. code:: Java
    
    Prediction<U> getPrediction(int maxLength)

This method just finds ranks a fixed number of user-user pairs (at most).

**Arguments:**
    * *maxLength*: the maximum number of links to predict.
**Returns**
    * a prediction: a sorted list of user-user-score triplets.

.. code:: Java
    
    Prediction<U> getPrediction(Predicate<Pair<U>> filter)

This method just finds ranks the set of possible user-user pairs passing a given filter.

**Arguments:**
    * *filter*: the user-user pair filter.
**Returns**
    * a prediction: a sorted list of user-user-score triplets.

.. code:: Java
    
    Prediction<U> getPrediction(int maxLength, Predicate<Pair<U>> filter)

This method just finds ranks the set of possible user-user pairs passing a given filter. It only predicts a fixed number of links.

**Arguments:**
    * *maxLength*: the maximum number of links to predict.
    * *filter*: the user-user pair filter.
**Returns**
    * a prediction: a sorted list of user-user-score triplets.


.. code:: Java
    
    Prediction<U> getPrediction(Stream<Pair<U>> candidates)

This method ranks a given set of pairs of users.

**Arguments:**
    * *candidates*: a stream containing the pairs of users to consider.
**Returns**
    * a prediction: a sorted list of user-user-score triplets.

.. code:: Java
    
    double getPredictionScore(U u, U v)

This method obtains the prediction score for a pair of users.

**Arguments:**
    * *u*: the first user.
    * *v*: the second user
**Returns**
    * the prediction score for the given pair of users.

Using people recommendation algorithms as link predictors
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
People recommendation and link prediction are closely related (they differ on how the task is carried), so it is possible
to use link recommendation algorithms to predict the next links to appear in a network. For this, we include in the framework
a class, :java:`RecommendationLinkPredictor`. This class receives in the constructor the recommender we want to use.