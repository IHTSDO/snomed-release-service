package org.ihtsdo.buildcloud.service.build;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.DAILY_BUILD_TIME_FORMAT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.INT_RELEASE_CENTER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DailyBuildRF2DeltaExtractor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DailyBuildRF2DeltaExtractor.class);
	private final Build build;
	private final BuildDAO buildDAO;

	
	public DailyBuildRF2DeltaExtractor(Build build, BuildDAO dao) {
		this.build = build;
		this.buildDAO = dao;
	}

	public void outputDailyBuildPackage(ResourceManager resourceManager) throws IOException, ResourceNotFoundException, JAXBException {
		File deltaZip = null;
		try {
			if (build.getConfiguration().isDailyBuild()) {
				Zipper zipper = new Zipper(build, buildDAO);
				deltaZip = zipper.createZipFile(true);
				ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
				// for edition release the international content is not required for the daily build browser import
				if (extensionConfig != null && extensionConfig.isReleaseAsAnEdition()) {
					deltaZip = filterContentWithEffectiveTime(deltaZip, build.getConfiguration().getEffectiveTimeSnomedFormat());
				}
				uploadDailyBuildToS3(build, deltaZip, resourceManager);
			}
		} finally {
			org.apache.commons.io.FileUtils.deleteQuietly(deltaZip);
		}
	}

	File filterContentWithEffectiveTime(File deltaZip, String effectiveTimeFormatted) throws IOException {
		File updatedZip = new File(deltaZip.getParent(), deltaZip.getName().replace(".zip", "_updated.zip"));
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(deltaZip));
			ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(updatedZip), RF2Constants.UTF_8)) {
			ZipEntry zipEntry = null;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				zipOutputStream.putNextEntry(zipEntry);
				if (!zipEntry.isDirectory()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream));
					OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream, RF2Constants.UTF_8);
					String line = reader.readLine();
					writer.append(line);
					writer.append(RF2Constants.LINE_ENDING);
					while ((line = reader.readLine()) != null) {
						String[] splits = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
						if (effectiveTimeFormatted.equals(splits[1])) {
							writer.append(line);
							writer.append(RF2Constants.LINE_ENDING);
						}
					}
					writer.flush();
				}
			}
			zipOutputStream.closeEntry();
		}
		return updatedZip;
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
			String businessKey = build.getProduct().getReleaseCenter().getBusinessKey();
			if (!INT_RELEASE_CENTER.getBusinessKey().equalsIgnoreCase(businessKey)) {
				codeSystem += "-" + businessKey;
			}
		}
		String dateStr = DateUtils.now(DAILY_BUILD_TIME_FORMAT);
		String targetFilePath = codeSystem + BuildS3PathHelper.SEPARATOR + dateStr + ".zip";
		resourceManager.writeResource(targetFilePath, new FileInputStream(zipPackage));
		LOGGER.info("Daily build package {} is uploaded to S3 {}", zipPackage.getName(), targetFilePath);
	}
}
