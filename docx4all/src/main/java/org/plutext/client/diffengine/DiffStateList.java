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

package org.plutext.client.diffengine;

import java.util.Hashtable;

/**
 * @author Jojada Tirtowidjojo - 02/07/2008
 */
public class DiffStateList {
	private Hashtable<Integer, DiffState> _table;

	public DiffStateList(int destCount) {
		_table = new Hashtable<Integer, DiffState>(destCount);
	}

	public DiffState getByIndex(int index) {
		Integer key = Integer.valueOf(index);
		DiffState retval = _table.get(key);
		if (retval == null) {
			retval = new DiffState();
			_table.put(key, retval);
		}
		return retval;
	}

}// DiffStateList class

