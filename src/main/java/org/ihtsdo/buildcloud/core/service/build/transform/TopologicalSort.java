package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.*;

/**
 * Thanks to yongouyang for the implementation of sorting logic.
 * http://yongouyang.blogspot.co.uk/2013/03/sorting-objects-with-one-way-dependency.html
 */
public class TopologicalSort {

	public static <T> List<T> sort(final DirectedGraph<T> graph) {
		final DirectedGraph<T> reversedGraph = reverseGraph(graph);

		final List<T> result = new ArrayList<T>();
		final Set<T> visited = new HashSet<T>();

        /* We'll also maintain a third set consisting of all nodes that have
		 * been fully expanded.  If the graph contains a cycle, then we can
         * detect this by noting that a node has been explored but not fully
         * expanded.
         */
		final Set<T> expanded = new HashSet<T>();

		// Fire off a Depth-First Search from each node in the graph
		for (final T node : reversedGraph) {
			explore(node, reversedGraph, result, visited, expanded);
		}

		return result;
	}


	/**
	 * Recursively performs a Depth-First Search from the specified node, marking all nodes
	 * encountered by the search.
	 *
	 * @param node     The node to begin the search from.
	 * @param graph    The graph in which to perform the search.
	 * @param result   A list holding the topological sort of the graph.
	 * @param visited  A set of nodes that have already been visited.
	 * @param expanded A set of nodes that have been fully expanded.
	 */
	private static <T> void explore(final T node, final DirectedGraph<T> graph, final List<T> result, final Set<T> visited, final Set<T> expanded) {
		if (visited.contains(node)) {
			// if this node has already been expanded, then it's already been assigned a
			// position in the final topological sort and we don't need to explore it again.
			if (expanded.contains(node)) {
				return;
			}

			// if it hasn't been expanded, it means that we've just found a node that is currently being explored,
			// and therefore is part of a cycle.  In that case, we should report an error.
			throw new IllegalArgumentException("A cycle was detected within the Graph when exploring node " + node.toString());
		}

		visited.add(node);

		// recursively explore all predecessors of this node
		for (final T predecessor : graph.edgesFrom(node)) {
			explore(predecessor, graph, result, visited, expanded);
		}

		result.add(node);
		expanded.add(node);
	}

	private static <T> DirectedGraph<T> reverseGraph(final DirectedGraph<T> graph) {
		final DirectedGraph<T> result = new DirectedGraph<T>();

		// Add all the nodes from the original graph
		for (final T node : graph) {
			result.addNode(node);
		}

		// Scan over all the edges in the graph, adding their reverse to the reverse graph.
		for (final T node : graph) {
			for (final T endpoint : graph.edgesFrom(node)) {
				result.addEdge(endpoint, node);
			}
		}

		return result;
	}


	public static class DirectedGraph<T> implements Iterable<T> {

		// key is a Node, value is a set of Nodes connected by outgoing edges from the key
		private final Map<T, Set<T>> graph = new HashMap<T, Set<T>>();

		public boolean addNode(final T node) {
			if (graph.containsKey(node)) {
				return false;
			}

			graph.put(node, new HashSet<T>());
			return true;
		}

		public void addNodes(final Collection<T> nodes) {
			for (final T node : nodes) {
				addNode(node);
			}
		}

		public void addEdge(final T src, final T dest) {
			validateSourceAndDestinationNodes(src, dest);

			// Add the edge by adding the dest node into the outgoing edges
			graph.get(src).add(dest);
		}

		public void removeEdge(final T src, final T dest) {
			validateSourceAndDestinationNodes(src, dest);

			graph.get(src).remove(dest);
		}

		public boolean edgeExists(final T src, final T dest) {
			validateSourceAndDestinationNodes(src, dest);

			return graph.get(src).contains(dest);
		}

		public Set<T> edgesFrom(final T node) {
			// Check that the node exists.
			final Set<T> edges = graph.get(node);
			if (edges == null) {
				throw new NoSuchElementException("Source node does not exist.");
			}

			return Collections.unmodifiableSet(edges);
		}

		@Override
		public Iterator<T> iterator() {
			return graph.keySet().iterator();
		}

		public int size() {
			return graph.size();
		}

		public boolean isEmpty() {
			return graph.isEmpty();
		}

		private void validateSourceAndDestinationNodes(final T src, final T dest) {
			// Confirm both endpoints exist
			if (!graph.containsKey(src) || !graph.containsKey(dest)) {
				throw new NoSuchElementException("Both nodes must be in the graph.");
			}
		}
	}
}