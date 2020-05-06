package org.ihtsdo.buildcloud.releaseinformation;

import java.math.BigInteger;

public class LanguageRefset {
    private String dataSource;

    private BigInteger id;

    private String label;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
