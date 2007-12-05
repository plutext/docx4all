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
import org.docx4j.document.wordprocessingml.Paragraph;
import org.docx4j.document.wordprocessingml.ParagraphProperties;
import org.docx4j.document.wordprocessingml.Run;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphML extends ElementML {
	private static Logger log = Logger.getLogger(ParagraphML.class);

	private Paragraph paragraph;
	private boolean isDummy;
	
	public ParagraphML(Paragraph p) {
		this(p, false);
	}
	
	public ParagraphML(Paragraph p, boolean isDummy) {
		this.paragraph = p;
		this.tag = WordML.Tag.P;
		this.isDummy = isDummy;
		
		initChildren();
	}
	
	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public ParagraphPropertiesML getParagraphProperties() {
		//According to the specification, 
		//ParagraphPropertiesML has to be the first child.
		ElementML firstChild = getChild(0);
		if (firstChild instanceof ParagraphPropertiesML) {
			return (ParagraphPropertiesML) firstChild;
		}
		return null;
	}
	
	private void initChildren() {
		List pKids = this.paragraph.getParagraphContents();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());

			for (Object o : pKids) {
				// TODO: Currently 'paraKids' may contain:
				// org.docx4j.document.wordprocessingml.ParagraphProperties
				// org.docx4j.document.wordprocessingml.Run
				// and/or org.dom4j.Element objects
				// Watch any future change in Paragraph class
				ElementML elem;
				if (o instanceof Run) {
					Run run = (Run) o;
					elem = new RunML(run);
				} else if (o instanceof ParagraphProperties) {
					ParagraphProperties pPr = (ParagraphProperties) o;
					elem = new ParagraphPropertiesML(pPr);
				} else {
					elem = createDummyRun((Element) o);
				}
				
				elem.setParent(ParagraphML.this);
				children.add(elem);
			}// for (Object:o)
		}// if (!pKids.isEmpty())
	}// initChildren()
	
	private RunML createDummyRun(Element unsupportedElem) {
		//Create an implied Run that contains a piece of information
		//about this unsupported element
		//TODO: A more informative information in the implied Run.
		StringBuffer text = new StringBuffer();
		text.append("UNSUPPORTED '");
		text.append(unsupportedElem.getName());
		text.append("' tag element inside paragraph");
		return ElementMLFactory.createRunML(text.toString(), true);
	}
	
}// ParagraphML class





















