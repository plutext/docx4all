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

package org.plutext.client.wrappedTransforms;

import java.util.Comparator;

/**
 *	@author Jojada Tirtowidjojo - 23/07/2008
 */
public class TransformComparator implements Comparator<TransformAbstract> {
    public int compare(TransformAbstract t1, TransformAbstract t2) {
    	Long sn1 = Long.valueOf(t1.getSequenceNumber());
    	Long sn2 = Long.valueOf(t2.getSequenceNumber());
    	return sn1.compareTo(sn2);
    }
}// TransformComparator class



















