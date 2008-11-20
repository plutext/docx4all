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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.docx4j.XmlUtils;

/**
 *	@author Jojada Tirtowidjojo - 20/11/2008
 */
public class HyperlinkML extends ElementML {
	
	public final static String encodeTarget(HyperlinkML ml, String basePath) {
		String target = ml.getTarget().replace('\\', '/');
		if (target.indexOf("://") > 0) {
			//if protocol is specified, basePath param is ignored
			try {
				target = URLDecoder.decode(target, "UTF-8");
			} catch (UnsupportedEncodingException exc) {
				//should not happen
			}
		} else if (basePath != null) {
			//protocol is NOT specified.
			//Append target to basePath param.
			StringBuilder sb = new StringBuilder();
			sb.append(basePath.replace('\\', '/'));
			if (!basePath.endsWith("/")) {
				sb.append("/");
			}
			if (target.startsWith("/")) {
				target = target.substring(1);
			}
			sb.append(target);
			target = sb.toString();
		}
		return target;
	}
	
	public HyperlinkML(Object docxObject) {
		this(docxObject, false);
	}
	
	public HyperlinkML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public String getId() {
		return ((org.docx4j.wml.P.Hyperlink) this.docxObject).getId();
	}
	
	public void setId(String id) {
		((org.docx4j.wml.P.Hyperlink) this.docxObject).setId(id);
	}
	
	public String getTarget() {
		org.docx4j.openpackaging.parts.relationships.RelationshipsPart part = 
			getWordprocessingMLPackage().getMainDocumentPart().getRelationshipsPart();
		org.docx4j.relationships.Relationship rel = part.getRelationshipByID(getId());
		return rel.getTarget();
	}
	
	public void setTarget(String s) {
		org.docx4j.openpackaging.parts.relationships.RelationshipsPart part = 
			getWordprocessingMLPackage().getMainDocumentPart().getRelationshipsPart();
		org.docx4j.relationships.Relationship rel = part.getRelationshipByID(getId());
		rel.setTarget(s);
	}
	
	public String getTooltip() {
		return ((org.docx4j.wml.P.Hyperlink) this.docxObject).getTooltip();
	}
	
	public void setTooltip(String s) {
		((org.docx4j.wml.P.Hyperlink) this.docxObject).setTooltip(s);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		boolean canAdd = false;
		
		if (elem instanceof RunML 
			|| elem instanceof RunDelML 
			|| elem instanceof RunDelML
			|| elem instanceof HyperlinkML) {
			canAdd = super.canAddSibling(elem, after);
		}
		
		return canAdd;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new HyperlinkML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML.");
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		if (this.docxObject instanceof org.docx4j.wml.P.Hyperlink) {
			theChildren = ((org.docx4j.wml.P.Hyperlink) this.docxObject).getParagraphContent();
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.P.Hyperlink link = null;
		
		if (docxObject == null) {
			;// implied HyperlinkML
			
		} else if (docxObject instanceof org.docx4j.wml.P.Hyperlink) {
			link = (org.docx4j.wml.P.Hyperlink) docxObject;
			this.isDummy = false;
			
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = "
					+ docxObject);
		}

		initChildren(link);
	}
	
	private void initChildren(org.docx4j.wml.P.Hyperlink link) {
		this.children = null;
		
		if (link == null) {
			return;
		}
		
		List<Object> pKids = link.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(HyperlinkML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	
}// HyperlinkML class



















