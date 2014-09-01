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

package org.docx4all.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.CompositeView;
import javax.swing.text.Element;
import javax.swing.text.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunView extends CompositeView {
	private static Logger log = LoggerFactory.getLogger(RunView.class);

	public RunView(Element elem) {
		super(elem);
	}
	
	public View getTextView(int pos) {
		return super.getViewAtPosition(pos, null);
	}
	
    public void setParent(View parent) {
    	super.setParent(parent);
    	
    	//Because javax.swing.text.View.setParent()
    	//resets the parent field of all children
    	//when 'parent' is null we passes on
    	//the new parent to all children.
    	//This behaviour is specifically needed when 
    	//'parent' is javax.swing.text.FlowView$LogicalView;
    	//ie: parent.getParent() instanceof ImpliedParagraphView.
    	if (parent != null
    		&& parent.getParent() instanceof ImpliedParagraphView) {
    		for (int i=0; i < getViewCount(); i++) {
    			View v = getView(i);
    			v.setParent(this);
    		}
    	}
    }
    
    public float getPreferredSpan(int axis) {
		float maxpref = 0;
		float pref = 0;
		int n = getViewCount();
		for (int i = 0; i < n; i++) {
			View v = getView(i);
			pref += v.getPreferredSpan(axis);
			if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) >= ForcedBreakWeight) {
				maxpref = Math.max(maxpref, pref);
				pref = 0;
			}
		}
		maxpref = Math.max(maxpref, pref);
		return maxpref;
	}

    public void paint(Graphics g, Shape allocation) {
    	;//do nothing
	}

    protected void childAllocation(int index, Rectangle a) {
    	;//do nothing
    }
	
    protected boolean isBefore(int x, int y, Rectangle alloc) {
    	return false;
    }

    protected boolean isAfter(int x, int y, Rectangle alloc) {
    	return false;
    }

    protected View getViewAtPoint(int x, int y, Rectangle alloc) {
    	return null;
    }

}// RunView class




























