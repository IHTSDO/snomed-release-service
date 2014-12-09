package org.ihtsdo.buildcloud.service.build.transform;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDListFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CachedSctidFactoryTest {

	private MocksControl mocksControl;
	private CachedSctidFactory cachedSctidFactory;
	private IdAssignmentBI mockIdAssignmentBI;

	@Before
	public void setUp() throws Exception {
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockIdAssignmentBI = mocksControl.createMock(IdAssignmentBI.class);
		cachedSctidFactory = new CachedSctidFactory(1, "123", "456", mockIdAssignmentBI, 3, 1);
	}

	@Test
	public void testGetSCTID() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andReturn(1234L);
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

		// Verify mock object expectations
		mocksControl.verify();
		Assert.assertEquals(new Long(1234), sctid);
	}

	@Test
	public void testGetSCTIDWithNetworkIssuesWithinLimit() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andThrow(new CreateSCTIDFaultException(new NoHttpResponseException()));
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andThrow(new CreateSCTIDFaultException(new NoHttpResponseException()));
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andReturn(1234L);
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

		// Verify mock object expectations
		mocksControl.verify();
		Assert.assertEquals(new Long(1234), sctid);
	}

	@Test
	public void testGetSCTIDWithNetworkIssuesOverLimit() throws Exception {
		// Set up expectations
		// Mock objects in record mode
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andThrow(new CreateSCTIDFaultException(new NoHttpResponseException()));
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andThrow(new CreateSCTIDFaultException(new NoHttpResponseException()));
		EasyMock.expect(
				mockIdAssignmentBI.createSCTID(UUID.fromString("e568b6b6-1869-4adb-99ea-039d076f64f0"), 1, "1", "123", "456", "123"))
				.andThrow(new CreateSCTIDFaultException(new NoHttpResponseException()));
		// Switch mock objects to replay mode
		mocksControl.replay();

		// Call target method
		try {
			final Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");
			Assert.fail("Should have thrown exception");
		} catch (final CreateSCTIDFaultException e) {
			// ignore
		}

		// Verify mock object expectations
		mocksControl.verify();
	}
	
	@Test
	public void testGetSctdIdsForEmptyUUIDs() throws RemoteException, CreateSCTIDListFaultException, InterruptedException {
		final Map<String, Long> result = cachedSctidFactory.getSCTIDs( new ArrayList<String>(), "1", "123");
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testGetSctdIdsForNullUUIDList() throws RemoteException, CreateSCTIDListFaultException, InterruptedException {
		final Map<String, Long> result = cachedSctidFactory.getSCTIDs( null, "1", "123");
		Assert.assertTrue(result.isEmpty());
	}
}
