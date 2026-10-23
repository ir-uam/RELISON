/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Tidy ordered tree/forest drawing using the Buchheim-Juenger-Leipert refinement
 * of Walker's algorithm (Graph Drawing 2002, DOI 10.1007/3-540-36151-0_32).
 * Directed inputs must be outward forests with at most one distinct parent per
 * node. Undirected inputs must be acyclic. Self-loops are rejected; parallel
 * edges are collapsed. An invisible parent arranges forest roots on one level.
 * Walks use explicit stacks, so deep trees do not consume the Java call stack.
 * Pins override the tidy positions and may violate spacing or centring.
 * @param <U> node type
 */
public final class TreeLayout<U> extends AbstractStaticLayout<U>
{
    private final U root;
    private final double siblingSpacing, levelSpacing;

    /** Chooses roots from topology/request order, with unit spacing. */
    public TreeLayout() { this(null, 1, 1); }

    /**
     * @param root preferred root, or null for automatic roots; a directed root must have no parent
     * @param siblingSpacing minimum horizontal node separation at each level
     * @param levelSpacing vertical separation between levels
     */
    public TreeLayout(U root, double siblingSpacing, double levelSpacing)
    {
        super("tree", "Tidy tree / forest");
        this.root = root;
        this.siblingSpacing = positive(siblingSpacing, "siblingSpacing");
        this.levelSpacing = positive(levelSpacing, "levelSpacing");
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        int size = graph.getNodes().size();
        int selected = root == null ? -1 : graph.indexOf(root);
        if (root != null && selected < 0) throw new IllegalArgumentException("Tree root must exist in the graph");
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        if (size == 0) return positions;
        Node virtual = buildForest(graph, selected);
        firstWalk(virtual);
        double origin = virtual.children.get(0).preliminary + virtual.modifier;
        Deque<SecondFrame> stack = new ArrayDeque<>();
        stack.push(new SecondFrame(virtual, 0, -1));
        while (!stack.isEmpty())
        {
            SecondFrame frame = stack.pop();
            Node node = frame.node;
            if (node.index >= 0)
                positions.put(graph.getNodes().get(node.index), new Pair<>(node.preliminary + frame.modifier - origin, frame.depth * levelSpacing));
            for (int i = node.children.size() - 1; i >= 0; i--)
                stack.push(new SecondFrame(node.children.get(i), frame.modifier + node.modifier, frame.depth + 1));
        }
        return positions;
    }

    private Node buildForest(IndexedGraphSnapshot<U> graph, int selected)
    {
        int[][] adjacent = Topology.neighbours(graph, graph.isDirected() ? EdgeOrientation.OUT : EdgeOrientation.UND);
        Node[] nodes = new Node[adjacent.length];
        for (int i = 0; i < nodes.length; i++) nodes[i] = new Node(i);
        Node virtual = new Node(-1);
        int[] parents = new int[nodes.length];
        Arrays.fill(parents, -1);
        if (graph.isDirected())
        {
            for (int source = 0; source < nodes.length; source++)
                for (int target : adjacent[source])
                {
                    if (source == target) throw new IllegalArgumentException("Tree layouts do not accept self-loops");
                    if (parents[target] >= 0) throw new IllegalArgumentException("Directed tree nodes may have only one parent");
                    parents[target] = source;
                    attach(nodes[source], nodes[target]);
                }
            if (selected >= 0 && parents[selected] >= 0)
                throw new IllegalArgumentException("A directed tree root must have no parent");
            if (selected >= 0) attach(virtual, nodes[selected]);
            for (int i = 0; i < nodes.length; i++)
                if (parents[i] < 0 && i != selected) attach(virtual, nodes[i]);
            int visited = 0;
            Deque<Node> queue = new ArrayDeque<>(virtual.children);
            while (!queue.isEmpty()) { Node node = queue.remove(); visited++; queue.addAll(node.children); }
            if (visited != nodes.length) throw new IllegalArgumentException("Tree layouts require an acyclic graph");
        }
        else
        {
            boolean[] visited = new boolean[nodes.length];
            List<Integer> starts = new ArrayList<>();
            if (selected >= 0) starts.add(selected);
            for (int i = 0; i < nodes.length; i++) if (i != selected) starts.add(i);
            Deque<Integer> queue = new ArrayDeque<>();
            for (int start : starts)
            {
                if (visited[start]) continue;
                attach(virtual, nodes[start]);
                visited[start] = true;
                queue.add(start);
                while (!queue.isEmpty())
                {
                    int source = queue.remove();
                    for (int target : adjacent[source])
                    {
                        if (target == source) throw new IllegalArgumentException("Tree layouts do not accept self-loops");
                        if (target == parents[source]) continue;
                        if (visited[target]) throw new IllegalArgumentException("Tree layouts require an acyclic graph");
                        visited[target] = true;
                        parents[target] = source;
                        attach(nodes[source], nodes[target]);
                        queue.add(target);
                    }
                }
            }
        }
        return virtual;
    }

