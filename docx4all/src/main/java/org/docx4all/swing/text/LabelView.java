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

import java.awt.Color;

import javax.swing.text.Element;

import org.docx4all.xml.ElementML;
import org.docx4all.xml.RunDelML;
import org.docx4all.xml.RunInsML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 28/08/2008
 */
public class LabelView extends javax.swing.text.LabelView {
	private boolean impliedUnderline;
	private boolean impliedStrikethrough;
	
    public LabelView(Element elem) {
    	super(elem);
    	
    	ElementML parent = 
    		((DocumentElement) elem.getParentElement()).getElementML();
    	impliedUnderline = 
    		(parent instanceof RunML)
    		&& (parent.getParent() instanceof RunInsML);
    	impliedStrikethrough = 
    		(parent instanceof RunML)
    		&& (parent.getParent() instanceof RunDelML);
    		
    }

    public boolean isImpliedUnderline() {
    	return impliedUnderline;
    }
    
    public boolean isImpliedStrikethrough() {
    	return impliedStrikethrough;
    }
    
    public Color getForeground() {
    	Color c =
    		(isImpliedUnderline() || isImpliedStrikethrough())
    			? Color.RED 
    			: super.getForeground();
    	return c;
    }
    
    protected void setPropertiesFromAttributes() {
    	super.setPropertiesFromAttributes();
    	if (isImpliedUnderline()) {
    		setUnderline(true);
    	}
    	if (isImpliedStrikethrough()) {
    		setStrikeThrough(true);
    	}
    }
    
}// LabelView class



















