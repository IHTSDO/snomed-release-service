package org.ihtsdo.buildcloud.core.service.build.transform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.transform.CachedSctidFactory;
import org.ihtsdo.buildcloud.core.service.build.transform.SCTIDTransformation;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClientImpl;
import org.junit.Before;
import org.junit.Test;

public class SCTIDTransformationTestManual {

	private CachedSctidFactory cachedSctidFactory;
	private IdServiceRestClient idRestClient;
	private SCTIDTransformation sctidTransformation;
	private final String url = "http://162.243.20.236:3000/api";
	private final String userName = "userName";
	private final String password = "password";

	@Before
	public void setUp() throws Exception {
		idRestClient = new IdServiceRestClientImpl(url, userName, password);
		idRestClient.logIn();
		cachedSctidFactory = new CachedSctidFactory(RF2Constants.INTERNATIONAL_NAMESPACE_ID, "20150131", new Date().toString(), idRestClient, 3, 10);
		sctidTransformation = new SCTIDTransformation(0, 3, "00", cachedSctidFactory);
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
