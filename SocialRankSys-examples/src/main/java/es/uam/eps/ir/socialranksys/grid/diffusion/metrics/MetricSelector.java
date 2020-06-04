/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics;

import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.global.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.indiv.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.informationpieces.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserGlobalEntropyMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserGlobalGiniMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserRecallMetricConfigurator;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.metrics.SimulationMetric;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricIdentifiers.*;

/**
 * Class that selects an individual metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class MetricSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a metric.
     * @param ppr Parameters for the metric.
     * @return A pair containing the name and the selected metric.
     */
    public Tuple2oo<String, SimulationMetric<U,I,P>> select(MetricParamReader ppr)
    {
        String name = ppr.getName();
        MetricConfigurator<U,I,P> conf;
        switch(name)
        {
            case GINI:
                conf = new FeatureGiniMetricConfigurator<>();
                break;
            case GLOBALGINI:
                conf = new FeatureGlobalGiniMetricConfigurator<>();
                break;
            case GLOBALUSERGINI:
                conf = new FeatureGlobalUserGiniMetricConfigurator<>();
                break;
            case RECALL:
                conf = new FeatureRecallMetricConfigurator<>();
                break;
            case NRECALL:
                conf = new FeatureNormalizedRecallMetricConfigurator<>();
                break;
            case EXTRATE:
                conf = new ExternalFeatureRateMetricConfigurator<>();
                break;
            case GLOBALEXTRATE:
                conf = new GlobalExternalFeatureRateMetricConfigurator<>();
                break;
            case ENTROPY:
                conf = new FeatureEntropyMetricConfigurator<>();
                break;
            case GLOBALENTROPY:
                conf = new FeatureGlobalEntropyMetricConfigurator<>();
                break;
            case GLOBALUSERENTROPY:
                conf = new FeatureGlobalUserEntropyMetricConfigurator<>();
                break;

            case EXTRECALL:
                conf = new ExternalFeatureRecallMetricConfigurator<>();
                break;
            case EXTGINI:
                conf = new ExternalFeatureGiniMetricConfigurator<>();
                break;
            case GLOBALEXTGINI:
                conf = new ExternalFeatureGlobalGiniMetricConfigurator<>();
                break;

            case KLD:
                conf = new FeatureKLDivergenceMetricConfigurator<>();
                break;
            case INVKLD:
                conf = new FeatureKLDivergenceInverseMetricConfigurator<>();
                break;
            case GLOBALKLD:
                conf = new FeatureGlobalKLDivergenceMetricConfigurator<>();
                break;
            case GLOBALKLDINVERSE:
                conf = new FeatureGlobalKLDivergenceInverseMetricConfigurator<>();
                break;
                
            case USERFEATURECOUNT:
                conf = new UserFeatureCountMetricConfigurator<>();
                break;
            case USERFEATUREGINI:
                conf = new UserFeatureGiniMetricConfigurator<>();
                break;
                
            case MONTECARLOGLOBALGINI:
                conf = new MonteCarloFeatureGlobalGiniMetricConfigurator<>();
                break;
            case MONTECARLOGLOBALUSERGINI:
                conf = new MonteCarloFeatureGlobalUserGiniMetricConfigurator<>();
                break;
            case MONTECARLOUSERFEATUREGINI:
                conf = new MonteCarloUserFeatureGiniMetricConfigurator<>();
                break;
                
            case USERSPEED:
                conf = new UserSpeedMetricConfigurator<>();
                break;
            case SPEED:
                conf = new SpeedMetricConfigurator<>();
                break;
            case INFOGINI:
                conf = new InformationGiniMetricConfigurator<>();
                break;
            case REALPROPRECALL:
                conf = new RealPropagatedRecallMetricConfigurator<>();
                break;
            case GLOBALREALPROPRECALL:
                conf = new GlobalRealPropagatedRecallMetricConfigurator<>();
                break;
                
            case USERGLOBALGINI:
                conf = new UserGlobalGiniMetricConfigurator<>();
                break;
            case USERRECALL:
                conf = new UserRecallMetricConfigurator<>();
                break;
            case USERGLOBALENTROPY:
                conf = new UserGlobalEntropyMetricConfigurator<>();
                break;
            
            default:
                return null;
        }
        
        SimulationMetric<U,I,P> propagation = conf.configure(ppr);
        return new Tuple2oo<>(name, propagation);
    }
}
