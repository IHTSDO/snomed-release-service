package org.ihtsdo.buildcloud.core.releaseinformation;

import java.util.List;

public class ReleasePackageInformation {

    private String effectiveTime;

    private String deltaFromDate;

    private String deltaToDate;

    private List<ConceptMini> includedModules;

    private List<ConceptMini> languageRefsets;

    private String licenceStatement;

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

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

    public List<ConceptMini> getIncludedModules() {
        return includedModules;
    }

    public void setIncludedModules(List<ConceptMini> includedModules) {
        this.includedModules = includedModules;
    }

    public List<ConceptMini> getLanguageRefsets() {
        return languageRefsets;
    }

    public void setLanguageRefsets(List<ConceptMini> languageRefsets) {
        this.languageRefsets = languageRefsets;
    }

    public String getLicenceStatement() {
        return licenceStatement;
    }

    public void setLicenceStatement(String licenceStatement) {
        this.licenceStatement = licenceStatement;
    }

}
