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

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public class BadSelectionException extends Exception {
    private int offset, length;
    
    /**
     * Creates a new BadSelectionException object.
     * 
     * @param s		a string indicating what was wrong with the arguments
     * @param offs  offset within the document that the requested selection starts
     * @param length  length of the requested selection
     */
    public BadSelectionException(String s, int offset, int length) {
		super(s);
		this.offset = offset;
		this.length = length;
	}

    /**
     * Returns the offset of the illegal selection
     *
     * @return the offset >= 0
     */
    public int getOffsetRequested() {
    	return offset;
    }

    /**
     * Returns the length of the illegal selection
     *
     * @return the offset >= 0
     */
    public int getLengthRequested() {
    	return length;
    }
    
}// BadSelectionException class



















