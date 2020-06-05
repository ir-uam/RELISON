/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


/**
 * Rerankers that optimize global properties of the network related to the whole graph. 
 * This rerankers start from a graph containing all the recommended edges. Then, edges are
 * swapped with some of the not recommended edges, in order to improve the global parameter.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.graph;
