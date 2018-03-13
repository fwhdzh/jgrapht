/*
 * (C) Copyright 2018, by Timofey Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.cycle;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.jgrapht.traverse.LexBreadthFirstIterator;
import org.jgrapht.traverse.MaximumCardinalityIterator;

import java.util.*;

/**
 * Allows testing chordality of a graph. The inspected {@code graph} is specified at construction time
 * and cannot be modified. Currently chordality of a graph is tested via {@link MaximumCardinalityIterator}
 * by default. When {@link ChordalityInspector#LEX_BFS} is specified as a second constructor parameter, this
 * {@code ChordalityInspector} uses {@link LexBreadthFirstIterator} to compute perfect elimination order.
 * <p>
 * A <a href="https://en.wikipedia.org/wiki/Chordal_graph">chordal graph</a> is one in which all cycles of
 * four or more vertices have a chord, which is an edge that is not part of the cycle but connects two vertices
 * of the cycle.
 * <p>
 * A graph is chordal iff its the vertices can be arranged into a perfect elimination order. Perfect elimination
 * order isn't unique. Both maximum cardinality search and lexicographical breadth-first search produces this order.
 * <p>
 * Both lexicographical BFS and maximum cardinality search run in $\mathcal{O}(|V| + |E|)$. Checking whether given order
 * is the perfect elimination order via {@link ChordalityInspector#isPerfectEliminationOrder(List)} takes
 * $\mathcal{O}(|V| + |E|)$ as well. So, overall time complexity of the method
 * {@link ChordalityInspector#isChordal()} is $\mathcal{O}(|V| + |E|)$.
 *
 * @param <V> the graph vertex type.
 * @param <E> the graph edge type.
 * @author March 2018
 * @see LexBreadthFirstIterator
 * @see MaximumCardinalityIterator
 */
public class ChordalityInspector<V, E> {
    /**
     * Specifies {@link MaximumCardinalityIterator} as an internal iterator for this inspector.
     */
    public static final int MCS = 0;
    /**
     * Specifies {@link LexBreadthFirstIterator} as an internal iterator for this inspector.
     */
    public static final int LEX_BFS = 1;

    /** The inspected graph.*/
    private Graph<V, E> graph;
    /**
     * Contains true if the graph is chordal, otherwise false. Is null before the first call to the
     * {@link ChordalityInspector#isChordal()}.
     */
    private Boolean chordal = null;
    /**
     * Order produced by {@code orderIterator}.
     */
    private List<V> order;
    /**
     * Iterator used for producing perfect elimination order.
     */
    private AbstractGraphIterator<V, E> orderIterator;

    /**
     * Creates a chordality inspector for {@code graph}, which uses {@link MaximumCardinalityIterator}
     * as a default iterator.
     *
     * @param graph the graph for which a chordality inspector to be created.
     */
    public ChordalityInspector(Graph<V, E> graph) {
        this(graph, MCS);
    }

    /**
     * Creates a chordality inspector for {@code graph}, which uses as iterator defined by the second
     * parameter as an internal iterator.
     *
     * @param graph the graph for which a chordality inspector to be created.
     * @param algorithm the constant, which defines iterator to be used by this {@code ChordalityInspector}.
     */
    public ChordalityInspector(Graph<V, E> graph, int algorithm) {
        this.graph = Objects.requireNonNull(graph);
        if (graph.getType().isDirected()) {
            this.graph = new AsUndirectedGraph<>(graph);
        }
        if (algorithm == MCS) {
            this.orderIterator = new MaximumCardinalityIterator<>(graph);
        } else if(algorithm == LEX_BFS){
            this.orderIterator = new LexBreadthFirstIterator<>(graph);
        }else{
            throw new IllegalArgumentException("Invalid algorithm specified");
        }
    }

    /**
     * Checks whether the inspected graph is chordal.
     *
     * @return true if this graph is chordal, otherwise false.
     */
    public boolean isChordal() {
        if (chordal == null) {
            order = lazyComputeOrder();
            chordal = isPerfectEliminationOrder(order);
        }
        return chordal;
    }

    /**
     * Returns the computed vertex order. In the case the inspected graph chordal, returned order
     * is a perfect elimination order.
     *
     * @return computed vertex order.
     */
    public List<V> getOrder() {
        return lazyComputeOrder();
    }

    /**
     * Checks whether the vertices in the {@code vertexOrder} are in perfect elimination order with
     * respect to the inspected graph. Returns false, if the inspected graph isn't chordal.
     *
     * @param vertexOrder the sequence of vertices of the {@code graph}.
     * @return true if the {@code graph} is chordal and the vertices in {@code vertexOrder} are in
     * perfect elimination order, otherwise false.
     */
    public boolean isPerfectEliminationOrder(List<V> vertexOrder) {
        Set<V> graphVertices = graph.vertexSet();
        if (graphVertices.size() == vertexOrder.size() && graphVertices.containsAll(vertexOrder)) {
            Map<V, Integer> map = new HashMap<>(vertexOrder.size());
            int i = 0;
            for (V vertex : vertexOrder) {
                map.put(vertex, i);
                ++i;
            }
            return isPerfectEliminationOrder(vertexOrder, map);
        } else {
            return false;
        }
    }

    /**
     * Computes vertex order via {@code orderIterator}.
     *
     * @return computed order.
     */
    private List<V> lazyComputeOrder() {
        if(order == null){
            int vertexNum = graph.vertexSet().size();
            order = new ArrayList<>(vertexNum);
            for (int i = 0; i < vertexNum; i++) {
                order.add(orderIterator.next());
            }
        }
        return order;
    }

    /**
     * Checks whether the vertices in the {@code vertexOrder} are in perfect elimination order.
     * Returns false, if the inspected graph isn't chordal.
     *
     * @param vertexOrder the sequence of vertices of {@code graph}.
     * @param map         maps every vertex in {@code graph} to its position in {@code vertexOrder}.
     * @return true if the {@code graph} is chordal and the vertices in {@code vertexOrder} are in
     * perfect elimination order.
     */
    private boolean isPerfectEliminationOrder(List<V> vertexOrder, Map<V, Integer> map) {
        for (V vertex : vertexOrder) {
            Set<V> predecessors = getPredecessors(map, vertex);
            if (predecessors.size() > 0) {
                V maxPredecessor = Collections.max(predecessors, Comparator.comparingInt(map::get));
                for (V predecessor : predecessors) {
                    if (!predecessor.equals(maxPredecessor) && !graph.containsEdge(predecessor, maxPredecessor)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns the predecessors of {@code vertex} in the order defined by {@code map}. More precisely,
     * returns those of {@code vertex}, whose mapped index in {@code map} is less then the index of {@code vertex}.
     *
     * @param map    defines the mapping of vertices in {@code graph} to their indices in order.
     * @param vertex the vertex whose predecessors in order are to be returned.
     * @return the predecessors of {@code vertex} in order defines by {@code map}.
     */
    private Set<V> getPredecessors(Map<V, Integer> map, V vertex) {
        Set<V> predecessors = new HashSet<>();
        Integer vertexPosition = map.get(vertex);
        Set<E> edges = graph.edgesOf(vertex);
        for (E edge : edges) {
            V oppositeVertex = Graphs.getOppositeVertex(graph, edge, vertex);
            Integer destPosition = map.get(oppositeVertex);
            if (destPosition < vertexPosition) {
                predecessors.add(oppositeVertex);
            }
        }
        return predecessors;
    }
}


