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

package org.docx4all.swing.event;

import javax.swing.event.DocumentListener;

/**
 *	@author Jojada Tirtowidjojo - 15/05/2008
 */
public interface WordMLDocumentListener extends DocumentListener {
    /**
     * Gives notification that there was a snapshot change in the document.
     * The range given by the WordMLDocumentEvent bounds the freshly changed region.
     *
     * @param e the document event
     */
    public void snapshotChangedUpdate(WordMLDocumentEvent e);

}// WordMLDocumentListener interface



















