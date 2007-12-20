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

import org.docx4all.ui.main.Constants;
import org.docx4j.jaxb.document.ObjectFactory;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public abstract class ElementML {
	public final static ParagraphML IMPLIED_PARAGRAPH = 
		new ParagraphML(null, true);
	public final static RunML IMPLIED_RUN = 
		new RunML(null, true);
	public final static RunContentML IMPLIED_NEWLINE = 
		new RunContentML(null, Constants.NEWLINE, true);
	
	protected boolean isDummy;
	protected WordML.Tag tag;
	protected ElementML parent;
	protected List<ElementML> children;
	
	/**
	 * An implied ElementML is an ElementML that
	 * does not have a DOM element associated with it.
	 * This kind of ElementML may still have a WordML.Tag.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public abstract boolean isImplied();
	
	/**
	 * A dummy ElementML is an ElementML that is implied 
	 * or declared as dummy.
	 * This kind of ElementML may still have a WordML.Tag.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public boolean isDummy() {
		return isImplied() || isDummy;
	}
	
	public WordML.Tag getTag() {
		return tag;
	}
	
	public ElementML getParent() {
		return this.parent;
	}
	
	public void setParent(ElementML parent) {
		this.parent = parent;
	}
	
	public List<ElementML> getChildren() {
		return this.children;
	}
	
	public int getChildrenCount() {
		return (this.children == null) ? 0 : this.children.size();
	}
	
	public ElementML getChild(int idx) {
		if (this.children != null && !this.children.isEmpty()) {
			return this.children.get(idx);
		}
		return null;
	}
	
	public boolean isBlockElement() {
		return getTag().isBlockTag();
	}
	
	public boolean breaksFlow() {
		return getTag().breaksFlow();
	}
	
	public String toString() {
		String dummy = "";
		if (isImplied()) {
			dummy = "IMPLIED_";
		} else if (isDummy()) {
			dummy = "DUMMY_";
		}
		
		StringBuffer sb = new StringBuffer(dummy);
		sb.append(getClass().getSimpleName());
		sb.append("@");
		sb.append(hashCode());
		
		return sb.toString();
	}
	
} //ElementML class






















