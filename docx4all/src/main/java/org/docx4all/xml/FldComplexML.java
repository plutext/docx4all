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

import java.util.List;

/**
 *	@author Jojada Tirtowidjojo - 17/08/2009
 */
public class FldComplexML extends ElementML {
	public FldComplexML() {
		super(null, false);
	}
	
	public Object clone() {
		return new FldComplexML();
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
	
	public void addChild(ElementML child) {
		addChild(child, false);
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (adopt) {
			throw new IllegalArgumentException("Cannot adopt.");
		}
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("Cannot become a child.");
		}
		super.addChild(idx, child, adopt);
		child.setGodParent(this);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		Boolean canAdd = false;
		
		int idx = after ? getChildrenCount() : 0;
		ElementML child = getChild(idx);
		if (child != null) {
			canAdd = child.canAddSibling(elem, after);		
		}
		
		return canAdd;
	}
	
	public void addSibling(ElementML elem, boolean after) {
		int idx = after ? getChildrenCount() : 0;
		getChild(idx).addSibling(elem, after);		
	}
	
	public void delete() {
		if (this.children != null) {
			for (ElementML ml: this.children) {
				ml.setGodParent(null);
			}
			this.children = null;
		}
	}
	
	public void deleteChild(ElementML child) {
		if (this.children != null) {
			this.children.remove(child);
			child.setGodParent(null);
		}
	}
	
	public RunML getSeparate() {
		RunML theRun = null;
		if (this.children != null) {
			for (ElementML ml: this.children) {
				if (ml instanceof RunML) {
					RunML run = (RunML) ml;
					org.docx4j.wml.FldChar fldChar =
						run.getFldChar();
					if (fldChar != null
						&& fldChar.getFldCharType() 
							== org.docx4j.wml.STFldCharType.SEPARATE) {
						theRun = run;
					}
				}
			}
		}
		return theRun;
	}
	
	protected List<Object> getDocxChildren() {
		return null;
	}
	
	protected void init(Object docxObject) {
		//do nothing
	}
	
	public void setParent(ElementML parent) {
		throw new UnsupportedOperationException("Cannot have parent.");
	}
	
	public void setDocxParent(Object docxParent) {
		throw new UnsupportedOperationException("Cannot have parent.");
	}
	
}// FldComplexML class



















