package org.ihtsdo.buildcloud.service.srs;

import java.util.List;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 2:18 PM
 */
public class TextDescriptionProcessingConfig {

    private String targetName;
    private List<String> languageCodes;

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public List<String> getLanguageCodes() {
        return languageCodes;
    }

    public void setLanguageCodes(List<String> languageCode) {
        this.languageCodes = languageCode;
    }
}
