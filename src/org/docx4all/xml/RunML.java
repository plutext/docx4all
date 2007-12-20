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

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.docx4j.jaxb.document.RPr;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunML extends ElementML {
	private static Logger log = Logger.getLogger(RunML.class);
	
	private org.docx4j.jaxb.document.R run;
	private PropertiesContainerML rPr;
	
	public RunML(org.docx4j.jaxb.document.R run) {
		this(run, false);
	}
	
	public RunML(org.docx4j.jaxb.document.R run, boolean isDummy) {
		this.run = run;
		this.tag = WordML.Tag.R;
		this.isDummy = isDummy;
		
		initRunProperties();
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
		return this.run == null;
	}

	/**
	 * Gets the run property element of this run element.
	 * 
	 * @return a RunPropertiesML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getRunProperties() {
		return this.rPr;
	}
	
	private void initRunProperties() {
		this.rPr = null;
		if (this.run != null) {
			//if not an implied RunML
			RPr rPr = this.run.getRPr();
			if (rPr != null) {
				this.rPr = new RunPropertiesML(rPr);
			}
		}
	}
	
	private void initChildren() {
		this.children = null;
		if (this.run == null) {
			//if an implied RunML
			return;
		}

		List<Object> rKids = this.run.getRunContent();
		if (!rKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(rKids.size());

			for (Object o : rKids) {
				if (o instanceof org.docx4j.jaxb.document.Br) {
					// TODO: Full support of BR element
					RunContentML child = 
						new BrML(
							(org.docx4j.jaxb.document.Br) o,
							this.isDummy);
					child.setParent(RunML.this);
					this.children.add(child);
					
				} else if (o instanceof org.docx4j.jaxb.document.Cr) {
					// TODO: Full support of BR element
					RunContentML child = 
						new CrML(
							(org.docx4j.jaxb.document.Cr) o,
							this.isDummy);
					child.setParent(RunML.this);
					this.children.add(child);
					
				} else if (o instanceof JAXBElement<?>) {
					JAXBElement<?> jaxbElem = (JAXBElement<?>) o;
					String typeName = jaxbElem.getDeclaredType().getName();
					if ("org.docx4j.jaxb.document.Text".equals(typeName)) {
						org.docx4j.jaxb.document.Text t = 
							(org.docx4j.jaxb.document.Text) 
								jaxbElem.getValue();
						String s = t.getValue();
						if (s != null && s.length() > 0) {
							RunContentML child = new TextML(t, this.isDummy);
							child.setParent(RunML.this);
							this.children.add(child);
						}
					} else {
						// TODO: Create an unsupported RunContentML ?
					}
				}// if (o instanceof JAXBElement<?>)
			}// for (Object o : rKids)
		}// if (!rKids.isEmpty())
	}// initChildren()
	
}// RunML class






















