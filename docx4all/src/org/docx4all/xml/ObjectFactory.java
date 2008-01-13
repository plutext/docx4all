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

import javax.swing.text.StyleConstants;
import javax.xml.bind.JAXBElement;

import org.docx4all.ui.main.Constants;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.P;
import org.docx4j.wml.STJc;
import org.docx4j.wml.Text;


/**
 *	@author Jojada Tirtowidjojo - 02/01/2008
 */
public class ObjectFactory {
	private final static org.docx4j.wml.ObjectFactory _jaxbFactory = 
		new org.docx4j.wml.ObjectFactory();

	public final static JAXBElement<P> createPara(String textContent) {
		org.docx4j.wml.P p = createP(textContent);
		return _jaxbFactory.createP(p);
	}
	
	public final static org.docx4j.wml.P createP(String textContent) {
		org.docx4j.wml.P p = _jaxbFactory.createP();
		if (textContent != null) {
			org.docx4j.wml.R r = createR(textContent);
			p.getParagraphContent().add(r);
			r.setParent(p);
		}
		return p;
	}
	
	public final static org.docx4j.wml.R createR(String textContent) {
		org.docx4j.wml.R r = _jaxbFactory.createR();
		
		if (org.docx4all.ui.main.Constants.NEWLINE.equals(textContent)) {
			org.docx4j.wml.Cr cr = _jaxbFactory.createCr();
			r.getRunContent().add(cr);
			cr.setParent(r);
		} else {
			org.docx4j.wml.Text text = _jaxbFactory.createText();
			text.setValue(textContent);
			r.getRunContent().add(_jaxbFactory.createT(text));
			text.setParent(r);
		}
		return r;
	}
	
	public final static JAXBElement<Text> createT(String textContent) {
		org.docx4j.wml.Text text = _jaxbFactory.createText();
		text.setValue(textContent);
		return _jaxbFactory.createT(text);
	}
	
	public static WordprocessingMLPackage createDocumentPackage(org.docx4j.wml.Document doc) {
		// Create a package
		WordprocessingMLPackage wmlPack = new WordprocessingMLPackage();

		try {
		// Create main document part
		MainDocumentPart wordDocumentPart = new MainDocumentPart();		
		
		// Put the content in the part
		wordDocumentPart.setJaxbElement(doc);
						
		// Add the main document part to the package relationships
		// (creating it if necessary)
		wmlPack.addTargetPart(wordDocumentPart);
				
		// Create a styles part
		StyleDefinitionsPart stylesPart = new StyleDefinitionsPart();
			stylesPart.unmarshalDefaultStyles();
			
			// Add the styles part to the main document part relationships
			// (creating it if necessary)
			wordDocumentPart.addTargetPart(stylesPart); // NB - add it to main doc part, not package!			
			
		} catch (Exception e) {
			// TODO: Synch with WordprocessingMLPackage.createTestPackage()
			e.printStackTrace();	
			wmlPack = null;
		}
		
		// Return the new package
		return wmlPack;
		
	}
	
	public final static WordprocessingMLPackage createEmptyDocumentPackage() {
		org.docx4j.wml.P  para = createP(Constants.NEWLINE);
		
		org.docx4j.wml.Body  body = _jaxbFactory.createBody();
		body.getBlockLevelElements().add(_jaxbFactory.createP(para));
		para.setParent(body);
		
		org.docx4j.wml.Document doc = _jaxbFactory.createDocument();
		doc.setBody(body);
		body.setParent(doc);

		return createDocumentPackage(doc);
	}
	
	public final static org.docx4j.wml.Jc createJc(Integer align) {
		org.docx4j.wml.Jc theJc = null;
		
        if (align != null) {
        	theJc = _jaxbFactory.createJc();
			if (align.intValue() == StyleConstants.ALIGN_LEFT) {
				theJc.setVal(STJc.LEFT);
			} else if (align.intValue() == StyleConstants.ALIGN_RIGHT) {
				theJc.setVal(STJc.RIGHT);
			} else if (align.intValue() == StyleConstants.ALIGN_CENTER) {
				theJc.setVal(STJc.CENTER);
			} else if (align.intValue() == StyleConstants.ALIGN_JUSTIFIED) {
				theJc.setVal(STJc.BOTH);
			} else {
				theJc = null;
			}
		}

        return theJc;
	}
	
	public final static org.docx4j.wml.BooleanDefaultTrue createBooleanDefaultTrue(Boolean b) {
		BooleanDefaultTrue bdt = _jaxbFactory.createBooleanDefaultTrue();
		bdt.setVal(b);
		return bdt;
	}
	
	public final static org.docx4j.wml.Underline createUnderline(String value, String color) {
		org.docx4j.wml.Underline u = _jaxbFactory.createUnderline();
		u.getVal().add(value);
		u.setColor(color);
		return u;
	}
	
	private ObjectFactory() {
		;//uninstantiable
	}
}// ObjectFactory class



















