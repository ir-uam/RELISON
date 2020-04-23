/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.tree.fast;

import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.DirectedEdges;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.ir.socialranksys.graph.tree.Tree;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fast implementation of the tree.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the nodes of the tree
 */
public abstract class FastTree<U> extends FastGraph<U> implements Tree<U>
{   
    /**
     * Constructor.
     * @param edges The edges of the tree. 
     */
    public FastTree(DirectedEdges edges)
    {
        super(new FastIndex<>(), edges);

    }
    
    @Override
    public U getRoot()
    {
        if(this.getVertexCount() > 0)
            return vertices.idx2object(0);
        return null;
    }

    @Override
    public Stream<U> getLeaves()
    {
        List<U> leaves = new ArrayList<>();
        
        this.getAllNodes().forEach(u -> 
        {
            if(this.getAdjacentNodesCount(u) == 0)
            {
                leaves.add(u);
            }
        });
        
        return leaves.stream();
    }

    @Override
    public Stream<U> getLeaves(U u)
    {
        List<U> leaves = new ArrayList<>();
        
        if(!this.containsVertex(u))
            return null;
        
        if(this.getChildrenCount(u) == 0)
        {
            return Stream.empty();
        }
        else
        {
            this.getChildren(u).forEach(v -> 
            {
                if(this.isLeaf(v))
                {
                    leaves.add(v);
                }
                else
                {
                    this.getLeaves(v).forEach(leaves::add);
                }
            });
        }
        
        return leaves.stream();
    }

    @Override
    public Stream<U> getLevel(int level)
    {
        // Perform a breadth first search, with fixed depth.
        LinkedList<U> currentLevelUsers = new LinkedList<>();
        LinkedList<U> nextLevelUsers = new LinkedList<>();
        int currentLevel = 0;
        currentLevelUsers.add(this.getRoot());
        
        while(!currentLevelUsers.isEmpty() && currentLevel != level)
        {
            U current = currentLevelUsers.pop();
            this.getChildren(current).forEach(nextLevelUsers::add);
            
            if(currentLevelUsers.isEmpty())
            {
                ++currentLevel;
                currentLevelUsers.addAll(nextLevelUsers);
                nextLevelUsers.clear();
            }
        }
        
        if(level == currentLevel)
        {
            return currentLevelUsers.stream();
        }
        else
        {
            return Stream.empty();
        }
    }
    
    @Override
    public Map<Integer, Set<U>> getLevels()
    {
        Map<Integer, Set<U>> levels = new HashMap<>();
        // Perform a breadth first search, with fixed depth.
        LinkedList<U> currentLevelUsers = new LinkedList<>();
        LinkedList<U> nextLevelUsers = new LinkedList<>();
        int currentLevel = 0;
        currentLevelUsers.add(this.getRoot());
        
        levels.put(currentLevel, new HashSet<>(nextLevelUsers));
        while(!currentLevelUsers.isEmpty())
        {
            U current = currentLevelUsers.pop();
            this.getChildren(current).forEach(nextLevelUsers::add);
            
            if(currentLevelUsers.isEmpty())
            {
                ++currentLevel;
                levels.put(currentLevel, new HashSet<>(nextLevelUsers));
                currentLevelUsers.addAll(nextLevelUsers);
                nextLevelUsers.clear();
            }
        }
        
        return levels;        
    }


    @Override
    public Stream<U> getChildren(U u)
    {
        if(!this.containsVertex(u))
            return null;
        
        return this.edges.getAdjacentNodes(this.vertices.object2idx(u))
                   .map(this.vertices::idx2object);
    }
    
    @Override
    public long getChildrenCount(U u)
    {
        if(this.containsVertex(u))
            return this.edges.getAdjacentCount(this.vertices.object2idx(u));
        else
            return -1;
    }

    @Override
    public U getParent(U u)
    {
        if(this.containsVertex(u))
        {
            if(this.isRoot(u))
            {
                return null;
            }

            int parentIdx = this.edges.getIncidentNodes(this.vertices.object2idx(u)).findFirst().get();
            return this.vertices.idx2object(parentIdx);
            
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isLeaf(U u)
    {
        if(this.containsVertex(u))
        {
            return this.getChildrenCount(u) == 0;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean isRoot(U u)
    {
        if(this.containsVertex(u))
        {
            return this.getIncidentNodesCount(u) == 0;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean addRoot(U root)
    {
        if(this.vertices.numObjects() > 0)
            return false;
        else
        {
            int idx = this.vertices.addObject(root);
            return this.edges.addUser(idx);
        }
    }       

    @Override
    public boolean addChild(U parent, U child, double weight, int type)
    {
        if(this.containsVertex(parent))
        {
            if(this.containsVertex(child))
            {
                return false;
            }
            
            int parentIdx = this.vertices.object2idx(parent);
            int childIdx = this.vertices.addObject(child);
            this.edges.addUser(childIdx);
            
            return this.edges.addEdge(parentIdx,childIdx,weight,type);
        }
        else
        {
            return false; // the parent node does not exist.
        }
    }
    
    @Override
    public boolean isParent(U parent, U child)
    {
        if(this.containsVertex(parent) && this.containsVertex(child))
        {
            return this.containsEdge(parent, child);
        }
        else
        {
            return false;
        }
    }
    
    
    @Override
    public boolean isAscendant(U parent, U child)
    {
        if(this.containsVertex(parent) && this.containsVertex(child))
        {
            if(parent.equals(child))
                return false;
            
            // Perform a breadth first search, with fixed depth.
            LinkedList<U> currentLevelUsers = new LinkedList<>();
            LinkedList<U> nextLevelUsers = new LinkedList<>();
            currentLevelUsers.add(parent);
            boolean found = false;
            while(!currentLevelUsers.isEmpty() && !found)
            {
                U current = currentLevelUsers.pop();
                List<U> children = this.getChildren(current).collect(Collectors.toCollection(ArrayList::new));
                for(U node : children)
                {
                    if(node.equals(child))
                    {
                        found = true;
                    }
                    
                    nextLevelUsers.add(node);
                }
                

                if(currentLevelUsers.isEmpty())
                {
                    currentLevelUsers.addAll(nextLevelUsers);
                    nextLevelUsers.clear();
                }
            }

            return found;
        }
        else
        {
            return false;
        }
        
        
    }  
    
    @Override
    public boolean addNode(U node)
    {
        throw new UnsupportedOperationException("Trees do not allow adding nodes this way");
    }
    
    @Override
    public boolean addEdge(U nodeA, U nodeB, double weight, int type, boolean insertNodes)
    {
        throw new UnsupportedOperationException("Trees do not allow adding nodes this way");
    }
    
    @Override
    public Stream<Weight<U,Double>> getChildrenWeights(U u)
    {
        if(this.containsVertex(u))
        {
            return this.getAdjacentNodesWeights(u);
        }
        return null;
    }
    
    @Override
    public double getParentWeight(U u)
    {
        if(!this.containsVertex(u))
            return Double.NaN;
        else if(this.isRoot(u))
            return Double.NaN;
        else
            return this.getEdgeWeight(this.getParent(u), u);
    }    
}
