package com.termmed.genid;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import com.termmed.genid.data.ConidMap;
import com.termmed.genid.data.SctIdBase;
import com.termmed.genid.data.SctIdIdentifier;
import com.termmed.genid.util.MyBatisUtil;

public class SctIdGenImpl implements SctIdGen {

	private static final Logger log = Logger.getLogger(SctIdGenImpl.class);
	private SqlSession session = null;

	public SctIdGenImpl(SqlSession session) {
		super();
		this.session = session;
	}

	public SctIdGenImpl() {
		super();
		this.session = MyBatisUtil.getSessionFactory().openSession(false);
	}

	public void commitSession() {
		session.commit();
	}

	public void rollbackSession() {
		session.rollback();
	}

	public void closeSession() {
		session.close();
	}

	@Override
	public Long getSCTID(String componentUuid) throws Exception {
		Object result = session.selectOne("com.termmed.genid.data.SctIdIdentifierMapper.getSctIdByComponentUuid", componentUuid);
		return (Long) result;
	}

	@Override
	public String getSNOMEDID(String componentUuid) throws Exception {
		return (String) session.selectOne("com.termmed.genid.data.ConidMapMapper.getSnomedIdByComponentUuid", componentUuid);
	}

	@Override
	public String getCTV3ID(String componentUuid) throws Exception {
		return (String) session.selectOne("com.termmed.genid.data.ConidMapMapper.getCtv3idByComponentUuid", componentUuid);
	}

	@Override
	public HashMap<String, Long> getSCTIDList(List<String> componentUuidList) throws Exception {
		HashMap<String, Long> result = new HashMap<String, Long>();
		for (String uuid : componentUuidList) {
			result.put(uuid, getSCTID(uuid));
		}
		return result;
	}

