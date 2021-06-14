/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.data;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2ol;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class that contains the basic information for simulations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class Data<U extends Serializable,I extends Serializable, F>
{
    /**
     * User index.
     */
    private final Index<U> users;
    /**
     * Information pieces index.
     */
    private final Index<I> informationPieces;
    /**
     * Additional information for the information pieces.
     */
    private final Map<Integer, Information<I>> informationPieceData;
    
    /**
     * Feature indexes (stored by the name of the parameter).
     */
    private final Map<String, Index<F>> features;
    
    /**
     * Relation between users and the propagated information (allows identifying both the
     * information pieces created by a user, and the creator of the information piece.
     */
    private final Relation<Integer> userInformation;
    
    /**
     * The graph in which the simulation will run.
     */
    private final Graph<U> graph;
    
    /**
     * List of features related to each individual user.
     */
    private final List<String> userFeatureNames;
    /**
     * List of features related to each information piece
     */
    private final List<String> infoPiecesFeatureNames;
    
    /**
     * Relation between users and their features (allows identifying the users
     * that share the same feature, as well as the features of the different users).
     */
    private final Map<String, Relation<Double>> userFeatures;
    
    /**
     * Relation between users and their features (allows identifying the information pieces
     * that share the same feature, as well as the features of the different information pieces).
     */
    private final Map<String, Relation<Double>> infoPiecesFeatures;

    /**
     * Map containing the information pieces which have been really propagated by the user.
     */
    private final Relation<Long> realPropagated;
    
    /**
     * Ordered set of timestamps when information pieces where published in real life.
     */
    private TreeSet<Long> timestamps;

    /**
     * Relation between the timestamps and the pieces propagated by each user.
     */
    private final HashMap<Long, HashMap<U, Set<I>>> propagatedByTS;

    /**
     * Relation between the timestamps and the pieces repropagated by each user.
     */
    private final HashMap<Long, HashMap<U, Set<I>>> realPropagatedByTS;

    
    /**
     * Simplest constructor. There is no additional information for information pieces, nor features.
     * @param graph the social network graph.
     * @param users the user index.
     * @param informationPieces information pieces index.
     * @param userInformation relation between users and information pieces.
     */
    public Data(Graph<U> graph, Index<U> users, Index<I> informationPieces, Relation<Integer> userInformation)
    {
        this(graph, users, informationPieces, new HashMap<>(), userInformation, new HashMap<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), new HashMap<>());        
    }
    
    /**
     * Constructor. There is no additional information for information pieces, nor features.
     * @param graph the social network graph.
     * @param users the user index.
     * @param informationPieces information pieces index.
     * @param informationData detailed information about the information pieces.
     * @param userInformation relation between users and information pieces.
     */
    public Data(Graph<U> graph, Index<U> users, Index<I> informationPieces, Map<Integer, Information<I>> informationData,  Relation<Integer> userInformation)
    {
        this(graph, users, informationPieces, informationData, userInformation, new HashMap<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), new HashMap<>());        
    }
    
    /**
     * Constructor. Only user / information pieces features.
     * @param graph the social network graph.
     * @param users the user index.
     * @param informationPieces information pieces index.
     * @param informationData detailed information about the information pieces.
     * @param userInformation relation between users and information pieces.
     * @param features indexes for the different features.
     * @param featureNames the names of the user/pieces features.
     * @param featureRel relation between users/pieces and their features.
     * @param userfeatures true if features are related to users, false if they are related to information pieces.
     */
    public Data(Graph<U> graph, Index<U> users, Index<I> informationPieces, Map<Integer, Information<I>> informationData, Relation<Integer> userInformation, Map<String, Index<F>> features, List<String> featureNames, Map<String, Relation<Double>> featureRel, boolean userfeatures)
    {
        this.users = users;
        this.informationPieces = informationPieces;
        this.informationPieceData = informationData;
        this.features = features;
        this.userInformation = userInformation;
        this.graph = graph;
        this.userFeatureNames = userfeatures ? featureNames : new ArrayList<>();
        this.infoPiecesFeatureNames = userfeatures ? new ArrayList<>() : featureNames;
        this.userFeatures = userfeatures ? featureRel : new HashMap<>();
        this.infoPiecesFeatures = userfeatures ? new HashMap<>() : featureRel;
        this.realPropagated = new FastWeightedPairwiseRelation<>();
        this.propagatedByTS = new HashMap<>();
        this.realPropagatedByTS = new HashMap<>();
        IntStream.range(0, this.users.numObjects()).forEach(this.realPropagated::addFirstItem);
        IntStream.range(0, this.informationPieces.numObjects()).forEach(this.realPropagated::addSecondItem);
        
        
        IntStream.range(0, this.users.numObjects()).forEach(uidx -> 
        {
            this.userInformation.getIdsSecond(uidx).forEach(iidx -> this.timestamps.add(this.informationPieceData.get(iidx.getIdx()).getTimestamp()));
            this.realPropagated.getIdsSecond(uidx).forEach(iidx -> this.timestamps.add(iidx.getValue()));
        });
        this.timestamps.add(Long.MAX_VALUE);

        
        this.timestamps.forEach(ts -> 
        {
            propagatedByTS.put(ts, new HashMap<>());
            realPropagatedByTS.put(ts, new HashMap<>());
        });
        
        
        buildTimestampPieces();
        
    }
    
    /**
     * Builds the relationship between timestamps and pairs (user, piece).
     */
    private void buildTimestampPieces()
    {
        this.getUserIndex().getAllObjects().forEach(u -> 
        {
            this.getPieces(u).forEach(i -> 
            {
                long ts = this.getTimestamp(i);
                if(!propagatedByTS.get(ts).containsKey(u))
                {
                    propagatedByTS.get(ts).put(u, new HashSet<>());
                }
                propagatedByTS.get(ts).get(u).add(i);
            });
            
            this.getRealPropagatedPiecesWithTimestamp(u).forEach(tuple -> 
            {
                long ts = tuple.v2();
                if(!realPropagatedByTS.get(ts).containsKey(u))
                {
                    realPropagatedByTS.get(ts).put(u, new HashSet<>());
                }
                realPropagatedByTS.get(ts).get(u).add(tuple.v1());
            });
        });
    }
        
    /**
     * Full constructor.
     * @param graph the social network graph.
     * @param users the user index.
     * @param informationPieces information pieces index.
     * @param informationData detailed information about the information pieces.
     * @param userInformation relation between users and information pieces.
     * @param features indexes for the different features.
     * @param userFeatureNames the names of the user features.
     * @param userFeatures relation between users and their features.
     * @param infoPiecesFeatureNames the name for information pieces features.
     * @param infoPiecesFeatures relation between information pieces and their features.
     */
    public Data(Graph<U> graph, Index<U> users, Index<I> informationPieces, Map<Integer, Information<I>> informationData, Relation<Integer> userInformation, Map<String, Index<F>> features, List<String> userFeatureNames, Map<String, Relation<Double>> userFeatures, List<String> infoPiecesFeatureNames, Map<String, Relation<Double>> infoPiecesFeatures)
    {
        this(graph,users,informationPieces,informationData,userInformation,features,userFeatureNames,userFeatures,infoPiecesFeatureNames,infoPiecesFeatures, null);
    }
    
    /**
     * Full constructor.
     * @param graph the social network graph.
     * @param users the user index.
     * @param informationPieces information pieces index.
     * @param informationData detailed information about the information pieces.
     * @param userInformation relation between users and information pieces.
     * @param features indexes for the different features.
     * @param userFeatureNames the names of the user features.
     * @param userFeatures relation between users and their features.
     * @param infoPiecesFeatureNames the name for information pieces features.
     * @param infoPiecesFeatures relation between information pieces and their features.
     * @param realPropagated relation indicating which information pieces were repropagated in real scenario.
     */
    public Data(Graph<U> graph, Index<U> users, Index<I> informationPieces, Map<Integer, Information<I>> informationData, Relation<Integer> userInformation, Map<String, Index<F>> features, List<String> userFeatureNames, Map<String, Relation<Double>> userFeatures, List<String> infoPiecesFeatureNames, Map<String, Relation<Double>> infoPiecesFeatures, Relation<Long> realPropagated)
    {
        this.users = users;
        this.informationPieces = informationPieces;
        this.informationPieceData = informationData;
        this.features = features;
        this.userInformation = userInformation;
        this.graph = graph;
        this.userFeatureNames = userFeatureNames;
        this.infoPiecesFeatureNames = infoPiecesFeatureNames;
        this.userFeatures = userFeatures;
        this.infoPiecesFeatures = infoPiecesFeatures;
        this.propagatedByTS = new HashMap<>();
        this.realPropagatedByTS = new HashMap<>();

        if(realPropagated == null)
        {
            this.realPropagated = new FastWeightedPairwiseRelation<>();
            IntStream.range(0, this.users.numObjects()).forEach(this.realPropagated::addFirstItem);
            IntStream.range(0, this.informationPieces.numObjects()).forEach(this.realPropagated::addSecondItem);
        }
        else
        {
            this.realPropagated = realPropagated;
        }
        
        this.timestamps = new TreeSet<>();
        IntStream.range(0, this.users.numObjects()).forEach(uidx -> 
        {
            this.userInformation.getIdsSecond(uidx).forEach(iidx -> this.timestamps.add(this.informationPieceData.get(iidx.getIdx()).getTimestamp()));
            
            this.realPropagated.getIdsSecond(uidx).forEach(iidx -> this.timestamps.add(iidx.getValue()));
        });
        
        this.timestamps.add(Long.MAX_VALUE);
        
        this.timestamps.forEach(ts -> 
        {
            propagatedByTS.put(ts, new HashMap<>());
            realPropagatedByTS.put(ts, new HashMap<>());
        });
        
        
        buildTimestampPieces();
    }
    /**
     * Obtain all users.
     * @return a stream containing all users.
     */
    public Stream<U> getAllUsers()
    {
        return this.users.getAllObjects();
    }
    
    /**
     * Obtain the number of users in the simulation.
     * @return the number of users.
     */
    public int numUsers()
    {
        return this.users.numObjects();
    }
    
    /**
     * Obtains a user index for creating relations.
     * @return the user index.
     */
    public Index<U> getUserIndex()
    {
        return this.users;
    }
    
    /**
     * Obtain all information pieces.
     * @return a stream containing all information pieces.
     */
    public Stream<I> getAllInformationPieces()
    {
        return this.informationPieces.getAllObjects();
    }
    
    /**
     * Obtains the number of information pieces.
     * @return the number of information pieces.
     */
    public int numInformationPieces()
    {
        return this.informationPieces.numObjects();
    }
    
    /**
     * Obtains an index for the different information pieces.
     * @return the information pieces index.
     */
    public Index<I> getInformationPiecesIndex() 
    {
        return informationPieces;
    }
    
    /**
     * Obtains the extended information of an information piece
     * @param piece the information piece.
     * @return the extended information if the piece exists, null if it does not.
     */
    public Information<I> getInformation(I piece)
    {
        int iidx = this.informationPieces.object2idx(piece);
        if(this.informationPieceData.containsKey(iidx))
        {
            return this.informationPieceData.get(iidx);
        }
        return null;
    }
    
    /**
     * Obtains the creators of a single information piece.
     * @param piece the piece.
     * @return a stream of creators, empty if there are none.
     */
    public Stream<U> getCreators(I piece)
    {
        int iidx = this.informationPieces.object2idx(piece);
        if(iidx == -1)
        {
            return Stream.empty();
        }
        return this.userInformation.getIdsFirst(iidx).map(uidx -> this.users.idx2object(uidx.getIdx()));
    }
       
    /**
     * Obtains the information pieces created by a user.
     * @param user the user.
     * @return a stream of information pieces, empty if there are none.
     */
    public Stream<I> getPieces(U user)
    {
        int uidx = this.users.object2idx(user);
        if(uidx == -1)
        {
            return Stream.empty();
        }
        return this.userInformation.getIdsSecond(uidx).map(iidx -> this.informationPieces.idx2object(iidx.getIdx()));
    }
        
    /**
     * Obtains the timestamp for an information piece.
     * @param info the information piece.
     * @return the timestamp in case it exists, -1 if it does not.
     */
    public long getTimestamp(I info)
    {
        int iidx = this.informationPieces.object2idx(info);
        
        if(iidx != -1)
        {
            return this.informationPieceData.get(iidx).getTimestamp();
        }
        
        return -1L;
    }
    
    /**
     * Obtains the graph.
     * @return the graph.
     */
    public Graph<U> getGraph()
    {
        return this.graph;
    }
    
    /**
     * Obtains the features for a single user.
     * @param user the user.
     * @param param the parameter.
     * @return a stream of pairs (parameter,value), empty if there are none.
     */
    public Stream<Tuple2od<F>> getUserFeatures(U user, String param)
    {
        int uidx = this.users.object2idx(user);
        if(this.userFeatures.containsKey(param) && uidx != -1)
        {
            return this.userFeatures.get(param).getIdsSecond(uidx).map(pidx -> new Tuple2od<>(this.features.get(param).idx2object(pidx.getIdx()), pidx.getValue()));
        }
        return Stream.empty();
    }
    
    /**
     * Obtains the features for a single information piece.
     * @param info the information piece.
     * @param param the parameter.
     * @return a stream of pairs (parameter,value), empty if there are none.
     */
    public Stream<Tuple2od<F>> getInfoPiecesFeatures(I info, String param)
    {
        int iidx = this.informationPieces.object2idx(info);
        if(this.infoPiecesFeatures.containsKey(param) && iidx != -1)
        {
            return this.infoPiecesFeatures.get(param).getIdsSecond(iidx).map(pidx -> new Tuple2od<>(this.features.get(param).idx2object(pidx.getIdx()), pidx.getValue()));
        }
        return Stream.empty();
    }
    
    /**
     * Gets all possible values for a certain parameter.
     * @param parameter the parameter name.
     * @return a stream containing all the possible values if the parameter exists,
     * an empty stream if it does not.
     */
    public Stream<F> getAllFeatureValues(String parameter)
    {
        if(this.doesFeatureExist(parameter))
        {
            return this.features.get(parameter).getAllObjects();
        }
        return Stream.empty();
    }
    
    /**
     * Gets the number of possible values for a certain parameter.
     * @param parameter the parameter name.
     * @return the number of possible features if the parameter exists, 0 if it does not.
     */
    public int numFeatureValues(String parameter)
    {
        if(this.doesFeatureExist(parameter))
        {
            return this.features.get(parameter).numObjects();
        }
        return 0; 
    }
    
    
    
    /**
     * Returns an index for a given parameter.
     * @param param the parameter name.
     * @return the index if it exists, null if it does not.
     */
    public Index<F> getFeatureIndex(String param)
    {
        if(this.features.containsKey(param))
            return this.features.get(param);
        return null;
    }
    
    /**
     * Indicates if a user is contained in the data.
     * @param u the user.
     * @return true if the user is contained in the data, false if it is not.
     */
    public boolean containsUser(U u)
    {
        return this.users.containsObject(u);
    }
    
    /**
     * Indicates if data contains a certain information piece.
     * @param i the information piece.
     * @return true if the user is contained in the data, false if it is not.
     */
    public boolean containsInformationPiece(I i)
    {
        return this.informationPieces.containsObject(i);
    }
    
    /**
     * Checks if a feature is stored in the data.
     * @param feature the feature name.
     * @return true if the feature exists, false otherwise.
     */
    public boolean doesFeatureExist(String feature)
    {
        return this.features.containsKey(feature);
    }
    
    /**
     * Checks if a feature exists, and it is a user feature.
     * @param feature the feature name.
     * @return true if the feature exists, false otherwise.
     */
    public boolean isUserFeature(String feature)
    {
        return this.features.containsKey(feature) && this.userFeatureNames.contains(feature);
    }
    
    /**
     * Checks if a feature exists, and it is an information piece feature.
     * @param feature the feature name.
     * @return true if the feature exists, false otherwise.
     */
    public boolean isInfoPieceFeature(String feature)
    {
        return this.features.containsKey(feature) && this.infoPiecesFeatureNames.contains(feature);
    }
    
    /**
     * Obtains the names of the user features.
     * @return a list containing the user features.
     */
    public List<String> getUserFeatureNames()
    {
        return this.userFeatureNames;
    }
    
    /**
     * Obtains the names of the information piece features.
     * @return a list containing the information piece features.
     */
    public List<String> getInfoPiecesFeatureNames()
    {
        return this.infoPiecesFeatureNames;
    }
    
    /**
     * Checks if a feature value exists.
     * @param feature the feature name.
     * @param value the feature value we want to check.
     * @return true if the value exists, false otherwise.
     */
    public boolean containsFeatureValue(String feature, F value)
    {
        if(this.doesFeatureExist(feature))
        {
            return this.features.get(feature).containsObject(value);
        }
        return false;
    }
    
    /**
     * Obtains all users with a certain feature value.
     * @param feature the name of the feature.
     * @param value the value of the feature.
     * @return a stream containing the different pairs (user, value) that are represented
     * by the value parameter.
     */
    public Stream<Tuple2od<U>> getUsersWithFeature(String feature, F value)
    {
        if(this.containsFeatureValue(feature, value))
        {
            int pidx = this.features.get(feature).object2idx(value);
            return this.userFeatures.get(feature).getIdsFirst(pidx).map(pair -> 
            {
                U u = this.users.idx2object(pair.getIdx());
                return new Tuple2od<>(u, pair.getValue());
            });
        }
        else
        {
            return Stream.empty();
        }
    }
    
    /**
     * Obtains all information pieces with a certain feature value.
     * @param feature the name of the feature.
     * @param value the value of the feature.
     * @return a stream containing the different pairs (infopiece, value) that are represented
     * by the value parameter.
     */
    public Stream<Tuple2od<I>> getInformationPiecesWithFeature(String feature, F value)
    {
        if(this.containsFeatureValue(feature, value))
        {
            int pidx = this.features.get(feature).object2idx(value);
            return this.infoPiecesFeatures.get(feature).getIdsFirst(pidx).map(pair -> 
            {
                I i = this.informationPieces.idx2object(pair.getIdx());
                return new Tuple2od<>(i, pair.getValue());
            });
        }
        else
        {
            return Stream.empty();
        }
    }    
    
    /**
     * Given an information piece, obtains the set of users that repropagated them in a real scenario.
     * @param piece the information piece.
     * @return the stream of users that repropagated the piece.
     */
    public Stream<U> getRealPropagatedUsers(I piece)
    {
        if(this.informationPieces.containsObject(piece))
        {
            int iidx = this.informationPieces.object2idx(piece);
            return this.realPropagated.getIdsFirst(iidx).map(tuple -> this.users.idx2object(tuple.getIdx()));
        }
        return Stream.empty();
    }
    
    /**
     * Given a user, obtains the set of information pieces repropagated in a real scenario.
     * @param user the user.
     * @return the stream of pieces repropagated by the user.
     */
    public Stream<I> getRealPropagatedPieces(U user)
    {
        if(this.users.containsObject(user))
        {
            int uidx = this.users.object2idx(user);
            return this.realPropagated.getIdsSecond(uidx).map(tuple -> this.informationPieces.idx2object(tuple.getIdx()));
        }
        return Stream.empty();
    }
    
    /**
     * Given a user, obtains the set of information pieces repropagated in a real scenario.
     * @param user the user.
     * @return the stream of pieces repropagated by the user and their timestamps.
     */
    public Stream<Tuple2ol<I>> getRealPropagatedPiecesWithTimestamp(U user)
    {
        if(this.users.containsObject(user))
        {
            int uidx = this.users.object2idx(user);
            return this.realPropagated.getIdsSecond(uidx).map(tuple -> new Tuple2ol<>(this.informationPieces.idx2object(tuple.getIdx()), tuple.getValue()));
        }
        
        return Stream.empty();
    }
    
    /**
     * Checks if a user repropagated or not an information piece.
     * @param user the user.
     * @param piece the piece.
     * @return true if the user repropagated the piece, false if he/she did not.
     */
    public boolean isRealRepropagatedPiece(U user, I piece)
    {
        if(this.users.containsObject(user) && this.informationPieces.containsObject(piece))
        {
            int uidx = this.users.object2idx(user);
            int iidx = this.informationPieces.object2idx(piece);
            return this.realPropagated.containsPair(uidx, iidx);
        }
        return false;
    }
    
    /**
     * Obtains the timestamp for an information piece.
     * @param user the user.
     * @param info the piece.
     * @return the timestamp if the user repropagated the piece, -1 if he/she did not.
     */
    public long getRealPropagatedTimestamp(U user, I info)
    {
        if(this.isRealRepropagatedPiece(user, info))
        {
            int uidx = this.users.object2idx(user);
            int iidx = this.informationPieces.object2idx(info);            
            return this.realPropagated.getValue(uidx, iidx);
        }
        
        return -1L;
    }
    
    /**
     * Obtains the list of timestamps.
     * @return the list of timestamps.
     */
    public TreeSet<Long> getTimestamps()
    {
        return this.timestamps;
    }

    /**
     * Obtains the list of pieces that a user has propagated in a given time.
     * @param timestamp the timestamp.
     * @param user the user.
     * @return an stream containing the pieces propagated by the user at the given time.
     */
    public Stream<I> getPiecesByTimestamp(long timestamp, U user)
    {
        if(this.propagatedByTS.containsKey(timestamp) && this.propagatedByTS.get(timestamp).containsKey(user))
            return this.propagatedByTS.get(timestamp).get(user).stream();
        return Stream.empty();
    }

    /**
     * Obtains the list of pieces which have been repropagated by a user at a given time.
     * @param timestamp the timestamp.
     * @param user the user.
     * @return an stream containing the pieces repropagated by the user at the given time.
     */
    public Stream<I> getRealPropPiecesByTimestamp(long timestamp, U user)
    {
        if(this.realPropagatedByTS.containsKey(timestamp) && this.realPropagatedByTS.get(timestamp).containsKey(user))
            return this.realPropagatedByTS.get(timestamp).get(user).stream();
        return Stream.empty();
    }

    /**
     * Obtains the list of users which have propagated pieces at a given time.
     * @param timestamp the timestamp.
     * @return a stream containing all users which have propagated information at the given time.
     */
    public Stream<U> getUsersByTimestamp(long timestamp)
    {
        if(this.propagatedByTS.containsKey(timestamp))
        {
            return this.propagatedByTS.get(timestamp).keySet().stream();
        }
        
        return Stream.empty();
    }

    /**
     * Obtains the list of users which have repropagated pieces at a given time.
     * @param timestamp the timestamp.
     * @return a stream containing all users which have repropagated information at the given time.
     */
    public Stream<U> getRealPropUsersByTimestamp(long timestamp)
    {
        if(this.realPropagatedByTS.containsKey(timestamp))
        {
            return this.realPropagatedByTS.get(timestamp).keySet().stream();
        }
        
        return Stream.empty();
    }
    
    /**
     * Obtains a string containing a summary of the data.
     * @return The summary of the data.
     */
    public String dataSummary()
    {
        StringBuilder summary = new StringBuilder();
        summary.append("Graph:\n");
        summary.append("\tNum. Nodes: ").append(this.graph.getVertexCount()).append("\n");
        summary.append("\tNum. Edges: ").append(this.graph.getEdgeCount()).append("\n");
        summary.append("Information:\n");
        summary.append("\tNum. Pieces: ").append(this.informationPieces.getAllObjects().count()).append("\n");
        summary.append("\tNum. Users with Info: ").append(this.users.getAllObjects().mapToInt(user -> this.getPieces(user).count() > 0 ? 1 : 0).sum()).append("\n");
        summary.append("User Features:\n");
        
        for(String name : this.userFeatureNames)
        {
            summary.append("\tname: ").append(name).append("\n");
            summary.append("\t\tNum. values: ").append(this.features.get(name).numObjects()).append("\n");
            summary.append("\t\tNum. relations: ").append(this.userFeatures.get(name).getAllFirst().mapToLong(u -> this.userFeatures.get(name).numSecond(u)).sum()).append("\n");
            summary.append("\t\tNum. users with feat.: ").append(this.users.getAllObjects().mapToInt(user -> this.getUserFeatures(user, name).count() > 0 ? 1 : 0).sum()).append("\n");
        }
        
        summary.append("Information Pieces Features:\n");
        for(String name : this.infoPiecesFeatureNames)
        {
            summary.append("\tname: ").append(name).append("\n");
            summary.append("\t\tNum. values: ").append(this.features.get(name).numObjects()).append("\n");
            summary.append("\t\tNum. relations: ").append(this.infoPiecesFeatures.get(name).getAllFirst().mapToLong(u -> this.infoPiecesFeatures.get(name).numSecond(u)).sum()).append("\n");
            summary.append("\t\tNum. info. pieces with feat.: ").append(this.informationPieces.getAllObjects().mapToInt(info -> this.getInfoPiecesFeatures(info, name).count() > 0 ? 1 : 0).sum()).append("\n");
        }
        
        summary.append("Information Pieces real propagation");
        {
            summary.append("\tNum. users that propagate other pieces:").append(this.realPropagated.numFirst()).append("\n");
            summary.append("\tNum. propagated info. pieces: ").append(this.realPropagated.numSecond()).append("\n");
            summary.append("\tNum. propagations: ").append(this.realPropagated.getAllFirst().mapToLong(this.realPropagated::numSecond).sum()).append("\n");
        }
        
        return summary.toString();
    }   


}
