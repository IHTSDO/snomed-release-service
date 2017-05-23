
package org.ihtsdo.buildcloud.manifest;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.ihtsdo.buildcloud.manifest package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Listing_QNAME = new QName("http://release.ihtsdo.org/manifest/1.0.0", "listing");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.ihtsdo.buildcloud.manifest
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ListingType }
     * 
     */
    public ListingType createListingType() {
        return new ListingType();
    }

    /**
     * Create an instance of {@link FileCombineActionType }
     * 
     */
    public FileCombineActionType createFileCombineActionType() {
        return new FileCombineActionType();
    }

    /**
     * Create an instance of {@link FileSplitTargetType }
     * 
     */
    public FileSplitTargetType createFileSplitTargetType() {
        return new FileSplitTargetType();
    }

    /**
     * Create an instance of {@link FileActionGroupType }
     * 
     */
    public FileActionGroupType createFileActionGroupType() {
        return new FileActionGroupType();
    }

    /**
     * Create an instance of {@link FileSplitActionType }
     * 
     */
    public FileSplitActionType createFileSplitActionType() {
        return new FileSplitActionType();
    }

    /**
     * Create an instance of {@link FolderType }
     * 
     */
    public FolderType createFolderType() {
        return new FolderType();
    }

    /**
     * Create an instance of {@link FileType }
     * 
     */
    public FileType createFileType() {
        return new FileType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListingType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://release.ihtsdo.org/manifest/1.0.0", name = "listing")
    public JAXBElement<ListingType> createListing(ListingType value) {
        return new JAXBElement<ListingType>(_Listing_QNAME, ListingType.class, null, value);
    }

}
