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

package org.docx4all.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.xml.namespace.QName;

import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.PropertiesContainerML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;

import com.sun.org.apache.xerces.internal.dom.NodeImpl;

/**
 *	@author Jojada Tirtowidjojo - 04/01/2008
 */
public class XmlUtil {

	public final static String getEnclosingTagPair(QName qname) {
		return getEnclosingTagPair(qname.getPrefix(), qname.getLocalPart());
	}
	
	public final static String getEnclosingTagPair(NodeImpl node) {
		return getEnclosingTagPair(node.getPrefix(), node.getLocalName());
	}
	
	private final static String getEnclosingTagPair(String prefix, String localName) {
		if (prefix == null) {
			prefix = "";
		} else if (prefix.length() > 0) {
			prefix = prefix + ":";
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<");
		sb.append(prefix);
		sb.append(localName);
		sb.append(">");
		sb.append("</");
		sb.append(prefix);
		sb.append(localName);
		sb.append(">");
		
		return sb.toString();
	}
	
	/**
	 * Empty the children of parent argument
	 * 
	 * @param parent the element whose children are to be deleted.
	 * @return The deleted children
	 */
	public final static List<ElementML> deleteChildren(ElementML parent) {
		List<ElementML> children = new ArrayList<ElementML>(parent.getChildren());
		for (ElementML elem: children) {
			elem.delete();
		}
		return children;
	}
	
	public final static RunContentML getLastRunContentML(ElementML root) {
		RunContentML theElem = null;
		
		if (root.getChildrenCount() > 0) {
			ElementML lastChild = root.getChild(root.getChildrenCount() - 1);
			if (lastChild instanceof RunContentML) {
				theElem = (RunContentML) lastChild;
			} else {
				theElem = getLastRunContentML(lastChild);
			}
		} else if (root instanceof RunContentML) {
			theElem = (RunContentML) root;
		}
		
		return theElem;
	}
	
	public final static int getIteratedIndex(ElementML root, ElementML target) {
		int theIdx = -1;
		
		ElementMLIterator it = new ElementMLIterator(root);
		int i = -1;
		while (it.hasNext() && theIdx == -1) {
			i++;
			ElementML elem = it.next();
			if (elem == target) {
				theIdx = i;
			}
		}
		
		return theIdx;
	}
	
	public final static ElementML getElementMLAtIteratedIndex(ElementML root, int idx) {
		ElementML theElem = null;
		
		ElementMLIterator it = new ElementMLIterator(root);
		int i = -1;
		while (it.hasNext() && i < idx) {
			i++;
			theElem = it.next();
		}
		
		if (i != idx) {
			theElem = null;
		}
		
		return theElem;
	}
	
	public final static void setAttributes(
		ElementML elem, 
		AttributeSet paragraphAttrs, 
		AttributeSet runAttrs,
		boolean replace) {
		
		ElementMLIterator it = new ElementMLIterator(elem);
		while (it.hasNext()) {
			ElementML ml = it.next();
			if (runAttrs != null && (ml instanceof RunML)) {
				PropertiesContainerML prop = ((RunML) ml).getRunProperties();
				if (replace) {
					prop.removeAttributes(prop.getAttributeSet());
				}
				prop.addAttributes(runAttrs);
				
			} else if (paragraphAttrs != null && (ml instanceof ParagraphML)) {
				PropertiesContainerML prop = ((ParagraphML) ml).getParagraphProperties();
				if (replace) {
					prop.removeAttributes(prop.getAttributeSet());
				}
				prop.addAttributes(paragraphAttrs);
			}
		}
	}
	
	private XmlUtil() {
		;//uninstantiable
	}
}// XmlUtil class



















