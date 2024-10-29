package org.ihtsdo.buildcloud.rest.pojo;

public record PostReleaseRequest
        (String nextCycleEffectiveTime, String dailyBuildProductKey, String releasedSourceProductKey,
         String releasedSourceBuildKey, String newDependencyPackage) {
}
