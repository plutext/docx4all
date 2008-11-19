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
import org.docx4all.xml.HyperlinkML;
import org.docx4all.xml.RunDelML;
import org.docx4all.xml.RunInsML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 28/08/2008
 */
public class LabelView extends javax.swing.text.LabelView {
	private boolean impliedUnderline;
	private boolean impliedStrikethrough;
	private Color foreground;
	
    public LabelView(Element elem) {
    	super(elem);
    	
    	impliedUnderline = impliedStrikethrough = false;
    	foreground = null;
    	
    	ElementML parent = 
    		((DocumentElement) getElement().getParentElement()).getElementML();
    	if (parent instanceof RunML) {
    		if (parent.getParent() instanceof RunInsML) {
    			foreground = Color.RED;
    			impliedUnderline = true;
    		} else if (parent.getParent() instanceof RunDelML) {
    			foreground = Color.RED;
    			impliedStrikethrough = true;
    		} else if (parent.getParent() instanceof HyperlinkML) {
    			foreground = Color.BLUE;
    			impliedUnderline = true;
    		}
    	}
    }

    public boolean isImpliedUnderline() {
    	return impliedUnderline;
    }
    
    public boolean isImpliedStrikethrough() {
    	return impliedStrikethrough;
    }
    
    public Color getForeground() {
    	return (foreground == null) ? super.getForeground() : foreground;
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



















