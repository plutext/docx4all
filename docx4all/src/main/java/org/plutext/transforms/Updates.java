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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element ref="{http://www.plutext.org/transforms}transforms"/>
 *         &lt;element ref="{http://www.plutext.org/transforms}changesets"/>
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
    "transforms",
    "changesets"
})
@XmlRootElement(name = "updates")
public class Updates {

    @XmlElement(required = true)
    protected Transforms transforms;
    @XmlElement(required = true)
    protected Changesets changesets;

    /**
     * Gets the value of the transforms property.
     * 
     * @return
     *     possible object is
     *     {@link Transforms }
     *     
     */
    public Transforms getTransforms() {
        return transforms;
    }

    /**
     * Sets the value of the transforms property.
     * 
     * @param value
     *     allowed object is
     *     {@link Transforms }
     *     
     */
    public void setTransforms(Transforms value) {
        this.transforms = value;
    }

    /**
     * Gets the value of the changesets property.
     * 
     * @return
     *     possible object is
     *     {@link Changesets }
     *     
     */
    public Changesets getChangesets() {
        return changesets;
    }

    /**
     * Sets the value of the changesets property.
     * 
     * @param value
     *     allowed object is
     *     {@link Changesets }
     *     
     */
    public void setChangesets(Changesets value) {
        this.changesets = value;
    }

}
