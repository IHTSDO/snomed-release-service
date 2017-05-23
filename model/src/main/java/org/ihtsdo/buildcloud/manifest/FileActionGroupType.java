
package org.ihtsdo.buildcloud.manifest;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fileActionGroupType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="fileActionGroupType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fileSplitAction" type="{http://release.ihtsdo.org/manifest/1.0.0}fileSplitActionType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="fileCombineAction" type="{http://release.ihtsdo.org/manifest/1.0.0}fileCombineActionType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileActionGroupType", propOrder = {
    "fileSplitAction",
    "fileCombineAction"
})
public class FileActionGroupType {

    protected List<FileSplitActionType> fileSplitAction;
    protected List<FileCombineActionType> fileCombineAction;

    /**
     * Gets the value of the fileSplitAction property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fileSplitAction property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFileSplitAction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FileSplitActionType }
     * 
     * 
     */
    public List<FileSplitActionType> getFileSplitAction() {
        if (fileSplitAction == null) {
            fileSplitAction = new ArrayList<FileSplitActionType>();
        }
        return this.fileSplitAction;
    }

    /**
     * Gets the value of the fileCombineAction property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fileCombineAction property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFileCombineAction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FileCombineActionType }
     * 
     * 
     */
    public List<FileCombineActionType> getFileCombineAction() {
        if (fileCombineAction == null) {
            fileCombineAction = new ArrayList<FileCombineActionType>();
        }
        return this.fileCombineAction;
    }

}
