
package org.ihtsdo.buildcloud.manifest;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for sourceType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="sourceType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="manual"/>
 *     &lt;enumeration value="mapping-tool"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "sourceType")
@XmlEnum
public enum SourceType {

    @XmlEnumValue("manual")
    MANUAL("manual"),
    @XmlEnumValue("mapping-tool")
    MAPPING_TOOL("mapping-tool");
    private final String value;

    SourceType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SourceType fromValue(String v) {
        for (SourceType c: SourceType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
