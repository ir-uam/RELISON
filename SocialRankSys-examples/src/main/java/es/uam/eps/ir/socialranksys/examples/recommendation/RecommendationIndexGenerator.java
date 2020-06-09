package es.uam.eps.ir.socialranksys.examples.recommendation;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecommendationIndexGenerator
{
    public static void main(String[] args)
    {
        if(args.length < 2)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("\tGraph: File containing the graph");
            System.err.println("\tOutput file: File for storing the index");
        }

        TextGraphReader<Long> greader = new TextGraphReader<>(true, false, false, "\t", Parsers.lp);
        Graph<Long> graph = greader.read(args[0]);
        if(graph == null)
        {
            System.err.println("ERROR: Invalid file " + args[0]);
        }

        List<Long> users = graph.getAllNodes().sorted().collect(Collectors.toCollection(ArrayList::new));

        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]))))
        {
            for(long user : users)
            {
                bw.write(user + "\n");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: Something failed while creating the index at " + args[1]);
        }
    }
}
