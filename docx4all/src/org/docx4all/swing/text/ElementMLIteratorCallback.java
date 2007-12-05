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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.docx4all.ui.main.Constants;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.PropertiesContainerML;
import org.docx4all.xml.PropertyML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.RunPropertiesML;
import org.docx4all.xml.RunContentML;

public class ElementMLIteratorCallback extends ElementMLIterator.Callback {
	List<ElementSpec> elementSpecs = new ArrayList<ElementSpec>();
	MutableAttributeSet paragraphAttrs, runAttrs;
	
	public void handleStartElement(ElementML elem) {
		if (elem instanceof PropertiesContainerML || elem instanceof PropertyML) {
			//ignore this element
			return;
		}
		
		SimpleAttributeSet elemAttrs = new SimpleAttributeSet();
		WordMLStyleConstants.setElementML(elemAttrs, elem);
		
		if (elem instanceof ParagraphML) {
			ParagraphML paraML = (ParagraphML) elem;
			ParagraphPropertiesML pPr = paraML.getParagraphProperties();
			if (pPr != null) {
				elemAttrs.addAttributes(pPr.getAttributeSet());
			}
			
			paragraphAttrs = elemAttrs;
			openElementSpec(paragraphAttrs);
			
		} else if (elem instanceof RunML) {
			RunML runML = (RunML) elem;
			RunPropertiesML rPr = runML.getRunProperties();
			if (rPr != null) {
				elemAttrs.addAttributes(rPr.getAttributeSet());
			}
			
			runAttrs = elemAttrs;
			openElementSpec(runAttrs);
			
		} else if (elem instanceof RunContentML) {
			addContentElementSpec(elemAttrs, ((RunContentML) elem).getText());
			
		} else {
			openElementSpec(elemAttrs);
		}
	}
	
	public void handleEndElement(ElementML elem) {
		if (elem instanceof RunContentML 
			|| elem instanceof PropertiesContainerML
			|| elem instanceof PropertyML) {
			return;
		}
		
		if (elem instanceof ParagraphML) {
			//TODO: Check first whether this paragraph already
			//ends with a newline character.
			RunML runML = ElementMLFactory.IMPLIED_NEWLINE_RUNML;
			SimpleAttributeSet runAttrs = new SimpleAttributeSet();
			WordMLStyleConstants.setElementML(runAttrs, runML);
			openElementSpec(runAttrs);
			
			RunContentML rcML = (RunContentML) runML.getChild(0);
			SimpleAttributeSet textAttrs = new SimpleAttributeSet();
			WordMLStyleConstants.setElementML(textAttrs, rcML);
			addContentElementSpec(textAttrs, rcML.getText());
			
			closeElementSpec(runAttrs.copyAttributes());
		}
		
		SimpleAttributeSet elemAttrs = new SimpleAttributeSet();
		WordMLStyleConstants.setElementML(elemAttrs, elem);
		closeElementSpec(elemAttrs);
	}
	
	public List<ElementSpec> getElementSpecs() {
		return elementSpecs;
	}
	
	private void openElementSpec(AttributeSet attrs) {
		ElementSpec es = new ElementSpec(attrs, ElementSpec.StartTagType);
		elementSpecs.add(es);
	}
	
	private void addContentElementSpec(AttributeSet attrs, String text) {
		ElementSpec es = 
			new ElementSpec(
				attrs, 
				ElementSpec.ContentType, 
				text.toCharArray(), 
				0, 
				text.length());
		elementSpecs.add(es);
	}
	
	private void closeElementSpec(AttributeSet attrs) {
		ElementSpec es = new ElementSpec(attrs, ElementSpec.EndTagType);
		elementSpecs.add(es);
	}
}// ElementMLIteratorCallback class























