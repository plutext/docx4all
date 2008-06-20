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

package org.plutext.client;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBIntrospector;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.docx4all.xml.ObjectFactory;
import org.docx4j.XmlUtils;
import org.docx4j.diff.ParagraphDifferencer;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;

/**
 *	@author Jojada Tirtowidjojo - 06/06/2008
 */
public class SdtBlockMerger {
	private static Logger log = Logger.getLogger(SdtBlockMerger.class);
	
	private SdtBlock sdt1, sdt2;
	
	public SdtBlockMerger(SdtBlock sdt1, SdtBlock sdt2) {
		this.sdt1 = sdt1;
		this.sdt2 = sdt2;
	}
	
	public org.docx4j.wml.SdtBlock merge() {
		//'small' list will contain less paragraphs than 'big' list
		List<org.docx4j.wml.P> small = getParagraphs(this.sdt1);
		List<org.docx4j.wml.P> big = getParagraphs(this.sdt2);
		
		//Make sure that 'small' contains less paragraphs
		if (small.size() > big.size()) {
			List<org.docx4j.wml.P> temp = small;
			small = big;
			big = temp;
		}

		List<org.docx4j.wml.P> newPs = 
			new ArrayList<org.docx4j.wml.P>(big.size());
		for (int i=0; i < small.size(); i++) {
			org.docx4j.wml.P p1 = small.get(i);
			org.docx4j.wml.P p2 = big.get(i);
			
			StreamResult result = new StreamResult(new ByteArrayOutputStream());
			ParagraphDifferencer.diff(p1, p2, result);
			
			String s = result.getOutputStream().toString();
			
			log.debug("SdtBlockMerger.merge(): Resulting p[" + i + "]=" + s);
			
			newPs.add((org.docx4j.wml.P) XmlUtils.unmarshalString(s));
		}
		
		//Absorb the remaining paragraphs in 'big'
		for (org.docx4j.wml.P p: big.subList(small.size(), big.size())) {
			org.docx4j.wml.P copy = (org.docx4j.wml.P) XmlUtils.deepCopy(p);
			newPs.add(copy);
		}
		small = null;
		big = null;
		
		//Put 'newPs' in SdtContentBlock
		SdtContentBlock sdtContent = ObjectFactory.createSdtContentBlock();
		sdtContent.getEGContentBlockContent().addAll(newPs);
		
		//Create the resulting SdtBlock
		org.docx4j.wml.SdtBlock sdt = 
			(org.docx4j.wml.SdtBlock) XmlUtils.deepCopy(this.sdt1);
		sdt.setSdtContent(sdtContent);

		return sdt;
	}
	
	private List<org.docx4j.wml.P> getParagraphs(SdtBlock sdt) {
		List<Object> list = sdt.getSdtContent().getEGContentBlockContent();
		List<org.docx4j.wml.P> theResult = new ArrayList<org.docx4j.wml.P>(list.size());
		
		for (Object obj : list) {
			obj = JAXBIntrospector.getValue(obj);
			
			if (obj instanceof org.docx4j.wml.P) {
				theResult.add((org.docx4j.wml.P) obj);
			}
		}
		return theResult;
	}
}// SdtBlockMerger class



















