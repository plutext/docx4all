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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;

/**
 *	@author Jojada Tirtowidjojo - 16/04/2008
 */
public class SdtBlockML extends ElementML {
	private static Logger log = Logger.getLogger(SdtBlockML.class);

	private SdtPrML sdtPr;

	public SdtBlockML(org.docx4j.wml.SdtBlock sdtBlock) {
		this(sdtBlock, false);
	}

	public SdtBlockML(org.docx4j.wml.SdtBlock sdtBlock, boolean isDummy) {
		super(sdtBlock, isDummy);
	}

	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public SdtPrML getSdtProperties() {
		return this.sdtPr;
	}

	public void setSdtProperties(SdtPrML sdtPr) {
		if (sdtPr != null && sdtPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}

		this.sdtPr = sdtPr;

		if (this.docxObject != null) {
			org.docx4j.wml.SdtPr newDocxSdtPr = null;
			if (sdtPr != null) {
				sdtPr.setParent(SdtBlockML.this);
				newDocxSdtPr = (org.docx4j.wml.SdtPr) sdtPr.getDocxObject();
			}
			((org.docx4j.wml.SdtBlock) this.docxObject).setSdtPr(newDocxSdtPr);

			if (newDocxSdtPr != null) {
				newDocxSdtPr.setParent(this.docxObject);
			}
		}
	}

	public Object clone() {
		org.docx4j.wml.SdtBlock obj = null;
		if (this.docxObject != null) {
			obj = (org.docx4j.wml.SdtBlock) XmlUtils.deepCopy(this.docxObject);
		}

		return new SdtBlockML(obj, this.isDummy);
	}

	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;

		if (!(child instanceof ParagraphML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}

		return canAdd;
	}

	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}

	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof BodyML)) {
			throw new IllegalArgumentException("NOT a BodyML.");
		}
		this.parent = parent;
	}

	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;

		if (this.docxObject == null) {
			;//do nothing
		} else {
			org.docx4j.wml.SdtBlock sdtBlock = 
				(org.docx4j.wml.SdtBlock) this.docxObject;
			theChildren = sdtBlock.getSdtContent().getEGContentBlockContent();
		}

		return theChildren;
	}

	protected void init(Object docxObject) {
		initSdtProperties((org.docx4j.wml.SdtBlock) docxObject);
		initChildren((org.docx4j.wml.SdtBlock) docxObject);
	}

	private void initSdtProperties(org.docx4j.wml.SdtBlock sdtBlock) {
		this.sdtPr = null;
		if (sdtBlock != null) {
			//if not an implied SdtBlockML
			org.docx4j.wml.SdtPr pr = sdtBlock.getSdtPr();
			if (pr != null) {
				this.sdtPr = new SdtPrML(pr);
				this.sdtPr.setParent(SdtBlockML.this);
			}
		}
	}

	private void initChildren(org.docx4j.wml.SdtBlock sdtBlock) {
		this.children = null;

		if (sdtBlock == null) {
			return;
		}

		List<Object> list = sdtBlock.getSdtContent().getEGContentBlockContent();
		if (!list.isEmpty()) {
			this.children = new ArrayList<ElementML>(list.size());
			for (Object obj : list) {
				ElementML ml = null;
				//if (obj instanceof org.docx4j.wml.Tbl) {
					//unsupported yet
				//} else {
					ml = new ParagraphML(obj);
				//}
				
				ml.setParent(SdtBlockML.this);
				this.children.add(ml);
			}
		}
	}// initChildren()

}// SdtBlockML class

