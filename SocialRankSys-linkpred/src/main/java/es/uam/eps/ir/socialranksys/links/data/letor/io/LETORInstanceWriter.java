/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.io;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;

import java.io.*;
import java.util.List;

import static es.uam.eps.ir.socialranksys.links.data.letor.io.LETORFormatConstants.*;

/**
 * Class for writing patterns in the LETOR format (for Learning TO Rank task).
 * Format: <br/>
 *
 * #featId1: description1 stats1 <br/>
 * #featId2: description2 stats2 <br/>
 * ... <br/>
 * #featIdN: descriptionN statsN <br/>
 * [relevance(u,v) or class1(u,v)] qid:[user u] [featId1]:[value1(u,v)] [featId2]:[value2(u,v)] ... [featIdN]:[valueN(u,v)] #docId=[user v] <br/>
 * ... <br/>
 * [relevance(u,v) or class1(u,v)] qid:[user u] [featId1]:[value1(u,v)] [featId2]:[value2(u,v)] ... [featIdN]:[valueN(u,v)] #docId=[user v] <br/>
 *
 * @see <a href="https://www.microsoft.com/en-us/research/project/letor-learning-rank-information-retrieval/#!letor-4-0">LETOR v.4.0</a>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> type of the users.
 */
public class LETORInstanceWriter<U> implements InstanceSetWriter<U>
{
    /**
     * True if comments are allowed, false otherwise.
     */
    private final boolean comments;

    /**
     * Constructor. Adds comments.
     */
    public LETORInstanceWriter()
    {
        this.comments = true;
    }

    /**
     * Constructor.
     * @param comments true to add comments, false otherwise.
     */
    public LETORInstanceWriter(boolean comments)
    {
        this.comments = comments;
    }

    @Override
    public void write(String file, InstanceSet<U> patternSet) throws IOException
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            FeatureInformation info = patternSet.getFeatInfo();

            String featInfo = this.writeFeatureInfo(info);
            if(comments)
                bw.write(featInfo);

            patternSet.getAllInstances().forEach(pattern ->
            {
                try
                {
                    bw.write("\n" + this.write(pattern));
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            });
        }
    }

    @Override
    public String writeFeatureInfo(FeatureInformation featInfo)
    {
        StringBuilder builder = new StringBuilder();
        int numFeats = featInfo.numFeats();
        for(int i = 0; i < numFeats-1; ++i)
        {
            builder.append(writeFeatureInfo(featInfo, i));
            builder.append("\n");
        }
        builder.append(writeFeatureInfo(featInfo,numFeats-1));
        return builder.toString();
    }

    @Override
    public void write(Writer writer, FeatureInformation featInfo) throws IOException
    {
        writer.write(this.writeFeatureInfo(featInfo));
    }

    @Override
    public void write(Writer writer, Instance<U> instance) throws IOException
    {
        writer.write("\n" + this.write(instance));
    }

    private String writeFeatureInfo(FeatureInformation featInfo, int i)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(COMMENT);
        builder.append(i+1);
        builder.append(": ");
        builder.append(featInfo.getFeatureDescription(i));
        builder.append(" ");
        builder.append(featInfo.getFeatureType(i));
        builder.append(" ");
        Stats stats = featInfo.getStats(i);
        builder.append(" min: ");
        builder.append(stats.getMin());
        builder.append(" max: ");
        builder.append(stats.getMax());
        builder.append(" avg: ");
        builder.append(stats.getMean());
        builder.append(" std: ");
        builder.append(stats.getStandardDeviation());

        return builder.toString();
    }

    @Override
    public String write(Instance<U> pattern)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(pattern.getCategory());
        builder.append(SEPARATOR);
        builder.append(QID);
        builder.append(pattern.getOrigin());
        List<Double> values = pattern.getValues();
        int length = values.size();
        for(int i = 0; i < length; ++i)
        {
            if(comments)
            {
                builder.append(SEPARATOR);
                builder.append(i + 1);
                builder.append(IDSEP);
                builder.append(values.get(i));
            }
            else if(values.get(i) != 0.0)
            {
                builder.append(SEPARATOR);
                builder.append(i + 1);
                builder.append(IDSEP);
                builder.append(values.get(i).intValue());
            }

        }

        if(comments)
        {
            builder.append(SEPARATOR);
            builder.append(COMMENT);
            builder.append(DOCID);
            builder.append(pattern.getDest());
        }
        else
        {
            builder.append(COMMENT);
            builder.append(DOCID);
            builder.append(pattern.getDest());
        }
        return builder.toString();
    }
}
