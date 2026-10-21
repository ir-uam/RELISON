package es.uam.eps.ir.relison.links.linkprediction;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.rec.AbstractRecommender;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.junit.Test;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class RecommendationLinkPredictorTest
{
    private final FastGraph<Integer> graph = new FastDirectedUnweightedGraph<>();

    public RecommendationLinkPredictorTest()
    {
        for (int u = 0; u < 12; u++) graph.addNode(u);
        for (int u = 0; u < 11; u++) graph.addEdge(u, u + 1);
    }

    @Test
    public void matchesFullRankingForDifferentCutoffsScoresAndFilters()
    {
        List<Integer> nodes = graph.getAllNodes().collect(Collectors.toList());
        Predicate<Pair<Integer>> filter = p -> !p.v1().equals(p.v2())
                && !graph.containsEdge(p.v1(), p.v2()) && !graph.containsEdge(p.v2(), p.v1());
        for (int seed = 0; seed < 8; seed++)
        {
            Recommender<Integer, Integer> rec = recommender(seed, false);
            Comparator<Tuple2od<Pair<Integer>>> comparator = MLComparators.descendingComparator(nodes);
            List<Tuple2od<Pair<Integer>>> expected = fullRanking(rec, comparator, filter);
            RecommendationLinkPredictor<Integer> predictor = new RecommendationLinkPredictor<>(graph, rec);
            for (int cutoff : new int[]{0, 1, 2, 10, 50, 200, Integer.MAX_VALUE})
            {
                assertRanking(expected.subList(0, Math.min(cutoff, expected.size())),
                        predictor.getPrediction(cutoff, filter).getPrediction());
            }
            assertRanking(expected, predictor.getPrediction(filter).getPrediction());
        }
    }

    @Test
    public void preservesCustomOrderingAndComparatorUniqueness()
    {
        List<Integer> nodes = graph.getAllNodes().collect(Collectors.toList());
        List<Comparator<Tuple2od<Pair<Integer>>>> comparators = Arrays.asList(
                MLComparators.ascendingComparator(nodes), Comparator.comparingDouble(t -> t.v2));
        Recommender<Integer, Integer> rec = recommender(3, false);
        for (Comparator<Tuple2od<Pair<Integer>>> comparator : comparators)
        {
            List<Tuple2od<Pair<Integer>>> expected = fullRanking(rec, comparator, p -> true);
            RecommendationLinkPredictor<Integer> predictor = new RecommendationLinkPredictor<>(graph, comparator, rec);
            for (int cutoff : new int[]{1, 3, 10, Integer.MAX_VALUE})
                assertRanking(expected.subList(0, Math.min(cutoff, expected.size())),
                        predictor.getPrediction(cutoff).getPrediction());
        }
    }

    @Test
    public void retainsMissingScoresAndHandlesEmptyCandidates()
    {
        Recommender<Integer, Integer> rec = recommender(0, true);
        RecommendationLinkPredictor<Integer> predictor = new RecommendationLinkPredictor<>(graph, rec);
        List<Tuple2od<Pair<Integer>>> expected = fullRanking(rec,
                MLComparators.descendingComparator(graph.getAllNodes().collect(Collectors.toList())), p -> true);
        assertRanking(expected.subList(0, 7), predictor.getPrediction(7).getPrediction());
        assertEquals(Double.NEGATIVE_INFINITY, predictor.getPrediction(1).getPrediction().get(0).v2, 0.0);
        assertTrue(predictor.getPrediction(10, p -> false).getPrediction().isEmpty());
        assertTrue(new RecommendationLinkPredictor<>(new FastDirectedUnweightedGraph<Integer>(), rec)
                .getPrediction(10).getPrediction().isEmpty());
    }

    @Test
    public void rejectsNegativeCutoffAndSkipsScoringForZero()
    {
        RecommendationLinkPredictor<Integer> predictor = new RecommendationLinkPredictor<>(graph, null);
        assertThrows(IllegalArgumentException.class, () -> predictor.getPrediction(-1));
        assertTrue(predictor.getPrediction(0).getPrediction().isEmpty());
    }

    @Test
    public void honoursCancellationWhileRetainingCandidates()
    {
        ComputationMonitor monitor = new ComputationMonitor()
        {
            private int checks;
            @Override public void progress(String phase, long completed, long total, String unit) { }
            @Override public void checkCancelled()
            {
                if (++checks == 5) throw new CancellationException();
            }
        };
        try (ComputationMonitor.Scope ignored = ComputationMonitor.open(monitor))
        {
            RecommendationLinkPredictor<Integer> predictor = new RecommendationLinkPredictor<>(graph, recommender(0, false));
            assertThrows(CancellationException.class, () -> predictor.getPrediction(3));
        }
    }

    private Recommender<Integer, Integer> recommender(int seed, boolean omitAll)
    {
        Random random = new Random(seed);
        double[][] scores = new double[12][12];
        double[] values = {0.0, -0.0, 1.0, -1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN};
        for (double[] row : scores)
            for (int i = 0; i < row.length; i++) row[i] = values[random.nextInt(values.length)];
        return new AbstractRecommender<Integer, Integer>()
        {
            @Override public Recommendation<Integer, Integer> getRecommendation(Integer u, Stream<Integer> candidates)
            {
                List<Tuple2od<Integer>> items = candidates.filter(v -> !omitAll && (u + v) % 4 != 0)
                        .map(v -> new Tuple2od<>(v, scores[u][v])).collect(Collectors.toList());
                // Repeated results must not fill multiple slots in the bounded ranking.
                if (!items.isEmpty()) items.add(items.get(0));
                return new Recommendation<>(u, items);
            }
            @Override public Recommendation<Integer, Integer> getRecommendation(Integer u, int maxLength, Predicate<Integer> filter)
            {
                return getRecommendation(u, graph.getAllNodes().filter(filter).limit(maxLength));
            }
        };
    }

    /** Reference implementation: construct the complete sorted set before truncating. */
    private List<Tuple2od<Pair<Integer>>> fullRanking(Recommender<Integer, Integer> rec,
            Comparator<Tuple2od<Pair<Integer>>> comparator, Predicate<Pair<Integer>> filter)
    {
        SortedSet<Tuple2od<Pair<Integer>>> all = new TreeSet<>(comparator);
        graph.getAllNodes().forEach(u -> {
            Set<Integer> candidates = graph.getAllNodes().filter(v -> filter.test(new Pair<>(u, v)))
                    .collect(Collectors.toCollection(HashSet::new));
            for (Tuple2od<Integer> score : rec.getRecommendation(u, candidates.stream()).getItems())
            {
                all.add(new Tuple2od<>(new Pair<>(u, score.v1), score.v2));
                candidates.remove(score.v1);
            }
            candidates.forEach(v -> all.add(new Tuple2od<>(new Pair<>(u, v), Double.NEGATIVE_INFINITY)));
        });
        return new ArrayList<>(all);
    }

    private void assertRanking(List<Tuple2od<Pair<Integer>>> expected, List<Tuple2od<Pair<Integer>>> actual)
    {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            assertEquals(expected.get(i).v1, actual.get(i).v1);
            assertEquals(0, Double.compare(expected.get(i).v2, actual.get(i).v2));
        }
    }
}
