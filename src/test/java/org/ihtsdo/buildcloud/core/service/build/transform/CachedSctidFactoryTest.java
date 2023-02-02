package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CachedSctidFactoryTest {

	private MocksControl mocksControl;
	private CachedSctidFactory cachedSctidFactory;
	private IdServiceRestClient mockIdRestClient;

	@BeforeEach
	public void setUp() throws Exception {
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockIdRestClient = mocksControl.createMock(IdServiceRestClient.class);
		cachedSctidFactory = new CachedSctidFactory(1, "123", "456", mockIdRestClient,1,20);
	}

	@Test
	@Disabled
	public void testGetSCTID() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "testing"))
				.andReturn(1234L);
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

		// Verify mock object expectations
		mocksControl.verify();
		assertEquals(Long.valueOf(1234), sctid);
	}

	@Test
	@Disabled
	public void testGetSCTIDWithNetworkIssuesWithinLimit() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"));
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"));
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"))
				.andReturn(1234L);
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

		// Verify mock object expectations
		mocksControl.verify();
		assertEquals(Long.valueOf(1234), sctid);
	}

	@Test
	@Disabled
	public void testGetSCTIDWithNetworkIssuesOverLimit() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"));
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"));
		EasyMock.expect(
				mockIdRestClient.getOrCreateSctId(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123"));
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		
		final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");
		fail("Should have thrown exception");
		// Verify mock object expectations
		mocksControl.verify();
	}
	
	@Test
	public void testGetSctdIdsForEmptyUUIDs() throws Exception {
		final Map<String, Long> result = cachedSctidFactory.getSCTIDs( new ArrayList<String>(), "1", "123");
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetSctdIdsForNullUUIDList() throws Exception {
		final Map<String, Long> result = cachedSctidFactory.getSCTIDs( null, "1", "123");
		assertTrue(result.isEmpty());
	}
}
