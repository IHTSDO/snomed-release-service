package org.ihtsdo.buildcloud.releaseinformation;

import org.ihtsdo.buildcloud.manifest.RefsetType;

import java.util.List;

public class ReleasePackageInfor {
    private String deltaFromDate;

    private String deltaToDate;

    private String languagesRefset;

    private List<RefsetType> humanReadableLanguageRefset;

    private String licenceStatement;

    private String effectiveTime;

    private String includeModuleId;

    public String getDeltaFromDate() {
        return deltaFromDate;
    }

    public void setDeltaFromDate(String deltaFromDate) {
        this.deltaFromDate = deltaFromDate;
    }

    public String getDeltaToDate() {
        return deltaToDate;
    }

    public void setDeltaToDate(String deltaToDate) {
        this.deltaToDate = deltaToDate;
    }

    public String getLanguagesRefset() {
        return languagesRefset;
    }

    public void setLanguagesRefset(String languagesRefset) {
        this.languagesRefset = languagesRefset;
    }

    public List<RefsetType> getHumanReadableLanguageRefset() {
        return humanReadableLanguageRefset;
    }

    public void setHumanReadableLanguageRefset(List<RefsetType> humanReadableLanguageRefset) {
        this.humanReadableLanguageRefset = humanReadableLanguageRefset;
    }

    public String getLicenceStatement() {
        return licenceStatement;
    }

    public void setLicenceStatement(String licenceStatement) {
        this.licenceStatement = licenceStatement;
    }

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getIncludeModuleId() {
        return includeModuleId;
    }

    public void setIncludeModuleId(String includeModuleId) {
        this.includeModuleId = includeModuleId;
    }
}
