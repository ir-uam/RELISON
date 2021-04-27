/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Given as set of instances, this algorithm predicts the presence or absence of a link, according
 * to a function of the different features. The score for a possible pair of indexes is defined by
 * a postfix expression with the following format:
 *
 * Format of the postfix expression:
 * - If two numbers are next to each other, they must be separated using a dot (".").
 * - Operations allowed: +,-,*,/
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of users
 */
public class InstancePostfixExLinkPredictor<U> extends AbstractLinkPredictor<U>
{

    /**
     * List which contains the results of the algorithm link prediction.
     */
    private final List<Tuple2od<Pair<U>>> results;
    
    /**
     * Constructor.
     * @param graph The graph.
     * @param patterns The set of patterns.
     * @param postfix The postfix notation expression.
     * @param comparator Comparator for ordering the nodes.
     * @throws UnsupportedOperationException if the postfix expression is badly created.
     */
    public InstancePostfixExLinkPredictor(Graph<U> graph, Comparator<Tuple2od<Pair<U>>> comparator, InstanceSet<U> patterns, String postfix)
    {
        super(graph, comparator);
        
        TreeSet<Tuple2od<Pair<U>>> ordered = new TreeSet<>(this.getComparator());
        
        if(Character.isDigit(postfix.charAt(postfix.length()-1))) // a lonely column
        {
            int attrId = Integer.parseInt(postfix);
            patterns.getAllInstances().forEach(pattern -> {
               Tuple2od<Pair<U>> value = new Tuple2od<>(new Pair<>(pattern.getOrigin(), pattern.getDest()), pattern.getValues().get(attrId));
               ordered.add(value);
            });
        }
        else // a regular expression in postfix format.
        {
            Stack<Double> stack = new Stack<>();
            TreeSet<Integer> points = new TreeSet<>();

            
            // Store which points contain a difference between numbers and characters
            int lastIndex = 0;
            while(lastIndex != -1) // points represent a separation between two numbers
            {
                lastIndex = postfix.indexOf(".", lastIndex+1);
                if(lastIndex != -1)
                {
                    points.add(lastIndex);
                    points.add(lastIndex+1);
                }
            }

            lastIndex = 0; // sums
            while(lastIndex != -1)
            {
                lastIndex = postfix.indexOf("+",lastIndex+1);
                if(lastIndex != -1)
                {
                    points.add(lastIndex);
                    points.add(lastIndex+1);
                }
            }

            lastIndex = 0; // substractions
            while(lastIndex != -1)
            {
                lastIndex = postfix.indexOf("-",lastIndex+1);
                if(lastIndex != -1)
                {
                    points.add(lastIndex);
                    points.add(lastIndex+1);
                }
            }

            lastIndex = 0;
            while(lastIndex != -1) // divisions
            {
                lastIndex = postfix.indexOf("/",lastIndex+1);
                if(lastIndex != -1)
                {
                    points.add(lastIndex);
                    points.add(lastIndex+1);
                }
            }

            lastIndex = 0; 
            while(lastIndex != -1) // products
            {
                lastIndex = postfix.indexOf("*",lastIndex+1);
                if(lastIndex != -1)
                {
                    points.add(lastIndex);
                    points.add(lastIndex+1);
                }
            }

            // Store the different elements of the expression
            int currentIndex = 0;
            List<String> expression = new ArrayList<>();
            for (Integer next : points)
            {
                String substring = postfix.substring(currentIndex, next);

                if (!substring.equals("."))
                    expression.add(substring);
                currentIndex = next;
            }

            // For each identified pattern, process the postfix expression
            patterns.getAllInstances().forEach(pattern ->
            {
                Pair<U> pair = new Pair<>(pattern.getOrigin(), pattern.getDest());

                
                for(String term : expression)
                {
                    double v1;
                    double v2;
                    switch (term)
                    {
                        // sum
                        case "+" -> {
                            if (stack.size() < 2)
                                throw new UnsupportedOperationException("Invalid regexp: not enough operands");
                            v2 = stack.pop();
                            v1 = stack.pop();
                            stack.push(v1 + v2);
                        }
                        // substract
                        case "-" -> {
                            if (stack.size() < 2)
                                throw new UnsupportedOperationException("Invalid regexp: not enough operands");
                            v2 = stack.pop();
                            v1 = stack.pop();
                            stack.push(v1 - v2);
                        }
                        // product
                        case "*" -> {
                            if (stack.size() < 2)
                                throw new UnsupportedOperationException("Invalid regexp: not enough operands");
                            v2 = stack.pop();
                            v1 = stack.pop();
                            stack.push(v1 * v2);
                        }
                        // division
                        case "/" -> {
                            if (stack.size() < 2)
                                throw new UnsupportedOperationException("Invalid regexp: not enough operands");
                            v2 = stack.pop();
                            v1 = stack.pop();
                            stack.push(v1 / v2);
                        }
                        // store the value of the selected attribute
                        default -> {
                            int attrId = Integer.parseInt(term);
                            v1 = pattern.getValues().get(attrId);
                            stack.push(v1);
                        }
                    }
                }
                
                if(stack.size() > 1)
                    throw new UnsupportedOperationException("Invalid regexp");
                ordered.add(new Tuple2od<>(pair, stack.pop()));
            });
        }
        
        this.results = new ArrayList<>(ordered);
    }

    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter)
    {
        return this.results.stream().filter(tuple -> filter.test(tuple.v1)).limit(maxLength).collect(Collectors.toList());
    }
}
