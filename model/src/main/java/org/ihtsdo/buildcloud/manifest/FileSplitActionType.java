
package org.ihtsdo.buildcloud.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fileSplitActionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fileSplitActionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ileSplitTarget" type="{http://release.ihtsdo.org/manifest/1.0.0}fileSplitTargetType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sourceName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileSplitActionType", propOrder = {
    "ileSplitTarget"
})
public class FileSplitActionType {

    @XmlElement(required = true)
    protected FileSplitTargetType ileSplitTarget;
    @XmlAttribute(name = "sourceName")
    protected String sourceName;

    /**
     * Gets the value of the ileSplitTarget property.
     * 
     * @return
     *     possible object is
     *     {@link FileSplitTargetType }
     *     
     */
    public FileSplitTargetType getIleSplitTarget() {
        return ileSplitTarget;
    }

    /**
     * Sets the value of the ileSplitTarget property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileSplitTargetType }
     *     
     */
    public void setIleSplitTarget(FileSplitTargetType value) {
        this.ileSplitTarget = value;
    }

    /**
     * Gets the value of the sourceName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Sets the value of the sourceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceName(String value) {
        this.sourceName = value;
    }

}
