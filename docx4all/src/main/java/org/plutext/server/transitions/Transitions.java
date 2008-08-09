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

package org.plutext.server.transitions;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
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
 *         &lt;element name="styles" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="styletransition" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
 *                           &lt;/sequence>
 *                           &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                           &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                           &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="ribs">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="rib" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence minOccurs="0">
 *                             &lt;element name="t" maxOccurs="unbounded">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                                     &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                                     &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                                     &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                           &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                           &lt;attribute name="version" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *                           &lt;attribute name="deleted" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *       &lt;attribute name="deleted" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "styles",
    "ribs"
})
@XmlRootElement(name = "transitions")
public class Transitions {

    protected Transitions.Styles styles;
    @XmlElement(required = true)
    protected Transitions.Ribs ribs;
    @XmlAttribute(namespace = "http://www.plutext.org/transitions")
    @XmlSchemaType(name = "unsignedInt")
    protected Long id;
    @XmlAttribute(namespace = "http://www.plutext.org/transitions")
    @XmlSchemaType(name = "unsignedInt")
    protected Long version;
    @XmlAttribute(namespace = "http://www.plutext.org/transitions")
    protected Boolean deleted;

    /**
     * Gets the value of the styles property.
     * 
     * @return
     *     possible object is
     *     {@link Transitions.Styles }
     *     
     */
    public Transitions.Styles getStyles() {
        return styles;
    }

    /**
     * Sets the value of the styles property.
     * 
     * @param value
     *     allowed object is
     *     {@link Transitions.Styles }
     *     
     */
    public void setStyles(Transitions.Styles value) {
        this.styles = value;
    }

    /**
     * Gets the value of the ribs property.
     * 
     * @return
     *     possible object is
     *     {@link Transitions.Ribs }
     *     
     */
    public Transitions.Ribs getRibs() {
        return ribs;
    }

    /**
     * Sets the value of the ribs property.
     * 
     * @param value
     *     allowed object is
     *     {@link Transitions.Ribs }
     *     
     */
    public void setRibs(Transitions.Ribs value) {
        this.ribs = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setId(Long value) {
        this.id = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setVersion(Long value) {
        this.version = value;
    }

    /**
     * Gets the value of the deleted property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets the value of the deleted property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDeleted(Boolean value) {
        this.deleted = value;
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
     *       &lt;sequence>
     *         &lt;element name="rib" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence minOccurs="0">
     *                   &lt;element name="t" maxOccurs="unbounded">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                           &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                           &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                           &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *                 &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                 &lt;attribute name="version" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                 &lt;attribute name="deleted" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
        "rib"
    })
    public static class Ribs {

        @XmlElement(required = true)
        protected List<Transitions.Ribs.Rib> rib;

        /**
         * Gets the value of the rib property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the rib property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRib().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Transitions.Ribs.Rib }
         * 
         * 
         */
        public List<Transitions.Ribs.Rib> getRib() {
            if (rib == null) {
                rib = new ArrayList<Transitions.Ribs.Rib>();
            }
            return this.rib;
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
         *       &lt;sequence minOccurs="0">
         *         &lt;element name="t" maxOccurs="unbounded">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *                 &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *                 &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *                 &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *       &lt;attribute name="version" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *       &lt;attribute name="deleted" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
        public static class Rib {

            protected List<Transitions.Ribs.Rib.T> t;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
            @XmlSchemaType(name = "unsignedInt")
            protected long id;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
            @XmlSchemaType(name = "unsignedInt")
            protected long version;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions")
            protected Boolean deleted;

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
             * {@link Transitions.Ribs.Rib.T }
             * 
             * 
             */
            public List<Transitions.Ribs.Rib.T> getT() {
                if (t == null) {
                    t = new ArrayList<Transitions.Ribs.Rib.T>();
                }
                return this.t;
            }

            /**
             * Gets the value of the id property.
             * 
             */
            public long getId() {
                return id;
            }

            /**
             * Sets the value of the id property.
             * 
             */
            public void setId(long value) {
                this.id = value;
            }

            /**
             * Gets the value of the version property.
             * 
             */
            public long getVersion() {
                return version;
            }

            /**
             * Sets the value of the version property.
             * 
             */
            public void setVersion(long value) {
                this.version = value;
            }

            /**
             * Gets the value of the deleted property.
             * 
             * @return
             *     possible object is
             *     {@link Boolean }
             *     
             */
            public Boolean isDeleted() {
                return deleted;
            }

            /**
             * Sets the value of the deleted property.
             * 
             * @param value
             *     allowed object is
             *     {@link Boolean }
             *     
             */
            public void setDeleted(Boolean value) {
                this.deleted = value;
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
             *       &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
             *       &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
             *       &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
             *       &lt;attribute name="op" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class T {

                @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
                @XmlSchemaType(name = "unsignedInt")
                protected long tstamp;
                @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
                @XmlSchemaType(name = "unsignedInt")
                protected long snum;
                @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
                @XmlSchemaType(name = "unsignedInt")
                protected long changeset;
                @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
                protected String op;

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

            }

        }

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
     *       &lt;sequence>
     *         &lt;element name="styletransition" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
     *                 &lt;/sequence>
     *                 &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                 &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
     *                 &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
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
        "styletransition"
    })
    public static class Styles {

        @XmlElement(required = true)
        protected List<Transitions.Styles.Styletransition> styletransition;

        /**
         * Gets the value of the styletransition property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the styletransition property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getStyletransition().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Transitions.Styles.Styletransition }
         * 
         * 
         */
        public List<Transitions.Styles.Styletransition> getStyletransition() {
            if (styletransition == null) {
                styletransition = new ArrayList<Transitions.Styles.Styletransition>();
            }
            return this.styletransition;
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
         *       &lt;sequence>
         *         &lt;element ref="{http://schemas.openxmlformats.org/wordprocessingml/2006/main}style"/>
         *       &lt;/sequence>
         *       &lt;attribute name="snum" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *       &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *       &lt;attribute name="changeset" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "style"
        })
        public static class Styletransition {

            @XmlElement(namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main", required = true)
            protected Style style;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
            @XmlSchemaType(name = "unsignedInt")
            protected long snum;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
            @XmlSchemaType(name = "unsignedInt")
            protected long tstamp;
            @XmlAttribute(namespace = "http://www.plutext.org/transitions", required = true)
            @XmlSchemaType(name = "unsignedInt")
            protected long changeset;

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

        }

    }

}
