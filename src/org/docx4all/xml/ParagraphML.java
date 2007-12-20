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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.document.PPr;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphML extends ElementML {
	private static Logger log = Logger.getLogger(ParagraphML.class);

	private org.docx4j.jaxb.document.P paragraph;
	private PropertiesContainerML pPr;
	
	public ParagraphML(org.docx4j.jaxb.document.P p) {
		this(p, false);
	}
	
	public ParagraphML(org.docx4j.jaxb.document.P p, boolean isDummy) {
		this.paragraph = p;
		this.tag = WordML.Tag.P;
		this.isDummy = isDummy;
		
		initParagraphProperties();
		initChildren();
	}
	
	/**
	 * An implied ElementML is an ElementML that
	 * does not have a DOM element associated with it.
	 * This kind of ElementML may still have a WordML.Tag.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public boolean isImplied() {
		return this.paragraph == null;
	}

	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getParagraphProperties() {
		return this.pPr;
	}
	
	private void initParagraphProperties() {
		this.pPr = null;
		if (this.paragraph != null) {
			//if not an implied ParagraphML
			PPr pProp = this.paragraph.getPPr();
			if (pProp != null) {
				this.pPr = new ParagraphPropertiesML(pProp);
			}
		}
	}
	
	private void initChildren() {
		this.children = null;
		if (this.paragraph == null) {
			//if an implied ParagraphML
			return;
		}
		
		List<Object> pKids = this.paragraph.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			for (Object o : pKids) {
				ElementML elem;
				if (o instanceof org.docx4j.jaxb.document.R) {
					org.docx4j.jaxb.document.R run = (org.docx4j.jaxb.document.R) o;
					elem = new RunML(run, this.isDummy);

					// } else if (o instanceof
					// org.docx4j.jaxb.document.RunTrackChange) {
					// TODO: To support RunTrackChange
				} else {
					// TODO: A more informative information in the implied Run.
					String textContent = XmlUtils.marshaltoString(o, true);
					elem = ElementMLFactory.createRunML(textContent, true);
				}

				elem.setParent(ParagraphML.this);
				this.children.add(elem);
			}
		}// if (!pKids.isEmpty())
	}// initChildren()
}// ParagraphML class





















