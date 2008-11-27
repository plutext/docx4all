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

import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.docx4all.ui.main.Constants;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.HyperlinkML;
import org.docx4all.xml.ImpliedContainerML;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunDelML;
import org.docx4all.xml.RunInsML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 17/01/2008
 */
public class WordMLFragment implements Cloneable {

	private ElementMLRecord[] records;
	private String text;
	
	public WordMLFragment(TextSelector ts) {
		List<ElementMLRecord> list = ts.getElementMLRecords();
		if (!list.isEmpty()) {
			this.records = new ElementMLRecord[list.size()];
			this.records = list.toArray(records);
		}
		this.text = ts.getText();
	}
	
	public WordMLFragment(String text) {
		this.text = text;
		String[] sarray = text.split(Constants.NEWLINE, -1);
		if (sarray.length == 1) {
			this.records = new ElementMLRecord[1];
			RunContentML rcml = 
				new RunContentML(ObjectFactory.createT(sarray[0]));
			this.records[0] = 
				new ElementMLRecord(rcml, true);
		} else {
			List<ElementMLRecord> list = new ArrayList<ElementMLRecord>();
			for (int i=0; i < sarray.length; i++) {
				if (i == sarray.length - 1 
					&& sarray[i].length() == 0) {
					;// ignore
				} else {
					String s = (sarray[i].length() == 0) ? null : sarray[i];
					ParagraphML para = 
						new ParagraphML(ObjectFactory.createP(s));
					boolean isFragmented =
						(s != null) && (i == sarray.length - 1);
					list.add(new ElementMLRecord(para, isFragmented));
				}
			}
			this.records = new ElementMLRecord[list.size()];
			list.toArray(this.records);
		}
	}
	
	public WordMLFragment(ElementMLRecord[] records) {
		this.records = records;
		
		ElementML container = new ImpliedContainerML();
		for (int i=0; i < records.length; i++) {
			ElementML ml = records[i].getElementML();
			container.addChild(ml, false);
		}

		List<ElementSpec> specs = DocUtil.getElementSpecs(container);
    	StringBuffer sb = new StringBuffer();
    	for (ElementSpec es: specs) {
    		if (es.getType() == ElementSpec.ContentType) {
    			sb.append(es.getArray());
    		}
    	}
    	this.text = sb.toString();
	}
	
	public List<ElementMLRecord> getParagraphRecords() {
		List<ElementMLRecord> theRecords = 
			new ArrayList<ElementMLRecord>();
		
		for (int i=records.length-1; i >= 0; i--) {
			ElementML ml = records[i].getElementML();
			if ((ml instanceof RunML) 
				|| (ml instanceof RunContentML)
				|| (ml instanceof RunInsML)
				|| (ml instanceof RunDelML)
				|| (ml instanceof HyperlinkML)) {
				break;
			} else {
				theRecords.add(0, records[i]);
			}
		}
		
		if (theRecords.isEmpty()) {
			theRecords = null;
		}
		
		return theRecords;
	}
	
	public List<ElementMLRecord> getParagraphContentRecords() {
		List<ElementMLRecord> theRecords = 
			new ArrayList<ElementMLRecord>();
		
		for (ElementMLRecord rec: records) {
			ElementML ml = rec.getElementML();
			if ((ml instanceof RunML) 
				|| (ml instanceof RunContentML)
				|| (ml instanceof RunInsML)
				|| (ml instanceof RunDelML)
				|| (ml instanceof HyperlinkML)) {
				theRecords.add(rec);				
			} else {
				break;
			}
		}
		
		if (theRecords.isEmpty()) {
			theRecords = null;
		}
		
		return theRecords;
	}
	
	public String getText() {
		return text;
	}
	
    public Object clone() {
    	WordMLFragment theClone = null;
        try {
        	theClone = (WordMLFragment) super.clone();
        	
        	theClone.records = null;
        	if (records != null) {
        		theClone.records = 
        			new ElementMLRecord[records.length];
				for (int i=0; i < records.length; i++) {
					theClone.records[i] = 
						(ElementMLRecord) records[i].clone();
				}
			}
        	
        	theClone.text = null;
        	if (text != null) {
        		theClone.text = new String(text);
        	}
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }

        return theClone;
    }

	public static class ElementMLRecord implements Cloneable {
		private ElementML elementML;
		private boolean isFragmented;
		
		public ElementMLRecord(ElementML elementML, boolean isFragmented) {
			this.elementML = elementML;
			this.isFragmented = isFragmented;
		}
		
		public ElementML getElementML() {
			return this.elementML;
		}
		
		public boolean isFragmented() {
			return this.isFragmented;
		}
		
	    public Object clone() {
	    	ElementMLRecord theClone = null;
	        try {
	        	theClone = (ElementMLRecord) super.clone();
	        	
	        	theClone.elementML = null;
	        	if (elementML != null) {
	        		theClone.elementML = (ElementML) elementML.clone();
	        	}
	        } catch (CloneNotSupportedException ex) {
	            ex.printStackTrace();
	        }

	        return theClone;
	    }

	}//ElementMLRecord inner class
	
}// WordMLFragment class



















