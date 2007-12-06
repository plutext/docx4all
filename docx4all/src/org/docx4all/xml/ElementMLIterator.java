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

import java.util.Stack;

import org.apache.log4j.Logger;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementMLIterator {
	private static Logger log = Logger.getLogger(ElementMLIterator.class);

	private final ElementML root;
	
	private Stack<StackEntry> stack;
	
	public ElementMLIterator(ElementML root) throws IllegalArgumentException {
		if (root == null) {
			throw new IllegalArgumentException("Root is NULL");
		}
		this.root = root;
	}
	
    public boolean hasNext() {
    	boolean hasNext = false;
    	
    	if (stack == null) {
    		hasNext = true;
    		
    	} else if (stack.isEmpty()) {
    		;//false
    		
    	} else {
    		StackEntry se = stack.peek();
    		if (se.hasNext()) {
    			hasNext = true;
    		} else if (stack.size() > 1) {
    			hasNext = true;
    		}
    	}
    	
    	return hasNext;
    }
    
    public ElementML next() {
    	if (stack == null) {
       		stack = new Stack<StackEntry>();
       		stack.push(new StackEntry(root));
    		return root;
    	}
    	
    	if (stack.isEmpty()) {
    		return null;
    	}
    	
    	ElementML theElem = null;
    	
    	StackEntry se = stack.peek();
    	if (se.hasNext()) {
    		theElem = se.next();
    		stack.push(new StackEntry(theElem));
    	} else {
    		stack.pop();
    		theElem = next();
    	}
    	return theElem;
    }
    
    public void cruise(Callback callback) {
    	if (stack == null) {
       		stack = new Stack<StackEntry>();
       		stack.push(new StackEntry(root));
       		callback.handleStartElement(root);
    		cruise(callback);
    		
    	} else if (!stack.isEmpty()) {
			StackEntry se = stack.peek();
			if (se.hasNext()) {
				ElementML elem = se.next();
				stack.push(new StackEntry(elem));
				callback.handleStartElement(elem);
			} else {
				stack.pop();
				callback.handleEndElement(se.elem);
			}
			cruise(callback);
		}
    }
    
    public static class Callback {
    	public void handleStartElement(ElementML elem) {
    		;//method template
    	}
    	
    	public void handleEndElement(ElementML elem) {
    		;//method template
    	}
    }// Callback inner class
    
    private final class StackEntry {
    	private ElementML elem;
    	private int childIdx;
    	
    	private StackEntry(ElementML elem) {
    		this.elem = elem;
    		this.childIdx = -1;
    	}
    	
    	private boolean hasNext() {
    		return (childIdx + 1) < elem.getChildrenCount();
    	}
    	
    	private ElementML next() {
    		return elem.getChild(++childIdx);
    	}
    }// StackEntry inner class
    
}// ElementMLIterator class



























