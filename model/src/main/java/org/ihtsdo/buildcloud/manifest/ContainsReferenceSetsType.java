
package org.ihtsdo.buildcloud.manifest;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for containsReferenceSetsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="containsReferenceSetsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="refset" type="{http://release.ihtsdo.org/manifest/1.0.0}refsetType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "containsReferenceSetsType", propOrder = {
    "refset"
})
public class ContainsReferenceSetsType {

    @XmlElement(required = true)
    protected List<RefsetType> refset;

    /**
     * Gets the value of the refset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the refset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRefset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RefsetType }
     * 
     * 
     */
    public List<RefsetType> getRefset() {
        if (refset == null) {
            refset = new ArrayList<RefsetType>();
        }
        return this.refset;
    }

}
