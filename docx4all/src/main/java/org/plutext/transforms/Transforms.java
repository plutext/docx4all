/*
 *  Copyright 2008, Plutext Pty Ltd.
 *   
 *  This file is part of Docx4all.

    Docx4all is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU General Public License 
    as published by the Free Software Foundation.

    Docx4all is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with Docx4all.  If not, see <http://www.gnu.org/licenses/>.
    
 */
package org.plutext.transforms;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.Style;
import org.jvnet.jaxb2_commons.ppp.Child;


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
 *         &lt;element name="t" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;choice>
 *                   &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
 *                   &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}sdt"/>
 *                 &lt;/choice>
 *                 &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                 &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedByte" />
 *                 &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="idref" type="{http://www.w3.org/2001/XMLSchema}unsignedByte" />
 *                 &lt;attribute name="after" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
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
public class Transforms
    implements Child
{

    @XmlElement(required = true)
    protected List<Transforms.T> t;
    @XmlTransient
    private Object parent;

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
     * Gets the parent object in the object tree representing the unmarshalled xml document.
     * 
     * @return
     *     The parent object.
     */
    public Object getParent() {
        return this.parent;
    }

    public void setParent(Object parent) {
        this.parent = parent;
    }

    /**
     * This method is invoked by the JAXB implementation on each instance when unmarshalling completes.
     * 
     * @param parent
     *     The parent object in the object tree.
     * @param unmarshaller
     *     The unmarshaller that generated the instance.
     */
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        setParent(parent);
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
     *       &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *       &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedByte" />
     *       &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="idref" type="{http://www.w3.org/2001/XMLSchema}unsignedByte" />
     *       &lt;attribute name="after" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
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
    public static class T
        implements Child
    {

        @XmlElement(namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        protected Style style;
        @XmlElement(namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        protected SdtBlock sdt;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger tstamp;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        @XmlSchemaType(name = "unsignedByte")
        protected short snum;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms", required = true)
        protected String op;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms")
        @XmlSchemaType(name = "unsignedByte")
        protected Short idref;
        @XmlAttribute(namespace = "http://www.plutext.org/transforms")
        @XmlSchemaType(name = "unsignedInt")
        protected Long after;
        @XmlTransient
        private Object parent;

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
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getTstamp() {
            return tstamp;
        }

        /**
         * Sets the value of the tstamp property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setTstamp(BigInteger value) {
            this.tstamp = value;
        }

        /**
         * Gets the value of the snum property.
         * 
         */
        public short getSnum() {
            return snum;
        }

        /**
         * Sets the value of the snum property.
         * 
         */
        public void setSnum(short value) {
            this.snum = value;
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
         *     {@link Short }
         *     
         */
        public Short getIdref() {
            return idref;
        }

        /**
         * Sets the value of the idref property.
         * 
         * @param value
         *     allowed object is
         *     {@link Short }
         *     
         */
        public void setIdref(Short value) {
            this.idref = value;
        }

        /**
         * Gets the value of the after property.
         * 
         * @return
         *     possible object is
         *     {@link Long }
         *     
         */
        public Long getAfter() {
            return after;
        }

        /**
         * Sets the value of the after property.
         * 
         * @param value
         *     allowed object is
         *     {@link Long }
         *     
         */
        public void setAfter(Long value) {
            this.after = value;
        }

        /**
         * Gets the parent object in the object tree representing the unmarshalled xml document.
         * 
         * @return
         *     The parent object.
         */
        public Object getParent() {
            return this.parent;
        }

        public void setParent(Object parent) {
            this.parent = parent;
        }

        /**
         * This method is invoked by the JAXB implementation on each instance when unmarshalling completes.
         * 
         * @param parent
         *     The parent object in the object tree.
         * @param unmarshaller
         *     The unmarshaller that generated the instance.
         */
        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            setParent(parent);
        }

    }

}
