package org.ihtsdo.buildcloud.core.service.build;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DailyBuildRF2SnapshotExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DailyBuildRF2SnapshotExtractor.class);
	private final Build build;
	private final BuildDAO buildDAO;
	private static final String SNAPSHOTS_FOLDER = "SNAPSHOTS";


	public DailyBuildRF2SnapshotExtractor(Build build, BuildDAO dao) {
		this.build = build;
		this.buildDAO = dao;
	}

	public void outputDailyBuildPackage(ResourceManager resourceManager) throws IOException, ResourceNotFoundException, JAXBException {
		File snapshotZip = null;
		try {
			if (build.getConfiguration().isDailyBuild()) {
				Zipper zipper = new Zipper(build, buildDAO);
				snapshotZip = zipper.createZipFile(Zipper.FileTypeOption.SNAPSHOT_ONLY);
				uploadDailyBuildToS3(build, snapshotZip, resourceManager);
			}
		} finally {
			org.apache.commons.io.FileUtils.deleteQuietly(snapshotZip);
		}
	}

	private void uploadDailyBuildToS3(Build build, File zipPackage, ResourceManager resourceManager) throws IOException {
		String codeSystem = RF2Constants.SNOMEDCT;
		String branchPath = build.getConfiguration().getBranchPath();
		if (branchPath != null) {
			String[] splits = branchPath.split("/");
			if (splits.length >= 2) {
				codeSystem = splits[1];
			}
		} else {
			String businessKey = build.getReleaseCenterKey();
			if (!RF2Constants.INT_RELEASE_CENTER.getBusinessKey().equalsIgnoreCase(businessKey)) {
				codeSystem += "-" + businessKey;
			}
		}
		String dateStr = DateUtils.now(RF2Constants.DAILY_BUILD_TIME_FORMAT);
		String targetFilePath = SNAPSHOTS_FOLDER + S3PathHelper.SEPARATOR + codeSystem + S3PathHelper.SEPARATOR + dateStr + ".zip";
		resourceManager.writeResource(targetFilePath, new FileInputStream(zipPackage));
		LOGGER.info("Daily build snapshot package {} is uploaded to S3 {}", zipPackage.getName(), targetFilePath);
	}
}
