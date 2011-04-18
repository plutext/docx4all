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

package org.docx4all.xml.drawing.type;

import org.docx4all.swing.text.StyleSheet;

/**
 *	@author Jojada Tirtowidjojo - 17/12/2008
 */
public class CTEffectExtent {
	private org.docx4j.dml.wordprocessingDrawing.CTEffectExtent ctEffectExtent;
	
	public CTEffectExtent(org.docx4j.dml.wordprocessingDrawing.CTEffectExtent ctEffectExtent) {
		this.ctEffectExtent = ctEffectExtent;
	}
	
	public Object getDocxObject() {
		return this.ctEffectExtent;
	}
	
	public int getLeftInPixels() {
		int i = Long.valueOf(ctEffectExtent.getL()).intValue();
		return StyleSheet.emuToPixels(i);
	}
	
	public int getRightInPixels() {
		int i = Long.valueOf(ctEffectExtent.getR()).intValue();
		return StyleSheet.emuToPixels(i);
	}
	
	public int getTopInPixels() {
		int i = Long.valueOf(ctEffectExtent.getT()).intValue();
		return StyleSheet.emuToPixels(i);
	}
	
	public int getBottomInPixels() {
		int i = Long.valueOf(ctEffectExtent.getB()).intValue();
		return StyleSheet.emuToPixels(i);
	}
	
}// CTEffectExtent class



















