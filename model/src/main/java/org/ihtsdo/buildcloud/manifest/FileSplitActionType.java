
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
 *         &lt;element name="fileSplitTarget" type="{http://release.ihtsdo.org/manifest/1.0.0}fileSplitTargetType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="sourceName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="removeFromOriginal" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="removeId" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileSplitActionType", propOrder = {
    "fileSplitTarget"
})
public class FileSplitActionType {

    @XmlElement(required = true)
    protected FileSplitTargetType fileSplitTarget;
    @XmlAttribute(name = "sourceName")
    protected String sourceName;
    @XmlAttribute(name = "removeFromOriginal")
    protected Boolean removeFromOriginal;
    @XmlAttribute(name = "removeId")
    protected Boolean removeId;

    /**
     * Gets the value of the fileSplitTarget property.
     * 
     * @return
     *     possible object is
     *     {@link FileSplitTargetType }
     *     
     */
    public FileSplitTargetType getFileSplitTarget() {
        return fileSplitTarget;
    }

    /**
     * Sets the value of the fileSplitTarget property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileSplitTargetType }
     *     
     */
    public void setFileSplitTarget(FileSplitTargetType value) {
        this.fileSplitTarget = value;
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

    /**
     * Gets the value of the removeFromOriginal property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRemoveFromOriginal() {
        return removeFromOriginal;
    }

    /**
     * Sets the value of the removeFromOriginal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRemoveFromOriginal(Boolean value) {
        this.removeFromOriginal = value;
    }

    /**
     * Gets the value of the removeId property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRemoveId() {
        return removeId;
    }

    /**
     * Sets the value of the removeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRemoveId(Boolean value) {
        this.removeId = value;
    }

}
