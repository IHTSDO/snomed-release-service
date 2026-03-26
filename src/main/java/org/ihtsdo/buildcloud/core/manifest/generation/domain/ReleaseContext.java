package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import org.ihtsdo.buildcloud.core.service.TermServerService;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;

public record ReleaseContext(CodeSystem codeSystem, String effectiveTime,
                             TermServerService snowstormClient) {
}
