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

import javax.swing.text.AttributeSet;
import javax.swing.text.Style;

import org.docx4all.xml.ElementML;

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public interface DocumentElement extends javax.swing.text.Element {
	public ElementML getElementML();
	public boolean isEditable();
	public void setResolveParent(AttributeSet parent);
	public String getStyleNameInAction();
	public boolean isTheOnlyChild();
}// DocumentElement class



















