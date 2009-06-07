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


/**
 * @author Jojada Tirtowidjojo - 03/07/2008
 */
public class TextLine implements Comparable<TextLine> {
	private String _line;
	private int _hash;

	public TextLine(String str) {
		_line = str.replace("\t", "    ");
		_hash = str.hashCode();
	}
	
	public String getLine() {
		return _line;
	}

	public int compareTo(TextLine obj) {
		Integer thisHash = Integer.valueOf(_hash);
		Integer objHash = Integer.valueOf(obj._hash);
		return thisHash.compareTo(objHash);
	}
	
	public boolean equals(Object other){
		  if ( this == other ) {
			  return true;
		  }
		  if ( !(other instanceof TextLine) ) {
			  return false;
		  }
		  if (compareTo( (TextLine)other)==0) {
			  return true;
		  } else {
			  return false;
		  }
	}

}// TextLine class

