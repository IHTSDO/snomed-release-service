
package org.ihtsdo.buildcloud.manifest;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fileCombineActionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fileCombineActionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fileCombineSourceType" type="{http://release.ihtsdo.org/manifest/1.0.0}fileCombineSourceType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="targetName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileCombineActionType", propOrder = {
    "fileCombineSourceType"
})
public class FileCombineActionType {

    protected List<FileCombineSourceType> fileCombineSourceType;
    @XmlAttribute(name = "targetName")
    protected String targetName;

    /**
     * Gets the value of the fileCombineSourceType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fileCombineSourceType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFileCombineSourceType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FileCombineSourceType }
     * 
     * 
     */
    public List<FileCombineSourceType> getFileCombineSourceType() {
        if (fileCombineSourceType == null) {
            fileCombineSourceType = new ArrayList<FileCombineSourceType>();
        }
        return this.fileCombineSourceType;
    }

    /**
     * Gets the value of the targetName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Sets the value of the targetName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetName(String value) {
        this.targetName = value;
    }

}
