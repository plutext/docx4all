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

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
//import org.docx4all.xml.DocumentML;
//import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
//import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.state.StateChunk;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/* In docx4all, we may be able to keep this object
 * uptodate, without needing to re-create it
 * again and again in Mediator.
 * 
 */
public class Pkg implements Cloneable
{

	private static Logger log = Logger.getLogger(Pkg.class);
	
	List<SdtBlock> orderedChunks = null;
	
	Skeleton skeleton = new Skeleton();

    public Pkg(WordMLDocument doc)  
    {
  		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();    	
    	
    	orderedChunks = doc.getSnapshotsList(0, doc.getLength());
		if (orderedChunks != null) {
			this.stateChunks = 
				new HashMap<String, StateChunk>(orderedChunks.size() );
			
			for (SdtBlock sdt : orderedChunks ) {
				StateChunk sc = new StateChunk(sdt);
				this.stateChunks.put(sc.getIdAsString(), sc);
				
				skeleton.getRibs().add( skeleton.new TextLine( sc.getIdAsString())); 				
				
			}
		} else {
			this.stateChunks = new HashMap<String, StateChunk>();
		}
		
		
    }



    public Skeleton getInferedSkeleton()
    {
    	
    	return skeleton;
    	
    }

    HashMap<String, StateChunk> stateChunks = null;
    public HashMap<String, StateChunk> getStateChunks()
    {
        return stateChunks;
    }


}
