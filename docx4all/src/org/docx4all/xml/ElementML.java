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
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementML {
	protected WordML.Tag tag;
	protected ElementML parent;
	protected List<ElementML> children;
	
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
	
} //ElementML class






















