package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PackageServiceImpl extends EntityServiceImpl<Package> implements PackageService {
	
	private static final String TRUE = "true";

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	protected PackageServiceImpl(final PackageDAO dao) {
		super(dao);
	}

	@Override
	public final Package find(final String buildCompositeKey, final String packageBusinessKey, final User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return packageDAO.find(buildId, packageBusinessKey, authenticatedUser);
	}

	@Override
	public final List<Package> findAll(final String buildCompositeKey, final User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return new ArrayList<>(buildDAO.find(buildId, authenticatedUser).getPackages());
	}

	@Override
	public final Package create(final String buildCompositeKey, final String name, final User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Build build = buildDAO.find(buildId, authenticatedUser);
		Package pkg = new Package(name);
		build.addPackage(pkg);
		packageDAO.save(pkg);
		return pkg;
	}

	@Override
	public final Package update(final String buildCompositeKey, final String packageBusinessKey,
			final Map<String, String> newPropertyValues, final User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Package aPackage = packageDAO.find(buildId, packageBusinessKey, authenticatedUser);

		if (newPropertyValues.containsKey(PackageService.FIRST_TIME_RELEASE)) {
			aPackage.setFirstTimeRelease(TRUE.equals(newPropertyValues.get(FIRST_TIME_RELEASE)));
		}

		if (newPropertyValues.containsKey(PackageService.PREVIOUS_PUBLISHED_PACKAGE)) {
			aPackage.setPreviousPublishedPackage(newPropertyValues.get(PREVIOUS_PUBLISHED_PACKAGE));
		}

		if (newPropertyValues.containsKey(PackageService.README_HEADER)) {
			String readmeHeader = newPropertyValues.get(PackageService.README_HEADER);
			aPackage.setReadmeHeader(readmeHeader);
		}

		packageDAO.update(aPackage);
		return aPackage;
	}

}
