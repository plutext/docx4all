/*
 *  Copyright 2008, Plutext Pty Ltd.
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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.plutext.client.state.StateChunk;

/*
 * In docx4all, we may be able to keep this object uptodate, without needing to
 * re-create it again and again in Mediator.
 * 
 * 2009 05 06 - this is Not used!  Remove it if not required...  
 * 
 */
public class Pkg implements Cloneable {

	private static Logger log = Logger.getLogger(Pkg.class);

	//private Map<String, StateChunk> stateChunks = null;
	private Skeleton skeleton = new Skeleton();

	private WordMLDocument doc;
	
	public Pkg(WordMLDocument doc) { 
		this.doc = doc;
	}
	
	public HashMap<String, StateChunk> extractStateChunks() {
		
		HashMap<String, StateChunk> stateChunks = new HashMap<String, StateChunk>();

		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
		for (int idx = 0; idx < root.getElementCount(); idx++) {
			DocumentElement elem = (DocumentElement) root.getElement(idx);
			ElementML ml = elem.getElementML();
			if (ml instanceof SdtBlockML) {
				org.docx4j.wml.SdtBlock sdt = 
					(org.docx4j.wml.SdtBlock) ml.getDocxObject();
				sdt = (org.docx4j.wml.SdtBlock) XmlUtils.deepCopy(sdt);
				StateChunk sc = new StateChunk(sdt);
				stateChunks.put(sc.getIdAsString(), sc);

				skeleton.getRibs().add(new TextLine(sc.getIdAsString()));
			}
		}
		
		return stateChunks;
	}

	public Skeleton getInferedSkeleton() {
		return skeleton;
	}

}// Pkg class



