    private static void attach(Node parent, Node child)
    {
        child.parent = parent;
        child.number = parent.children.size();
        parent.children.add(child);
    }

    private void firstWalk(Node rootNode)
    {
        Deque<FirstFrame> stack = new ArrayDeque<>();
        stack.push(new FirstFrame(rootNode));
        while (!stack.isEmpty())
        {
            FirstFrame frame = stack.peek();
            Node node = frame.node;
            if (frame.nextChild < node.children.size())
            {
                Node child = node.children.get(frame.nextChild);
                if (!frame.waiting)
                {
                    frame.waiting = true;
                    stack.push(new FirstFrame(child));
                }
                else
                {
                    frame.ancestor = apportion(child, frame.ancestor);
                    frame.nextChild++;
                    frame.waiting = false;
                }
                continue;
            }
            Node left = previous(node);
            if (node.children.isEmpty()) node.preliminary = left == null ? 0 : left.preliminary + siblingSpacing;
            else
            {
                executeShifts(node);
                double middle = node.children.get(0).preliminary / 2 + node.children.get(node.children.size() - 1).preliminary / 2;
                node.preliminary = left == null ? middle : left.preliminary + siblingSpacing;
                node.modifier = left == null ? 0 : node.preliminary - middle;
            }
            stack.pop();
        }
    }

    private Node apportion(Node node, Node defaultAncestor)
    {
        Node sibling = previous(node);
        if (sibling == null) return defaultAncestor;
        Node innerRight = node, outerRight = node, innerLeft = sibling, outerLeft = node.parent.children.get(0);
        double sumInnerRight = node.modifier, sumOuterRight = node.modifier;
        double sumInnerLeft = sibling.modifier, sumOuterLeft = outerLeft.modifier;
        while (nextRight(innerLeft) != null && nextLeft(innerRight) != null)
        {
            innerLeft = nextRight(innerLeft);
            innerRight = nextLeft(innerRight);
            outerLeft = nextLeft(outerLeft);
            outerRight = nextRight(outerRight);
            outerRight.ancestor = node;
            double shift = innerLeft.preliminary + sumInnerLeft - innerRight.preliminary - sumInnerRight + siblingSpacing;
            if (shift > 0)
            {
                Node ancestor = innerLeft.ancestor.parent == node.parent ? innerLeft.ancestor : defaultAncestor;
                moveSubtree(ancestor, node, shift);
                sumInnerRight += shift;
                sumOuterRight += shift;
            }
            sumInnerLeft += innerLeft.modifier;
            sumInnerRight += innerRight.modifier;
            sumOuterLeft += outerLeft.modifier;
            sumOuterRight += outerRight.modifier;
        }
        if (nextRight(innerLeft) != null && nextRight(outerRight) == null)
        {
            outerRight.thread = nextRight(innerLeft);
            outerRight.modifier += sumInnerLeft - sumOuterRight;
        }
        if (nextLeft(innerRight) != null && nextLeft(outerLeft) == null)
        {
            outerLeft.thread = nextLeft(innerRight);
            outerLeft.modifier += sumInnerRight - sumOuterLeft;
            defaultAncestor = node;
        }
        return defaultAncestor;
    }

    private static void moveSubtree(Node left, Node right, double shift)
    {
        double share = shift / (right.number - left.number);
        right.change -= share;
        right.shift += shift;
        left.change += share;
        right.preliminary += shift;
        right.modifier += shift;
    }

    private static void executeShifts(Node node)
    {
        double shift = 0, change = 0;
        for (int i = node.children.size() - 1; i >= 0; i--)
        {
            Node child = node.children.get(i);
            child.preliminary += shift;
            child.modifier += shift;
            change += child.change;
            shift += child.shift + change;
        }
    }

    private static Node previous(Node node) { return node.parent == null || node.number == 0 ? null : node.parent.children.get(node.number - 1); }
    private static Node nextLeft(Node node) { return node.children.isEmpty() ? node.thread : node.children.get(0); }
    private static Node nextRight(Node node) { return node.children.isEmpty() ? node.thread : node.children.get(node.children.size() - 1); }

    private static final class Node
    {
        final int index;
        final List<Node> children = new ArrayList<>();
        Node parent, thread, ancestor;
        int number;
        double preliminary, modifier, change, shift;
        Node(int index) { this.index = index; ancestor = this; }
    }

    private static final class FirstFrame
    {
        final Node node;
        Node ancestor;
        int nextChild;
        boolean waiting;
        FirstFrame(Node node) { this.node = node; ancestor = node.children.isEmpty() ? null : node.children.get(0); }
    }

    private static final class SecondFrame
    {
        final Node node;
        final double modifier;
        final int depth;
        SecondFrame(Node node, double modifier, int depth) { this.node = node; this.modifier = modifier; this.depth = depth; }
    }
}
