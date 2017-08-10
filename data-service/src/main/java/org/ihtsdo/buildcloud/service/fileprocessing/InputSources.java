package org.ihtsdo.buildcloud.service.fileprocessing;

public enum InputSources {

    TERM_SERVER("terminology-server"), REFSET_TOOL("reference-set-tool"), MAPPING_TOOLS("mapping-tools"), MANUAL("manual"), EXTERNALY_MAINTAINED("externally-maintained");

    private String sourceName;

    InputSources(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public static InputSources getSource(String sourceName) {
        for (InputSources sourceType : InputSources.values()) {
            if(sourceType.getSourceName().equalsIgnoreCase(sourceName)) {
                return sourceType;
            }
        }
        return null;
    }
}
