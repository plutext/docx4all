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

package org.plutext.client.wrappedTransforms;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.plutext.client.state.Controls;
import org.plutext.client.ServerFrom;
import org.plutext.transforms.Context;
import org.plutext.transforms.Transforms.T;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.Tag;
import java.math.BigInteger;
import org.docx4j.wml.Id;

public abstract class TransformAbstract
{

    // The ID of the wrapped SDT. This is *not* the ID of the transformation.
    // TODO - rename, so that this is more obvious.
    protected Id id;
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}


    // The tag of the wrapped SDT
    protected Tag tag;
	public Tag getTag() {
		return tag;
	}
	public void setTag(Tag tag) {
		this.tag = tag;
	}


    // Has this transform been applied to the document yet?
    Boolean applied = false;
	public Boolean getApplied() {
		return applied;
	}
	public void setApplied(Boolean applied) {
		this.applied = applied;
	}


    // The ID of the transformation.
    protected int sequenceNumber = 0;
	public int getSequenceNumber() {
		return sequenceNumber;
	}


	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
	protected SdtBlock sdt = null;	
	public SdtBlock getSdt() {
		return sdt;
	}
	
	public T t = null;
	
    public TransformAbstract(T t)
    {
    	this.t = t;
    	
        sequenceNumber =  t.getSnum();

        sdt = t.getSdt();

        if (t.getIdref() != 0)
        {
            // Case: Delete 
            
            // Convert the idref to an id object
            org.docx4j.wml.ObjectFactory factory = new org.docx4j.wml.ObjectFactory();
            id = factory.createId();
            id.setVal(BigInteger.valueOf(t.getIdref()));
            
            
            
        } else if (t.getOp().equals("style")) { 
        
            // No ID
        
        } else {
        	
            // Case: Update, Insert

            id = sdt.getSdtPr().getId();

            tag = sdt.getSdtPr().getTag();

        }

        //log.warn("Parsed SDT ID " + id);
        

    }


    /* Code to apply the transform */
    	// TODO - think through method signature
    public abstract int apply(ServerFrom serverFrom);



}
