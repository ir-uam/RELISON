/*package es.uam.eps.ir.socialranksys.ecir2019;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialnetwork.metrics.graph.aggregate.AggregateVertexMetric;
import es.uam.eps.ir.socialnetwork.metrics.vertex.Degree;
import es.uam.eps.ir.socialnetwork.metrics.vertex.Distance2Degree;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;

import java.util.HashMap;
import java.util.Map;

import static es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation.*;
import static org.ranksys.formats.parsing.Parsers.lp;

public class DistanceDegreeCheck
{
    public static void main(String[] args)
    {
        String trainDataPath = args[0];
        boolean directed = args[1].equalsIgnoreCase("true");

        TextGraphReader<Long> greader = new TextGraphReader<>(directed, false, true, "\t", lp);
        Graph<Long> auxgraph = greader.read(trainDataPath, false, false);
        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxgraph);

        EdgeOrientation[] orientations = new EdgeOrientation[]{IN,OUT,UND};
        if(directed)
        {
            System.out.println("Orientation\tAverage Degree * Average Degree\tAverage Distance Two Degree");
            Map<EdgeOrientation, Double> values = new HashMap<>();
            for(EdgeOrientation orient : orientations)
            {
                GraphMetric<Long> average = new AggregateVertexMetric<>(new Degree<>(orient));
                double averageValue = average.compute(graph);
                values.put(orient, averageValue);
            }

            for(EdgeOrientation orient : orientations)
            {
                double averageValue1 = values.get(orient);

                for(EdgeOrientation orient2: orientations)
                {
                    double averageValue2 = values.get(orient2);
                    GraphMetric<Long> metric = new AggregateVertexMetric<>(new Distance2Degree<>(orient, orient2));
                    double value = metric.compute(graph);

                    System.out.println(orient + "-" + orient2 + "\t" + (averageValue1*averageValue2) + "\t" + value);
                }
            }
        }


    }
}
*/