/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.utils.indexes;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Configures a set of MonteCarlo-computed Gini coefficients.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MonteCarloGiniCollection
{
    /**
     * Size of the population
     */
    private final int populationSize;
    /**
     * Maximum income per individual.
     */
    private final int maxSizePerIndiv;
    /**
     * A list containing the different distributions for the Gini
     */
    private final List<MonteCarloGini> mcList;
    /**
     * Total frequency.
     */
    private int totalFreq;

    /**
     * Full constructor.
     *
     * @param populationSize  the population size.
     * @param maxSizePerIndiv maximum income per individual in the population (0 if there is no limit).
     * @param numMC           Number of MonteCarlo samples
     */
    public MonteCarloGiniCollection(int populationSize, int maxSizePerIndiv, int numMC)
    {
        this.populationSize = populationSize;
        this.maxSizePerIndiv = maxSizePerIndiv;
        this.mcList = new ArrayList<>();
        for (int i = 0; i < numMC; ++i)
        {
            this.mcList.add(new MonteCarloGini(populationSize, 0, maxSizePerIndiv));
        }
        this.totalFreq = 0;
    }

    public MonteCarloGiniCollection(int populationSize, int numMC)
    {
        this(populationSize, 0, numMC);
    }

    /**
     * Returns the maximum value for the Gini coefficient and the current configuration.
     *
     * @return the maximum value for the Gini coefficient and the current configuration.
     */
    public double maxValue()
    {
        if (this.totalFreq <= 0 || this.populationSize <= 1)
        {
            return Double.NaN;
        }
        if (this.maxSizePerIndiv <= 0 || this.totalFreq < this.maxSizePerIndiv) // There is no upper limit to the income, or it has not been reached.
        {
            return 1.0;
        }
        else
        {
            int coc = this.totalFreq / this.maxSizePerIndiv;
            int remainder = this.totalFreq % this.maxSizePerIndiv;

            double val = (coc + 0.0) * (this.populationSize - remainder) * this.maxSizePerIndiv;
            val += (this.populationSize - 2 * coc - 1.0) * (remainder + 0.0);
            val *= 1.0 / (this.populationSize - 1.0);
            val /= (this.totalFreq + 0.0);

            return val;
        }
    }

    /**
     * Returns the minimum value for the Gini coefficient and the current configuration.
     *
     * @return the minimum value for the Gini coefficient and the current configuration.
     */
    public double minValue()
    {
        if (this.totalFreq <= 0 || this.populationSize <= 1)
        {
            return Double.NaN;
        }
        if (this.totalFreq % this.populationSize == 0 || (this.maxSizePerIndiv > 0 && this.totalFreq > this.populationSize * this.maxSizePerIndiv))
        {
            return 0.0;
        }
        else
        {
            int num = this.totalFreq / this.populationSize + 1;
            int remainder = this.totalFreq % this.populationSize;
            int numIncreased = this.populationSize - remainder;

            double val = (num - 1.0) * (this.populationSize - numIncreased) * num;
            val += -(remainder + 0.0) * numIncreased;
            val *= 1.0 / (this.populationSize - 1.0);
            val /= (this.totalFreq + 0.0);

            return val;
        }
    }

    /**
     * Computes the average value computed by MonteCarlo.
     *
     * @return the average value.
     */
    public double averageValue()
    {
        OptionalDouble opt = this.mcList.stream().mapToDouble(MonteCarloGini::getValue).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    public void update(int newElems)
    {
        this.totalFreq += newElems;
        this.mcList.forEach(gini -> gini.update(newElems));
    }
}
