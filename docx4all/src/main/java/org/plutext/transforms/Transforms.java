/*
 *  Copyright 2008, Plutext Pty Ltd.
 *   
 *  This file is part of Plutext-Server.

    Plutext-Server is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU Affero General Public License 
    as published by the Free Software Foundation.

    Plutext-Server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License   
    along with Plutext-Server.  If not, see <http://www.fsf.org/licensing/licenses/>.
    
 */

package org.plutext.transforms;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.Style;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="t" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;choice>
 *                   &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
 *                   &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}sdt"/>
 *                 &lt;/choice>
 *                 &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                 &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                 &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                 &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="idref" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                 &lt;attribute name="position" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "t"
})
@XmlRootElement(name = "transforms")
public class Transforms {

    protected List<Transforms.T> t;

    /**
     * Gets the value of the t property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the t property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getT().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Transforms.T }
     * 
     * 
     */
    public List<Transforms.T> getT() {
        if (t == null) {
            t = new ArrayList<Transforms.T>();
        }
        return this.t;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;choice>
     *         &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
     *         &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}sdt"/>
     *       &lt;/choice>
     *       &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *       &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *       &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *       &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="idref" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *       &lt;attribute name="position" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "style",
        "sdt"
    })
    public static class T {

        @XmlElement(namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        protected Style style;
        @XmlElement(namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        protected SdtBlock sdt;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        @XmlSchemaType(name = "unsignedInt")
        protected long tstamp;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        @XmlSchemaType(name = "unsignedInt")
        protected long snum;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        @XmlSchemaType(name = "unsignedInt")
        protected long changeset;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        protected String op;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms")
        @XmlSchemaType(name = "unsignedInt")
        protected Long idref;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms")
        @XmlSchemaType(name = "unsignedInt")
        protected Long position;

        /**
         * Gets the value of the style property.
         * 
         * @return
         *     possible object is
         *     {@link Style }
         *     
         */
        public Style getStyle() {
            return style;
        }

        /**
         * Sets the value of the style property.
         * 
         * @param value
         *     allowed object is
         *     {@link Style }
         *     
         */
        public void setStyle(Style value) {
            this.style = value;
        }

        /**
         * Gets the value of the sdt property.
         * 
         * @return
         *     possible object is
         *     {@link SdtBlock }
         *     
         */
        public SdtBlock getSdt() {
            return sdt;
        }

        /**
         * Sets the value of the sdt property.
         * 
         * @param value
         *     allowed object is
         *     {@link SdtBlock }
         *     
         */
        public void setSdt(SdtBlock value) {
            this.sdt = value;
        }

        /**
         * Gets the value of the tstamp property.
         * 
         */
        public long getTstamp() {
            return tstamp;
        }

        /**
         * Sets the value of the tstamp property.
         * 
         */
        public void setTstamp(long value) {
            this.tstamp = value;
        }

        /**
         * Gets the value of the snum property.
         * 
         */
        public long getSnum() {
            return snum;
        }

        /**
         * Sets the value of the snum property.
         * 
         */
        public void setSnum(long value) {
            this.snum = value;
        }

        /**
         * Gets the value of the changeset property.
         * 
         */
        public long getChangeset() {
            return changeset;
        }

        /**
         * Sets the value of the changeset property.
         * 
         */
        public void setChangeset(long value) {
            this.changeset = value;
        }

        /**
         * Gets the value of the op property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOp() {
            return op;
        }

        /**
         * Sets the value of the op property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOp(String value) {
            this.op = value;
        }

        /**
         * Gets the value of the idref property.
         * 
         * @return
         *     possible object is
         *     {@link Long }
         *     
         */
        public Long getIdref() {
            return idref;
        }

        /**
         * Sets the value of the idref property.
         * 
         * @param value
         *     allowed object is
         *     {@link Long }
         *     
         */
        public void setIdref(Long value) {
            this.idref = value;
        }

        /**
         * Gets the value of the position property.
         * 
         * @return
         *     possible object is
         *     {@link Long }
         *     
         */
        public Long getPosition() {
            return position;
        }

        /**
         * Sets the value of the position property.
         * 
         * @param value
         *     allowed object is
         *     {@link Long }
         *     
         */
        public void setPosition(Long value) {
            this.position = value;
        }

    }

}
