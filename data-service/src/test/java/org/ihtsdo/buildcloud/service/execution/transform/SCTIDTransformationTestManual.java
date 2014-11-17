package org.ihtsdo.buildcloud.service.execution.transform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.idgeneration.IdAssignmentImpl;
import org.junit.Before;
import org.junit.Test;

public class SCTIDTransformationTestManual {

	private CachedSctidFactory cachedSctidFactory;
	private IdAssignmentBI idAssignmentBI;
	private SCTIDTransformation sctidTransformation;

	@Before
	public void setUp() throws Exception {
		idAssignmentBI = new IdAssignmentImpl("http://localhost:8080/axis2/services/id_generator"); // Point this to test if no local.
		cachedSctidFactory = new CachedSctidFactory(TransformationService.INTERNATIONAL_NAMESPACE_ID, "20150131", new Date().toString(), idAssignmentBI, 3, 10);
		sctidTransformation = new SCTIDTransformation(0, 3, "1", cachedSctidFactory);
	}

	@Test
	public void testTransformLine() throws Exception {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("id-lookup-test.txt")));
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] split = line.split("\t");
			sctidTransformation.transformLine(split);
			for (final String s : split) {
				System.out.print(s);
				System.out.print("  ");
			}
			System.out.println();
		}
		System.out.println();
	}

	@Test
	public void testTransformLines() throws Exception {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("id-lookup-test.txt")));
		String line;
		final List<String[]> lines = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			final String[] split = line.split("\t");
			lines.add(split);
		}

		sctidTransformation.transformLines(lines);

		for (final String[] strings : lines) {
			for (final String s : strings) {
				System.out.print(s);
				System.out.print("  ");
			}
			System.out.println();
		}
	}

}
