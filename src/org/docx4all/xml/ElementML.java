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

import javax.xml.namespace.QName;

import org.docx4all.ui.main.Constants;
import org.docx4j.jaxb.Context;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public abstract class ElementML implements Cloneable {
	public final static ParagraphML IMPLIED_PARAGRAPH;
	public final static RunML IMPLIED_RUN;
	public final static RunContentML IMPLIED_NEWLINE;
	
	static {
		IMPLIED_PARAGRAPH = new ParagraphML(null);
		IMPLIED_RUN = new RunML(null);
		IMPLIED_NEWLINE = new RunContentML(null, true);
		IMPLIED_NEWLINE.setTextContent(Constants.NEWLINE);
	}
	
	protected Object docxObject;
	protected boolean isDummy;
	protected WordML.Tag tag;
	protected ElementML parent;
	protected List<ElementML> children;
	
	public ElementML() {
		;//do nothing
	}
	
	public ElementML(Object docxObject, boolean isDummy) {
		this.docxObject = docxObject;
		this.isDummy = isDummy;
		if (this.docxObject != null) {
			QName name = Context.jc.createJAXBIntrospector().getElementName(docxObject);
			if (name != null) {
				tag = WordML.getTag(name.getLocalPart());
			}
		}
		init(docxObject);
	}
	
	public abstract void setParent(ElementML parent);
	public abstract void setDocxParent(Object docxParent);
	public abstract Object clone();

	protected abstract void init(Object docxObject);
	protected abstract List<Object> getDocxChildren();
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (child.getParent() != null) {
			canAdd = false;
		} else if (this.children == null) {
			canAdd = (idx == 0);
		}
		
		return canAdd;
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		boolean canAdd = true;
		
		if (elem.getParent() != null) {
			canAdd = false;
		} else if (getParent() == null) {
			canAdd = false;
		} else {
			int idx = getParent().getChildIndex(this);
			if (idx < 0) {
				canAdd = false;
			} else {
				if (after) {
					idx++;
				}
				canAdd = getParent().canAddChild(idx, elem);
			}
		}
		
		return canAdd;
	}
	
	public void addSibling(ElementML elem, boolean after) {		
		if (elem.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		if (getParent() == null) {
			throw new IllegalStateException("Parent is NULL.");
		}
		
		int idx = getParent().getChildIndex(this);
		if (idx < 0) {
			throw new IllegalStateException("Index position not found.");
		}
		
		if (after) {
			idx++;
		}
		
		getParent().addChild(idx, elem);
	}
	
	public boolean canAddChild(ElementML child) {
		int idx = (getChildren() == null) ? 0 : getChildren().size();
		return canAddChild(idx, child);		
	}
	
	public void addChild(ElementML child) {
		int idx = (getChildren() == null) ? 0 : getChildren().size();
		addChild(idx, child);
	}
	
	public void addChild(int idx, ElementML child) {
		if (this.children == null) {
			if (idx == 0) {
				//Add to this ElementML's children
				this.children = new ArrayList<ElementML>();
				this.children.add(child);
				child.setParent(ElementML.this);
				
				//Add to Docx structure
				if (getDocxObject() != null && child.getDocxObject() != null) {
					List<Object> list = getDocxChildren();
					list.add(child.getDocxObject());
					child.setDocxParent(getDocxObject());
				}
			} else {
				throw new IndexOutOfBoundsException("Index: "+idx+", Size: 0");
			}
			
		} else {
			//Add to this ElementML's children
			this.children.add(idx, child);
			child.setParent(ElementML.this);
			
			//Add to Docx structure
			if (getDocxObject() != null && child.getDocxObject() != null) {
				List<Object> list = getDocxChildren();

				//The index position in the Docx structure may
				//be different from that in this ElementML structure.
				//Therefore, we find the index position from siblings.
				//TODO: Should we care about this difference ?
				
				//Browse older siblings for index position
				int siblingIndex = -1;
				if (idx > 0) {
					for (int i = idx - 1; 0 <= i && siblingIndex == -1; i--) {
						Object obj = ((ElementML) this.children.get(i)).getDocxObject();
						siblingIndex = list.indexOf(obj);
					}
				}
				
				if (siblingIndex > -1) {
					list.add(siblingIndex + 1, child.getDocxObject());
					child.setDocxParent(getDocxObject());
					
				} else if (idx < this.children.size() - 1){
					//Browse younger siblings for index position
					for (int i = idx + 1; i < this.children.size() && siblingIndex == -1; i++) {
						Object obj = ((ElementML) this.children.get(i)).getDocxObject();
						siblingIndex = list.indexOf(obj);
					}

					if (siblingIndex > -1) {
						list.add(siblingIndex, child.getDocxObject());
						child.setDocxParent(getDocxObject());
					}
				}
				
				if (siblingIndex == -1) {
					//Add child anyway
					list.add(child.getDocxObject());
					child.setDocxParent(getDocxObject());
				}
			}
		}
	}
	
	public void delete() {
		if (getParent() == null) {
			return;
		}
		getParent().deleteChild(ElementML.this);
	}
	
	public void deleteChild(ElementML child) {
		if (this.children == null) {
			//delete from Docx structure
			if (getDocxObject() != null && child.getDocxObject() != null) {
				List<Object> list = getDocxChildren();
				list.remove(child.getDocxObject());
				child.setDocxParent(null);
			}
		} else {
			//Delete from this ElementML's children
			this.children.remove(child);
			child.setParent(null);
			
			//delete from Docx structure
			if (getDocxObject() != null && child.getDocxObject() != null) {
				List<Object> list = getDocxChildren();
				list.remove(child.getDocxObject());
				child.setDocxParent(null);
			}
		}
	}
	
	public int getChildIndex(ElementML elem) {
		return (this.children != null && elem != null) ? this.children.indexOf(elem) : -1;
	}
	
	/**
	 * The DOM element associated with this ElementML.
	 * 
	 * @return docxObject DOM element
	 */
	public Object getDocxObject() {
		return this.docxObject;
	}
	
	/**
	 * An implied ElementML is an ElementML that
	 * does not have a DOM element associated with it.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 * @see getDocxObject()
	 */
	public boolean isImplied() {
		return this.docxObject == null;
	}
	
	/**
	 * A dummy ElementML is an ElementML that is declared as dummy.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public boolean isDummy() {
		return isDummy;
	}
	
	public WordML.Tag getTag() {
		return this.tag;
	}
	
	public ElementML getParent() {
		return this.parent;
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






















