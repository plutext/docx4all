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

package org.docx4all.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.docx4all.swing.text.WordMLFragment;

/**
 *	@author Jojada Tirtowidjojo - 16/01/2008
 */
public class WordMLTransferable implements Transferable {
	public static final DataFlavor WORDML_FRAGMENT_FLAVOR =
		new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType
				+ "; class=org.docx4all.swing.text.WordMLFragment"
			, null);

	public static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;

	public final static boolean isSupported(DataFlavor flavor) {
		return (flavor == WORDML_FRAGMENT_FLAVOR || flavor == STRING_FLAVOR);
	}
	
	private WordMLFragment fragment;

	/**
	 * Creates a Transferable capable of transferring the specified 'fragment'.
	 */
	public WordMLTransferable(WordMLFragment fragment) {
		this.fragment = fragment;
	}

    /**
     * Returns an array of DataFlavor objects indicating the flavors the data 
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] dataFlavors = { WORDML_FRAGMENT_FLAVOR, STRING_FLAVOR };
		return dataFlavors;
    }

    /**
     * Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
		return isSupported(flavor);
    }

    /**
     * Returns an object which represents the data to be transferred.  The class 
     * of the object returned is defined by the representation class of the flavor.
     *
     * @param flavor the requested flavor for the data
     * @see DataFlavor#getRepresentationClass
     * @exception IOException                if the data is no longer available
     *              in the requested flavor.
     * @exception UnsupportedFlavorException if the requested data flavor is
     *              not supported.
     */
    public Object getTransferData(DataFlavor flavor) 
    	throws UnsupportedFlavorException, IOException {

		Object theObject = null;

		if (flavor == WORDML_FRAGMENT_FLAVOR) {
			theObject = fragment.clone();
		} else if (flavor == STRING_FLAVOR) {
			theObject = new String(fragment.getText());
		} else {
			throw new UnsupportedFlavorException(flavor);
		}

		return theObject;
    }


}// WordMLTransferable class



















