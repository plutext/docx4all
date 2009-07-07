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
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.wml.Id;
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
		String id = null;
		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();
		if (sdtPr.getTag() != null) {
			id = SdtWrapper.getPlutextId(getDocxSdtPr());
		}
		return id;
	}
	
	public void setPlutextId(String id) {
		org.docx4j.wml.SdtPr sdtPr = getDocxSdtPr();
		String version = SdtWrapper.getVersionNumber(sdtPr);

		Id sdtId = new Id();
		sdtId.setVal(BigInteger.valueOf(Long.valueOf(id).longValue()));
		sdtPr.setId(sdtId);
		
		String tagValue = SdtWrapper.generateTag(id, version);
		sdtPr.setTag(ObjectFactory.createTag(tagValue));
	}
	
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



















