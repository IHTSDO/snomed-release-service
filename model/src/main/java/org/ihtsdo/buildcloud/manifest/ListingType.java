
package org.ihtsdo.buildcloud.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for listingType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="listingType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="folder" type="{http://release.ihtsdo.org/manifest/1.0.0}folderType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "listingType", propOrder = {
    "folder"
})
public class ListingType {

    @XmlElement(required = true)
    protected FolderType folder;

    /**
     * Gets the value of the folder property.
     * 
     * @return
     *     possible object is
     *     {@link FolderType }
     *     
     */
    public FolderType getFolder() {
        return folder;
    }

    /**
     * Sets the value of the folder property.
     * 
     * @param value
     *     allowed object is
     *     {@link FolderType }
     *     
     */
    public void setFolder(FolderType value) {
        this.folder = value;
    }

}
