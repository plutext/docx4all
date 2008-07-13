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
import org.plutext.client.state.StateDocx;
import org.docx4j.wml.SdtBlock;
import java.util.ArrayList;



public class Chunker
{

	private static Logger log = Logger.getLogger(Chunker.class);

	public static boolean containsMultipleBlocks(org.docx4j.wml.SdtContentBlock sdtContent) {
		log.info(sdtContent.getEGContentBlockContent().size());
		
		// TODO - In addition to P, getEGContentBlockContent() can contain things
		// like Tbl, RunTrackChange etc.  Consider what to do with those.
		
		if (sdtContent.getEGContentBlockContent().size() == 0 ) {
			log.error("Didn't expect 0 blocks!");
			return false;
		} else if (sdtContent.getEGContentBlockContent().size() == 1 ) {
			return false;
		} else {
			log.info("SDT has " + sdtContent.getEGContentBlockContent().size() + " block children.");
			return true;
		}

	}    
    

    /* Split a control containing n paragraphs
     * into n controls.  
     * 
     * The ID of the first control remains the same.
     * 
     * Returns a list of the IDs of the new controls. */
    public static ArrayList<String> chunk(SdtBlock cc)
    {
    	
    	// Jo to implement
    	
    	return null;

    }


}
