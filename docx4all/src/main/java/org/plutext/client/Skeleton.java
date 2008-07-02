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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffEngineLevel;
import org.plutext.client.diffengine.DiffResultSpan;
import org.plutext.client.diffengine.IDiffList;
import org.plutext.server.transitions.Transitions;

public class Skeleton implements IDiffList<TextLine> {
	private static Logger log = Logger.getLogger(Skeleton.class);

	private static void TextDiff(Skeleton source, Skeleton dest) {

		try {
			double time = 0;
			DiffEngine de = new DiffEngine();
			time = de.processDiff(source, dest, DiffEngineLevel.MEDIUM);

			ArrayList<DiffResultSpan> rep = de.getDiffLines();

			// log.Debug(de.Results(source, dest, rep));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// Version number
	private int version;

	// Ordered list of ribs
	private ArrayList<TextLine> ribs = new ArrayList<TextLine>();

	public Skeleton() {
		;// do nothing
	}

	public Skeleton(Transitions t) {
		/*
		 * <dst:transitions> <dst:ribs> <dst:rib id="54989358" /> <dst:rib
		 * id="1447653797" /> : </dst:transitions>
		 * 
		 */

		for (Transitions.Ribs.Rib r : t.getRibs().getRib()) {
			ribs.add(new TextLine(Long.toString(r.getId())));
		}
	}

	public ArrayList<TextLine> getRibs() {
		return ribs;
	}

	public void setRibs(ArrayList<TextLine> ribs) {
		this.ribs = ribs;
	}

	public int count() {
		return ribs.size();
	}

	public Comparable<TextLine> getByIndex(int index) {
		return ribs.get(index);
	}

}// Skeleton class
























