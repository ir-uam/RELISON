/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.selection;

import es.uam.eps.ir.sonalire.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.sonalire.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.sonalire.grid.diffusion.selection.SelectionMechanismIdentifiers.*;

/**
 * Class that selects an individual selection mechanism given its parameters.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class SelectionSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects and configures a selection mechanism.
     * @param name      the name of the selection mechanism.
     * @param params    the parameters of the selection mechanism.
     * @return A pair containing the name and the selected selection mechanism.
     */
    public Tuple2<String, SelectionMechanism<U,I, F>> select(String name, Parameters params)
    {
        SelectionConfigurator<U,I, F> conf;
        switch(name)
        {
            case ALLREALPROP:
                conf = new AllRealPropagatedSelectionConfigurator<>();
                break;
            case PURERECBATCH:
                conf = new BatchRecommenderSelectionConfigurator<>();
                break;
            case COUNTREALPROP:
                conf = new CountRealPropagatedSelectionConfigurator<>();
                break;
            case COUNT:
                conf = new CountSelectionConfigurator<>();
                break;
            case COUNTTHRESHOLD:
                conf = new CountThresholdSelectionConfigurator<>();
                break;
            case ICM:
                conf = new IndependentCascadeModelSelectionConfigurator<>();
                break;
            case LIMITEDCOUNTTHRESHOLD:
                conf = new LimitedCountThresholdSelectionConfigurator<>();
                break;
            case LIMITEDPROPTHRESHOLD:
                conf = new LimitedProportionThresholdSelectionConfigurator<>();
                break;
            case LOOSETIMESTAMP:
                conf = new LooseTimestampBasedSelectionConfigurator<>();
                break;
            case ONLYOWN:
                conf = new OnlyOwnSelectionConfigurator<>();
                break;
            case PROPORTIONTHRESHOLD:
                conf = new ProportionThresholdSelectionConfigurator<>();
                break;
            case PUSHPULL:
                conf = new PullPushSelectionConfigurator<>();
                break;
            case PUREREC:
                conf = new PureRecommenderSelectionConfigurator<>();
                break;
            case REC:
                conf = new RecommenderSelectionConfigurator<>();
                break;
            case TIMESTAMPORDERED:
                conf = new TimestampOrderedSelectionConfigurator<>();
                break;
            default:
                return null;
        }
        
        SelectionMechanism<U,I, F> selection = conf.configure(params);
        return new Tuple2<>(name, selection);
    }
}
