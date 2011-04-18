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

import java.awt.Dimension;
import java.util.List;

import org.docx4all.swing.text.StyleSheet;
import org.docx4all.ui.main.Constants;
import org.docx4all.xml.drawing.Graphic;
import org.docx4all.xml.drawing.type.CTEffectExtent;
//import org.docx4all.xml.drawing.type.CTEffectExtent;
import org.docx4j.XmlUtils;

/**
 *	@author Jojada Tirtowidjojo - 15/12/2008
 */
public class InlineDrawingML extends RunContentML {
	private Dimension extentInPixels;
	private CTEffectExtent effectExtent;
	private Graphic graphic;
	
	public InlineDrawingML(Object docxObject) {
		super(docxObject);
	}
	
	public InlineDrawingML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public String getTextContent() {
		return this.textContent;
	}
	
	public void setTextContent(String textContent) {
		//textContent is fixed.
	}
	
	public org.docx4j.dml.CTNonVisualDrawingProps getDocPr() {
		org.docx4j.dml.CTNonVisualDrawingProps docPr =
			getInline().getDocPr();
		return docPr;
	}
	
	public void setDocPr(org.docx4j.dml.CTNonVisualDrawingProps docPr) {
		getInline().setDocPr(docPr);
	}
	
	public Graphic getGraphic() {
		return this.graphic;
	}
	
	public Dimension getExtentInPixels() {
		return this.extentInPixels; 
	}
	
	public CTEffectExtent getEffectExtent() {
		return this.effectExtent;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		return new InlineDrawingML(obj, this.isDummy);
	}
	
	protected org.docx4j.dml.wordprocessingDrawing.Inline getInline() {
		org.docx4j.wml.Drawing drawing =
			(org.docx4j.wml.Drawing) this.docxObject;
		return (org.docx4j.dml.wordprocessingDrawing.Inline) drawing.getAnchorOrInline().get(0);
	}
	
	protected void init(Object docxObject) {
		if (docxObject == null) {
			//implied DrawingML.
			//Will an implied DrawingML ever be needed ?
			//or can this method just simply throw IllegalArgumentException ?
			
		} else if (docxObject instanceof org.docx4j.wml.Drawing) {
			this.textContent = Constants.SINGLE_SPACE;
			this.isDummy = false;
			org.docx4j.wml.Drawing drawing = (org.docx4j.wml.Drawing) docxObject;
			List<Object> list = drawing.getAnchorOrInline();
			if (list.size() != 1
				|| !(list.get(0) instanceof org.docx4j.dml.wordprocessingDrawing.Inline)) {
				//There should not be an Anchor in 'list'
				//because it is not being supported and 
				//RunML.initChildren() prevents it from
				//being assigned to this InlineDrawingML object.
				//See: RunML.initChildren().
				throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
			}
			
			org.docx4j.dml.wordprocessingDrawing.Inline inline = (org.docx4j.dml.wordprocessingDrawing.Inline) list.get(0);
			if (inline.getExtent() != null) {
				int cx = Long.valueOf(inline.getExtent().getCx()).intValue();
				cx = StyleSheet.emuToPixels(cx);
				int cy = Long.valueOf(inline.getExtent().getCy()).intValue();
				cy = StyleSheet.emuToPixels(cy);
				this.extentInPixels = new Dimension(cx, cy);
			}
			
			if (inline.getEffectExtent() != null) {
				this.effectExtent = new CTEffectExtent(inline.getEffectExtent());
			}
			
			if (inline.getGraphic() != null) {
				this.graphic = new Graphic(inline.getGraphic());
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
	}


}// DrawingML class



















