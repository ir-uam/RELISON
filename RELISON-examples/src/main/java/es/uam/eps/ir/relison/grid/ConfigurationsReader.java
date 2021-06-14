/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for reading parameters from a YAML file. It receives a list of parameter configurations,
 * which are separately read. The file should look as it follows:
 *
 * <br>
 * - param1:<br>
 *      type: value_of_type<br>
 *      value: value<br>
 *   param2:<br>
 *      ...<br>
 *   paramN:<br>
 *      type: value_of_type<br>
 *      value: value<br>
 * - param1:<br>
 *      ....<br>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class ConfigurationsReader
{
    /**
     * Reads the possible values for the parameters of an algorithm.
     * @param configurations    a list of configurations, read from a YAML file.
     * @param paramReader       a parameter reader.
     * @return the list of configurations.
     */
    protected Configurations readConfigurationGrid(List<Object> configurations, ParametersReader paramReader)
    {
        List<Parameters> configs = new ArrayList<>();

        if(configurations.isEmpty())
            configs.add(new Parameters());

        for (Object configuration : configurations)
        {
            Map<String, Object> config = (Map<String, Object>) configuration;
            Parameters params = paramReader.readParameterValues(config);
            configs.add(params);
        }
        
        return new Configurations(configs);
    }
}
