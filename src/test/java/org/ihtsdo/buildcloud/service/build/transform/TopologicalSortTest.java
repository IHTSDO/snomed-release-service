package org.ihtsdo.buildcloud.service.build.transform;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.service.build.transform.TopologicalSort.DirectedGraph;
import org.junit.Test;

public class TopologicalSortTest {
	
	@Test
	public void testSortingSourceAndDestinationIds() {
		final Map<String, String> sctIdAnParentMap = new HashMap<>();
		sctIdAnParentMap.put("uuid1","sct1");
		sctIdAnParentMap.put("uuid2","uuid1");
		sctIdAnParentMap.put("uuid3","uuid2");
		sctIdAnParentMap.put("uuid4","uuid3");
		sctIdAnParentMap.put("uuid5","uuid4");
		final DirectedGraph<String> graph = new DirectedGraph<String>();
		for (final String key : sctIdAnParentMap.keySet()) {
			final String parent = sctIdAnParentMap.get(key);
			graph.addNode(parent);
			graph.addNode(key);
			graph.addEdge(parent, key);
		}
		final List<String> result = TopologicalSort.sort(graph);
		
		assertEquals(6,result.size());
		assertEquals("sct1", result.get(0));
		assertEquals("uuid1", result.get(1));
		assertEquals("uuid2", result.get(2));
		assertEquals("uuid3", result.get(3));
		assertEquals("uuid4", result.get(4));
		assertEquals("uuid5", result.get(5));
	}

}
