/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics;

import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.metrics.creator.*;
import es.uam.eps.ir.relison.grid.diffusion.metrics.features.global.*;
import es.uam.eps.ir.relison.grid.diffusion.metrics.features.indiv.*;
import es.uam.eps.ir.relison.grid.diffusion.metrics.informationpieces.*;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

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
        MetricConfigurator<U,I,P> conf = switch (name)
        {
            case MetricIdentifiers.GINI -> new FeatureIndividualGiniComplementConfigurator<>();
            case MetricIdentifiers.GLOBALGINI -> new FeatureGlobalGiniComplementConfigurator<>();
            case MetricIdentifiers.GLOBALUSERGINI -> new FeatureGlobalUserGiniComplementConfigurator<>();
            case MetricIdentifiers.RECALL -> new FeatureRecallConfigurator<>();
            case MetricIdentifiers.EXTRATE -> new ExternalFeatureIndividualRateConfigurator<>();
            case MetricIdentifiers.GLOBALEXTRATE -> new ExternalFeatureGlobalRateConfigurator<>();
            case MetricIdentifiers.ENTROPY -> new FeatureIndividualEntropyConfigurator<>();
            case MetricIdentifiers.GLOBALENTROPY -> new FeatureGlobalEntropyConfigurator<>();
            case MetricIdentifiers.GLOBALUSERENTROPY -> new FeatureGlobalUserEntropyConfigurator<>();
            case MetricIdentifiers.EXTRECALL -> new ExternalFeatureRecallConfigurator<>();
            case MetricIdentifiers.EXTGINI -> new ExternalFeatureIndividualGiniComplementConfigurator<>();
            case MetricIdentifiers.GLOBALEXTGINI -> new ExternalFeatureGlobalGiniConfigurator<>();
            case MetricIdentifiers.KLD -> new FeatureIndividualKLDivergenceConfigurator<>();
            case MetricIdentifiers.GLOBALKLD -> new FeatureGlobalKLDivergenceConfigurator<>();
            case MetricIdentifiers.USERFEATURECOUNT -> new UserFeatureCountConfigurator<>();
            case MetricIdentifiers.USERFEATUREGINI -> new UserFeatureGiniComplementConfigurator<>();
            case MetricIdentifiers.USERSPEED -> new InformationCountConfigurator<>();
            case MetricIdentifiers.SPEED -> new SpeedConfigurator<>();
            case MetricIdentifiers.INFOGINI -> new InformationGiniComplementConfigurator<>();
            case MetricIdentifiers.REALPROPRECALL -> new RealPropagatedIndividualRecallConfigurator<>();
            case MetricIdentifiers.GLOBALREALPROPRECALL -> new RealPropagatedGlobalRecallConfigurator<>();
            case MetricIdentifiers.USERGLOBALGINI -> new CreatorGlobalGiniComplementConfigurator<>();
            case MetricIdentifiers.USERINDIVGINI -> new CreatorIndividualGiniComplementConfigurator<>();
            case MetricIdentifiers.USERRECALL -> new CreatorRecallConfigurator<>();
            case MetricIdentifiers.USERGLOBALENTROPY -> new CreatorGlobalEntropyConfigurator<>();
            case MetricIdentifiers.USERINDIVENTROPY -> new CreatorIndividualEntropyConfigurator<>();
            default -> null;
        };

        if(conf == null) return null;
        SimulationMetric<U,I,P> propagation = conf.configure(params);
        return new Tuple2<>(propagation.getName(), propagation);
    }
}
