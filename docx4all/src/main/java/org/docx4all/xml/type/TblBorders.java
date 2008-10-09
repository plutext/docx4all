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

package org.docx4all.xml.type;

import org.docx4all.swing.BottomLineBorderSegment;
import org.docx4all.swing.InsideHLineBorderSegment;
import org.docx4all.swing.InsideVLineBorderSegment;
import org.docx4all.swing.LeftLineBorderSegment;
import org.docx4all.swing.LineBorderSegment;
import org.docx4all.swing.RightLineBorderSegment;
import org.docx4all.swing.TopLineBorderSegment;

/**
 *	@author Jojada Tirtowidjojo - 30/09/2008
 */
public class TblBorders {
	org.docx4j.wml.TblBorders tblBorders;
	
	public TblBorders(org.docx4j.wml.TblBorders tblBorders) {
		this.tblBorders = tblBorders;
	}
	
	public LineBorderSegment getLeft() {
		if (tblBorders.getLeft() != null) {
			CTBorder left = new CTBorder(tblBorders.getLeft());
			return new LeftLineBorderSegment(left);
		}
		return null;
	}
	
	public LineBorderSegment getTop() {
		if (tblBorders.getTop() != null) {
			CTBorder top = new CTBorder(tblBorders.getTop());
			return new TopLineBorderSegment(top);
		}
		return null;
	}
	
	public LineBorderSegment getRight() {
		if (tblBorders.getRight() != null) {
			CTBorder right = new CTBorder(tblBorders.getRight());
			return new RightLineBorderSegment(right);
		}
		return null;
	}

	public LineBorderSegment getBottom() {
		if (tblBorders.getBottom() != null) {
			CTBorder bottom = new CTBorder(tblBorders.getBottom());
			return new BottomLineBorderSegment(bottom);
		}
		return null;
	}

	public LineBorderSegment getInsideV() {
		if (tblBorders.getInsideV() != null) {
			CTBorder insideV = new CTBorder(tblBorders.getInsideV());
			return new InsideVLineBorderSegment(insideV);
		}
		return null;
	}

	public LineBorderSegment getInsideH() {
		if (tblBorders.getInsideH() != null) {
			CTBorder insideH = new CTBorder(tblBorders.getInsideH());
			return new InsideHLineBorderSegment(insideH);
		}
		return null;
	}

}// TblBorders class



















