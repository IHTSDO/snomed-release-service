package org.ihtsdo.buildcloud.releaseinformation;

import java.math.BigInteger;

public class LanguageRefset {
    private BigInteger id;

    private String term;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
