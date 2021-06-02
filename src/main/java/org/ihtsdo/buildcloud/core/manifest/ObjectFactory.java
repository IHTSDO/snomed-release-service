//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.09.28 at 02:15:54 PM BST 
//


package org.ihtsdo.buildcloud.core.manifest;

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
     * Create an instance of {@link SourcesType }
     * 
     */
    public SourcesType createSourcesType() {
        return new SourcesType();
    }

    /**
     * Create an instance of {@link RefsetType }
     * 
     */
    public RefsetType createRefsetType() {
        return new RefsetType();
    }

    /**
     * Create an instance of {@link ContainsLanguageCodesType }
     * 
     */
    public ContainsLanguageCodesType createContainsLanguageCodesType() {
        return new ContainsLanguageCodesType();
    }

    /**
     * Create an instance of {@link ContainsAdditionalFieldsType }
     * 
     */
    public ContainsAdditionalFieldsType createContainsAdditionalFieldsType() {
        return new ContainsAdditionalFieldsType();
    }

    /**
     * Create an instance of {@link FolderType }
     * 
     */
    public FolderType createFolderType() {
        return new FolderType();
    }

    /**
     * Create an instance of {@link ContainsReferenceSetsType }
     * 
     */
    public ContainsReferenceSetsType createContainsReferenceSetsType() {
        return new ContainsReferenceSetsType();
    }

    /**
     * Create an instance of {@link FieldType }
     * 
     */
    public FieldType createFieldType() {
        return new FieldType();
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