/*
 *  Copyright 2007, Plutext Pty Ltd.
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

package org.docx4all.xml;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.model.sdt.QueryString;
import org.plutext.client.SdtWrapper;

/**
 *	@author Jojada Tirtowidjojo - 16/04/2008
 */
public class SdtPrML  extends ElementML {
	private static Logger log = Logger.getLogger(SdtPrML.class);

	public SdtPrML(org.docx4j.wml.SdtPr sdtPr) {
		this(sdtPr, false);
	}

	public SdtPrML(org.docx4j.wml.SdtPr sdtPr, boolean isDummy) {
		super(sdtPr, isDummy);
	}

	public String getPlutextId() {
//		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();		
//		org.docx4j.wml.Id id = sdtPr.getId();
//		BigInteger value = (id == null) ? null : id.getVal();
//		return value;
		
		return SdtWrapper.getPlutextId(getDocxSdtPr());
	}
	
//	public void setIdValue(BigInteger val) {
//		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();
//		org.docx4j.wml.Id id = sdtPr.getId();
//		if (id == null) {
//			id = ObjectFactory.createId(val);
//			sdtPr.setId(id);
//		} else {
//			id.setVal(val);
//		}
//	}
	
	public String getTagValue() {
		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();
		org.docx4j.wml.Tag tag = sdtPr.getTag();
		String value = (tag == null) ? null : tag.getVal();
		return value;
	}
	
	public void setTagValue(String val) {
		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();
		org.docx4j.wml.Tag tag = sdtPr.getTag();
		if (tag == null) {
			tag = ObjectFactory.createTag(val);
			sdtPr.setTag(tag);
		} else {
			tag.setVal(val);
		}
	}
	
	public Object clone() {
		org.docx4j.wml.SdtPr obj = null;
		if (this.docxObject != null) {
			obj = (org.docx4j.wml.SdtPr) XmlUtils.deepCopy(this.docxObject);
		}

		return new SdtPrML(obj, this.isDummy);
	}

	public boolean canAddChild(int idx, ElementML child) {
		//Cannot add child to this SdtPrML object.
		//Properties are set by calling its corresponding setter method.
		return false;
	}

	public void addChild(int idx, ElementML child, boolean adopt) {
		throw new UnsupportedOperationException(
			"Properties should be set by calling its corresponding setter method.");
	}

	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof SdtBlockML)) {
			throw new IllegalArgumentException("NOT a SdtBlockML.");
		}
		this.parent = parent;
	}

	protected List<Object> getDocxChildren() {
		return null;
	}

	protected void init(Object docxObject) {
		;//do nothing
	}

	private org.docx4j.wml.SdtPr getDocxSdtPr() {
		return (org.docx4j.wml.SdtPr) getDocxObject();
	}
}// SdtPrML class



















