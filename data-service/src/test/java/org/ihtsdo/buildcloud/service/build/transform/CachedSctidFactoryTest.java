package org.ihtsdo.buildcloud.service.build.transform;

import org.apache.commons.httpclient.NoHttpResponseException;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

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
		Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

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
		Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");

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
			Long sctid = cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123");
			Assert.fail("Should have thrown exception");
		} catch (CreateSCTIDFaultException e) {
			// ignore
		}

		// Verify mock object expectations
		mocksControl.verify();
	}

}