	@Override
	public Long createSCTID(String componentUuid, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws Exception {

		long startTime = System.currentTimeMillis();

		Long result = null;

		long num;
		long numTmp;
		long multip = 100;
		String nameSpaceChar;

		if (namespaceId > 0) {
			String nc = "0000000" + namespaceId;
			nameSpaceChar = nc.substring(nc.length() - 7);
		} else {
			nameSpaceChar = "0";
		}

		log.debug("Namespace Char " + nameSpaceChar);

		try {

			SctIdIdentifier sctIdIdentifier = new SctIdIdentifier();
			sctIdIdentifier.setCode(componentUuid);
			long selectStartTime = System.currentTimeMillis();
			Long sctIdentirier = (Long) session.selectOne("com.termmed.genid.data.SctIdIdentifierMapper.getSctIdByComponentUuid", componentUuid);
			log.info(sctIdentirier);
			result = sctIdentirier;
			long selectEndTime = System.currentTimeMillis();
			log.info("SELECT SCTID IN: " + (selectEndTime - selectStartTime) + " MS");

			log.debug("Selected SctID: " + result);

			if (result == null || result == 0) {
				log.debug("Partition Number: " + partitionId);
				if (namespaceId > 0) {
					log.debug("Extension number > 0");
					multip = 1000000000;
				}
				SctIdBase sctIdBase = new SctIdBase(partitionId, null, nameSpaceChar);
				sctIdBase = (SctIdBase) session.selectOne("com.termmed.genid.data.SctIdBaseMapper.selectVal", sctIdBase);
				log.debug("Selected sctid base: " + sctIdBase);
				num = sctIdBase.getValue();
				num++;
				sctIdBase.setValue(num);
				long insertStartTime = System.currentTimeMillis();
				session.update("com.termmed.genid.data.SctIdBaseMapper.updateVal", sctIdBase);
				long insertEndTime = System.currentTimeMillis();
				log.info("UPDATE IDBASE VALUE IN: " + (insertEndTime - insertStartTime) + " MS");

				numTmp = num;
				num = (num * multip) + ((long)(namespaceId * 100)) + Long.parseLong(partitionId);
				log.debug("NUMBER: " + num);
				result = GenIdHelper.verhoeffCompute("" + num);
				String newSctId = String.valueOf(num) + String.valueOf(result);
				log.debug("verhoeff Compute: " + newSctId);

				// Create new sctIdIdentifier to insert
				SctIdIdentifier newSctIdent = new SctIdIdentifier();
				newSctIdent.setPartitionId(partitionId);
				newSctIdent.setNamespaceId(nameSpaceChar);
				newSctIdent.setArtifactId("" + numTmp);
				newSctIdent.setReleaseId(releaseId);
				newSctIdent.setItemId(numTmp);
				newSctIdent.setSctId(newSctId);
				newSctIdent.setCode(componentUuid);

				insertNewSctIdentifier(newSctIdent);

				result = Long.parseLong(newSctId);
			} else {
				return result;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		long endTime = System.currentTimeMillis();
		log.info("Sctid created in: " + (endTime - startTime) + " MS");
		return result;
	}

	@Override
	public String createSNOMEDID(String componentUuid, String parentSnomedId) throws Exception {
		log.debug("\n\n##################### CREATE SNOMED ID #######################");
		log.debug("Component uuid: " + componentUuid);
		log.debug("Parent Snomed ID: " + parentSnomedId);
		String snoID = null;
		try {
			log.debug("Getting sctIdentifier");
			SctIdIdentifier sctIdentifier = (SctIdIdentifier) session.selectOne("com.termmed.genid.data.SctIdIdentifierMapper.selectSctIdByComponentUuid", componentUuid);
			log.debug("SctIdentifier: " + sctIdentifier);
			if (sctIdentifier != null) {
				log.debug("Getting conidmap for component uuid");
				ConidMap conidmap = (ConidMap) session.selectOne("com.termmed.genid.data.ConidMapMapper.getConidMapByCode", componentUuid);
				log.debug("SELECTED CONIDMAP: " + conidmap);
				if (conidmap != null) {
					if (conidmap.getSnomedId() != null && !conidmap.getSnomedId().trim().equals("")) {
						log.debug("Result: " + conidmap.getSnomedId());
						log.debug("\n\n######################### DONE #############################");
						return conidmap.getSnomedId();
					} else {
						log.debug("Getting new snomed id");
						snoID = GenIdHelper.getNewSNOMEDID(parentSnomedId, session);
						log.debug("New snomed id: " + snoID);
						log.debug("Updating sonmed id");
						conidmap.setSnomedId(snoID);
						int result = session.update("com.termmed.genid.data.ConidMapMapper.updateConidmap", conidmap);
						log.debug(result  + " rows affected");
					}
				} else {
					log.debug("Getting new snomed id");
					snoID = GenIdHelper.getNewSNOMEDID(parentSnomedId, session);
					log.debug("New snomed id: " + snoID);
					log.debug("Inserting sonmed id");
					ConidMap newConidMap = new ConidMap(sctIdentifier.getSctId(), null, snoID, componentUuid, sctIdentifier.getArtifactId(), sctIdentifier.getReleaseId());
					int result = session.insert("com.termmed.genid.data.ConidMapMapper.insertConidMap", newConidMap);
					log.debug(result + " rows affected");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		log.debug("Create snomed id result: " + snoID);
		log.debug("\n\n######################### DONE #############################");
		return snoID;
	}

	@Override
	public String createCTV3ID(String componentUuid) throws Exception {
		String ctv3ID = null;
		try {
			SctIdIdentifier sctIdentifier = (SctIdIdentifier) session.selectOne("com.termmed.genid.data.SctIdIdentifierMapper.selectSctIdByComponentUuid", componentUuid);
			if (sctIdentifier != null) {
				ConidMap conidmap = (ConidMap) session.selectOne("com.termmed.genid.data.ConidMapMapper.getConidMapByCode", componentUuid);
				log.debug(conidmap);
				if (conidmap != null) {
					if (conidmap.getCtv3Id() != null && !conidmap.getCtv3Id().trim().equals("")) {
						return conidmap.getCtv3Id();
					} else {
						ctv3ID = GenIdHelper.getNewCTV3ID(session);
						conidmap.setCtv3Id(ctv3ID);
						session.update("com.termmed.genid.data.ConidMapMapper.updateConidmap", conidmap);
					}
				} else {
					ctv3ID = GenIdHelper.getNewCTV3ID(session);
					ConidMap newConidMap = new ConidMap(sctIdentifier.getSctId(), ctv3ID, null, componentUuid, sctIdentifier.getArtifactId(), sctIdentifier.getNamespaceId());
					session.insert("com.termmed.genid.data.ConidMapMapper.insertConidMap", newConidMap);
				}
			}

		} catch (Exception e) {
			throw e;
		}
		return ctv3ID;
	}

	@Override
	public HashMap<IDENTIFIER, String> createConceptIds(String componentUuid, String parentSnomedId, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId)
			throws Exception {

		long startTime = System.currentTimeMillis();
		HashMap<IDENTIFIER, String> result = new HashMap<SctIdGen.IDENTIFIER, String>();
		SctIdIdentifier sctID = null;
		Long longSctId = null;

		long num;
		long numTmp;
		long multip = 100;
		String nameSpaceChar;
		long numPart = Integer.parseInt(partitionId);

		if (namespaceId > 0) {
			String nc = "0000000" + namespaceId;
			nameSpaceChar = nc.substring(nc.length() - 7);
		} else {
			nameSpaceChar = "0";
		}

		log.debug("Namespace Char " + nameSpaceChar);

		try {

			sctID = getSctIdentifierByCoordinates(componentUuid, partitionId, nameSpaceChar);

			log.debug("Selected SctID: " + sctID);

			if (sctID == null || sctID.getSctId().equals("0")) {
				log.debug("Partition Number: " + partitionId);
				if (namespaceId > 0) {
					log.debug("Extension number > 0");
					multip = 1000000000;
				}
				SctIdBase sctIdBase = new SctIdBase(partitionId, null, nameSpaceChar);
				sctIdBase = (SctIdBase) session.selectOne("com.termmed.genid.data.SctIdBaseMapper.selectVal", sctIdBase);
				log.debug("Selected sctid base: " + sctIdBase);
				num = sctIdBase.getValue();
				num++;
				sctIdBase.setValue(num);
				long insertStartTime = System.currentTimeMillis();
				session.update("com.termmed.genid.data.SctIdBaseMapper.updateVal", sctIdBase);
				long insertEndTime = System.currentTimeMillis();
				log.info("UPDATE IDBASE VALUE IN: " + (insertEndTime - insertStartTime) + " MS");

				numTmp = num;
				num = (num * multip) + (namespaceId * 100) + Integer.parseInt(partitionId);
				log.debug("NUMBER: " + num);
				longSctId = GenIdHelper.verhoeffCompute("" + num);
				String newSctId = String.valueOf(num) + String.valueOf(longSctId);
				log.debug("verhoeff Compute: " + newSctId);

				// Create new sctIdIdentifier to insert
				SctIdIdentifier newSctIdent = new SctIdIdentifier();
				newSctIdent.setPartitionId(partitionId);
				newSctIdent.setNamespaceId(nameSpaceChar);
				newSctIdent.setArtifactId("" + numTmp);
				newSctIdent.setReleaseId(releaseId);
				newSctIdent.setItemId(numTmp);
				newSctIdent.setSctId(newSctId);
				newSctIdent.setCode(componentUuid);

				insertNewSctIdentifier(newSctIdent);

				String snoID = "";
				String ctv3ID = "";
				if (numPart == 0) {
					snoID = GenIdHelper.getNewSNOMEDID(parentSnomedId, session);
					ctv3ID = GenIdHelper.getNewCTV3ID(session);
					result.put(IDENTIFIER.CTV3ID, ctv3ID);
					result.put(IDENTIFIER.SNOMEDID, snoID);
				}
				if (numPart == 10 || numPart == 0) {
					ConidMap newConidMap = new ConidMap(newSctId, ctv3ID, snoID, componentUuid, moduleId, executionId);
					insertStartTime = System.currentTimeMillis();
					session.insert("com.termmed.genid.data.ConidMapMapper.insertConidMap", newConidMap);
					insertEndTime = System.currentTimeMillis();
					log.info("ISERT CONIDMAP IN: " + (insertEndTime - insertStartTime) + " MS");
				}

				result.put(IDENTIFIER.SCTID, newSctId);
			} else {
				ConidMap conidmap = (ConidMap) session.selectOne("com.termmed.genid.data.ConidMapMapper.getConidMapByCode", componentUuid);
				if (conidmap != null) {
					result.put(IDENTIFIER.CTV3ID, conidmap.getCtv3Id());
					result.put(IDENTIFIER.SNOMEDID, conidmap.getSnomedId());
				}
				result.put(IDENTIFIER.SCTID, sctID.getSctId());
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		long endTime = System.currentTimeMillis();
		log.info("Sctid created in: " + (endTime - startTime) + " MS");
		return result;
	}

	private void insertNewSctIdentifier(SctIdIdentifier newSctIdent) throws Exception {
		long insertStartTime;
		long insertEndTime;
		insertStartTime = System.currentTimeMillis();
		session.insert("com.termmed.genid.data.SctIdIdentifierMapper.insertSctIdIdentifier", newSctIdent);
		insertEndTime = System.currentTimeMillis();
		log.info("INSERT SCTID IDENTIFIER IN: " + (insertEndTime - insertStartTime) + " MS");
	}

	private SctIdIdentifier getSctIdentifierByCoordinates(String componentUuid, String partitionId, String nameSpaceChar) {
		SctIdIdentifier sctID;
		SctIdIdentifier sctIdIdentifier = new SctIdIdentifier();
		sctIdIdentifier.setCode(componentUuid);
		sctIdIdentifier.setNamespaceId(nameSpaceChar);
		sctIdIdentifier.setPartitionId(partitionId);
		long selectStartTime = System.currentTimeMillis();
		sctID = (SctIdIdentifier) session.selectOne("com.termmed.genid.data.SctIdIdentifierMapper.selectSctId", sctIdIdentifier);
		long selectEndTime = System.currentTimeMillis();
		log.info("SELECT SCTID IN: " + (selectEndTime - selectStartTime) + " MS");
		return sctID;
	}

	@Override
	public HashMap<String, Long> createSCTIDList(List<String> componentUuidList, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws Exception {
		HashMap<String, Long> result = new HashMap<String, Long>();

		for (String componentUuid : componentUuidList) {
			result.put(componentUuid, createSCTID(componentUuid, namespaceId, partitionId, releaseId, executionId, moduleId));
		}
		return result;
	}

	@Override
	public HashMap<String, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<String, String> componentUUIDandParentSnomedId, Integer namespaceId, String partitionId, String releaseId,
			String executionId, String moduleId) throws Exception {
		HashMap<String, HashMap<IDENTIFIER, String>> result = new HashMap<String, HashMap<IDENTIFIER, String>>();
		for (String loopComponentUuid : componentUUIDandParentSnomedId.keySet()) {
			HashMap<IDENTIFIER, String> idString = createConceptIds(loopComponentUuid, componentUUIDandParentSnomedId.get(loopComponentUuid), namespaceId, partitionId, releaseId, executionId,
					moduleId);
			result.put(loopComponentUuid, idString);
		}
		return result;
	}

}
