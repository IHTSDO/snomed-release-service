package org.ihtsdo.buildcloud.core.service.jms.listener;

import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.ModuleStorageCoordinatorCache;
import org.junit.jupiter.api.Test;
import org.snomed.module.storage.ModuleMetadata;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeSystemNewAuthoringCycleHandlerTest {

	@Test
	void updateExtensionConfig_derivesPreviousDependencyEffectiveDateFromDependencyRelease_whenMscDependenciesMissing() throws Exception {
		ModuleStorageCoordinatorCache moduleStorageCoordinatorCache = new ModuleStorageCoordinatorCache() {
			@Override
			public Map<String, List<ModuleMetadata>> getAllReleases() {
				return Collections.emptyMap();
			}
		};

		CodeSystemNewAuthoringCycleHandler handler = new CodeSystemNewAuthoringCycleHandler(
				null, null, moduleStorageCoordinatorCache, null, null
		);

		ExtensionConfig extensionConfig = new ExtensionConfig();
		extensionConfig.setReleaseAsAnEdition(true);
		extensionConfig.setDependencyRelease("SnomedCT_InternationalRF2_Production_20240131T120000.zip");

		BuildConfiguration buildConfiguration = new BuildConfiguration();
		buildConfiguration.setExtensionConfig(extensionConfig);
		extensionConfig.setBuildConfiguration(buildConfiguration);

		Product product = new Product();
		product.setBuildConfiguration(buildConfiguration);

		Method updateExtensionConfig = CodeSystemNewAuthoringCycleHandler.class.getDeclaredMethod("updateExtensionConfig", Product.class, String.class);
		updateExtensionConfig.setAccessible(true);
		updateExtensionConfig.invoke(handler, product, "Some_Edition_20240731.zip");

		assertEquals("2024-01-31", extensionConfig.getPreviousEditionDependencyEffectiveDateFormatted());
	}
}

