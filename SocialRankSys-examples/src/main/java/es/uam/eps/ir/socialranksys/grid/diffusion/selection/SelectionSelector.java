/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.selection.SelectionMechanismIdentifiers.*;

/**
 * Class that selects an individual selection mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class SelectionSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a selection mechanism.
     * @param spr Parameters for the selection mechanism.
     * @return A pair containing the name and the selected selection mechanism.
     */
    public Tuple2oo<String, SelectionMechanism<U,I,P>> select(SelectionParamReader spr)
    {
        String name = spr.getName();
        SelectionConfigurator<U,I,P> conf;
        switch(name)
        {
            case COUNT:
                conf = new CountSelectionConfigurator<>();
                break;
            case ICM:
                conf = new IndependentCascadeModelSelectionConfigurator<>();
                break;
            case PUSHPULL:
                conf = new PullPushSelectionConfigurator<>();
                break;
            case REC:
                conf = new RecommenderSelectionConfigurator<>();
                break;
            case PUREREC:
                conf = new PureRecommenderSelectionConfigurator<>();
                break;
            case PURERECBATCH:
                conf = new PureRecommenderBatchSelectionConfigurator<>();
                break;
            case THRESHOLD:
                conf = new ThresholdSelectionConfigurator<>();
                break;
            case COUNTTHRESHOLD:
                conf = new CountThresholdSelectionConfigurator<>();
                break;
            case ALLREALPROP:
                conf = new AllRealPropagatedSelectionConfigurator<>();
                break;
            case COUNTREALPROP:
                conf = new CountRealPropagatedSelectionConfigurator<>();
                break;
            case PURETIMESTAMP:
                conf = new PureTimestampBasedSelectionConfigurator<>();
                break;
            case LOOSETIMESTAMP:
                conf = new LooseTimestampBasedSelectionConfigurator<>();
                break;
            case TIMESTAMPORDERED:
                conf = new TimestampOrderedSelectionConfigurator<>();
                break;
            default:
                return null;
        }
        
        SelectionMechanism<U,I,P> selection = conf.configure(spr);
        return new Tuple2oo<>(name, selection);
    }
}
