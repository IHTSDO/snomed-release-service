package org.ihtsdo.buildcloud.releaseinformation;

import org.ihtsdo.buildcloud.manifest.RefsetType;

import java.math.BigInteger;
import java.util.List;

public class ReleasePackageInfor {
    private String deltaFromDate;

    private String deltaToDate;

    private String languageRefset;

    private List<LanguageRefset> humanReadableLanguageRefset;

    private String licenceStatement;

    private String effectiveTime;

    private String includedModuleIDs;

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

    public String getLanguageRefset() {
        return languageRefset;
    }

    public void setLanguageRefset(String languageRefset) {
        this.languageRefset = languageRefset;
    }

    public List<LanguageRefset> getHumanReadableLanguageRefset() {
        return humanReadableLanguageRefset;
    }

    public void setHumanReadableLanguageRefset(List<LanguageRefset> humanReadableLanguageRefset) {
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

    public String getIncludedModuleIDs() {
        return includedModuleIDs;
    }

    public void setIncludedModuleIDs(String includedModuleIDs) {
        this.includedModuleIDs = includedModuleIDs;
    }
}
