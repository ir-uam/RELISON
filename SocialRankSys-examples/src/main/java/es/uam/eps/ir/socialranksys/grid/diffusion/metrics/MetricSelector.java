/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics;

import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.global.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.indiv.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.informationpieces.*;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserGlobalEntropyMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserGlobalGiniMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users.UserRecallMetricConfigurator;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricIdentifiers.*;

/**
 * Class for selecting an individual diffusion metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class MetricSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a metric.
     * @param name  the name of the metric.
     * @param params the set of parameters for the metric.
     * @return a pair containing the name and the selected metric.
     */
    public Tuple2<String, SimulationMetric<U,I,P>> select(String name, Parameters params)
    {
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
        
        SimulationMetric<U,I,P> propagation = conf.configure(params);
        return new Tuple2<>(propagation.getName(), propagation);
    }
}
