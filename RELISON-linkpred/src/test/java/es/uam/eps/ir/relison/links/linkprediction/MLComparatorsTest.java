package es.uam.eps.ir.relison.links.linkprediction;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.junit.Test;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class MLComparatorsTest
{
    @Test
    public void preservesSeededTieOrderInBothDirections()
    {
        List<Integer> nodes = Arrays.asList(0, 1, 2, 3, 4, 2);
        List<Integer> shuffled = new ArrayList<>(nodes);
        Collections.shuffle(shuffled, new Random(0));
        List<Tuple2od<Pair<Integer>>> links = new ArrayList<>();
        for (int u = 0; u < 7; u++)
            for (int v = 0; v < 7; v++)
                for (double score : new double[]{0.0, -0.0, 1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN})
                    links.add(new Tuple2od<>(new Pair<>(u, v), score));
        Comparator<Tuple2od<Pair<Integer>>> legacy = (a, b) -> {
            int value = Double.compare(b.v2, a.v2);
            if (value == 0) value = shuffled.indexOf(b.v1.v1()) - shuffled.indexOf(a.v1.v1());
            if (value == 0) value = shuffled.indexOf(b.v1.v2()) - shuffled.indexOf(a.v1.v2());
            return Integer.signum(value);
        };
        Comparator<Tuple2od<Pair<Integer>>> descending = MLComparators.descendingComparator(nodes);
        Comparator<Tuple2od<Pair<Integer>>> ascending = MLComparators.ascendingComparator(nodes);
        for (Tuple2od<Pair<Integer>> a : links)
            for (Tuple2od<Pair<Integer>> b : links)
            {
                assertEquals(legacy.compare(a, b), descending.compare(a, b));
                assertEquals(legacy.compare(b, a), ascending.compare(a, b));
            }
    }
}
