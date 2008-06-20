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
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.plutext.client.wrappedTransforms.TransformDelete;

/** This class handles the merging of conflicts.
 *  It will need substantial work before it works in docx4all.  
 */
public class Merge {
	private static Logger log = Logger.getLogger(Merge.class);

	/* Merge the change in the transformation t with the contents
	 * of the content control, marking up the difference. */
	protected static long mergeUpdate(SdtBlock cc, String t,
			ServerFrom serverFrom) {
		log.debug("MergeUpdate DIFFERENCING against server update: " + t);

		// Register the transform, and 
		// also use this to get tr 
		HashMap<Long, TransformAbstract> dict = 
			serverFrom.registerTransforms(t, false);
		TransformAbstract tr = null;
		int sanityCounter = 0;

		Iterator transformsIterator = dict.entrySet().iterator();
		while (transformsIterator.hasNext()) {
			Map.Entry pairs = (Map.Entry) transformsIterator.next();
			tr = (TransformAbstract) pairs.getValue();
			sanityCounter++;
			if (sanityCounter > 1) {
				log.debug("Expected a single transformation, but found many!!");
			}
		}

		return Merge.mergeUpdate(cc, tr, serverFrom);

	}

	/* Merge the change in the transformation t with the contents
	 * of the content control, marking up the difference. */
	protected static long mergeUpdate(SdtBlock cc, TransformAbstract tr, ServerFrom serverFrom) {
		// The Word 2007 addin uses Word's compare functionality,
		// on shared versus local changes.
		// But docx4all can't do that.  Options:
		// - 3DM three way XML merge, but this exits with conflicting
		//   changes in the common situation where there are edits
		//   to a run of text
		// - Topologi's diffx, which seems to handle the run
		//   situation nicely (with <ins> and <del> elements), but
		//   I'd still need to work out what to put in the document,
		//   and probably, represent it with redline and strikeout
		// Note that the sdt I tested diffx on was simplified - it
		// didn't contain any rsid elements.

		if (tr instanceof TransformDelete) {
			return tr.getSequenceNumber();
		}

		SdtBlockMerger merger = new SdtBlockMerger(cc, tr.getSdt());
		SdtBlock newSdt = merger.merge();
		tr.getSdt().setSdtContent(newSdt.getSdtContent());
		
		long result = tr.apply(serverFrom);
		tr.setApplied(true);
		
		return result;
	}

} //Merge class





























