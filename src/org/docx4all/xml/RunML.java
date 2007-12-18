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
import org.docx4j.document.wordprocessingml.Run;
import org.docx4j.document.wordprocessingml.RunProperties;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunML extends ElementML {
	private static Logger log = Logger.getLogger(RunML.class);
	
	private Run run;
	
	public RunML(Run run) {
		this(run, false);
	}
	
	public RunML(Run run, boolean isDummy) {
		this.run = run;
		this.tag = WordML.Tag.R;
		this.isDummy = isDummy;
		
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
		//According to the specification, 
		//RunPropertiesML has to be the first child.
		ElementML firstChild = getChild(0);
		if (firstChild instanceof PropertiesContainerML) {
			return (PropertiesContainerML) firstChild;
		}
		return null;
	}
	
	private void initChildren() {
		this.children = null;
		if (this.run != null) {
			List rKids = this.run.getRunContents();
			if (!rKids.isEmpty()) {
				this.children = new ArrayList<ElementML>(rKids.size());

				for (Object o : rKids) {
					// TODO: Currently 'rKids' may contain:
					// org.docx4j.document.wordprocessingml.RunProperties
					// and/or org.dom4j.Element objects
					// Watch any future change in Paragraph class

					if (o instanceof RunProperties) {
						RunProperties rPr = (RunProperties) o;
						children.add(new RunPropertiesML(rPr));

					} else {
						Element elem = (Element) o;
						WordML.Tag tag = WordML.getTag(elem.getName());
						if (tag == WordML.Tag.T) {
							// TODO:Check the xml:space attribute
							// in order to know whether we should trim
							// text or not.
							String txt = elem.getText().trim();
							if (txt != null && txt.length() > 0) {
								RunContentML child = new RunContentML(elem, txt, this.isDummy);
								children.add(child);
							}
						} else if (tag == WordML.Tag.BR) {
							// TODO: Full support of BR element
							RunContentML child = 
								new RunContentML(
									elem,
									org.docx4all.ui.main.Constants.NEWLINE,
									this.isDummy);
							children.add(child);

						} else if (tag == WordML.Tag.CR) {
							RunContentML child = 
								new RunContentML(
									elem,
									org.docx4all.ui.main.Constants.NEWLINE,
									this.isDummy);
							children.add(child);

						} else {
							// TODO: Create an unsupported RunContentML ?
						}
					}
				}// for (Object:o)
			}// if (!rKids.isEmpty())
		}
	}// initChildren()
	
}// RunML class






















