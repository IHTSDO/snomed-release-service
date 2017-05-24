
package org.ihtsdo.buildcloud.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fileType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fileType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="contains-reference-sets" type="{http://release.ihtsdo.org/manifest/1.0.0}containsReferenceSetsType" minOccurs="0"/>
 *         &lt;element name="contains-language-codes" type="{http://release.ihtsdo.org/manifest/1.0.0}containsLanguageCodesType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileType", propOrder = {
    "containsReferenceSets",
    "containsLanguageCodes"
})
public class FileType {

    @XmlElement(name = "contains-reference-sets")
    protected ContainsReferenceSetsType containsReferenceSets;
    @XmlElement(name = "contains-language-codes")
    protected ContainsLanguageCodesType containsLanguageCodes;
    @XmlAttribute(name = "Name")
    protected String name;

    /**
     * Gets the value of the containsReferenceSets property.
     * 
     * @return
     *     possible object is
     *     {@link ContainsReferenceSetsType }
     *     
     */
    public ContainsReferenceSetsType getContainsReferenceSets() {
        return containsReferenceSets;
    }

    /**
     * Sets the value of the containsReferenceSets property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContainsReferenceSetsType }
     *     
     */
    public void setContainsReferenceSets(ContainsReferenceSetsType value) {
        this.containsReferenceSets = value;
    }

    /**
     * Gets the value of the containsLanguageCodes property.
     * 
     * @return
     *     possible object is
     *     {@link ContainsLanguageCodesType }
     *     
     */
    public ContainsLanguageCodesType getContainsLanguageCodes() {
        return containsLanguageCodes;
    }

    /**
     * Sets the value of the containsLanguageCodes property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContainsLanguageCodesType }
     *     
     */
    public void setContainsLanguageCodes(ContainsLanguageCodesType value) {
        this.containsLanguageCodes = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}
