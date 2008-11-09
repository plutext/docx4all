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

import java.awt.Color;

import org.docx4all.swing.BottomLineBorderSegment;
import org.docx4all.swing.InsideHLineBorderSegment;
import org.docx4all.swing.InsideVLineBorderSegment;
import org.docx4all.swing.LeftLineBorderSegment;
import org.docx4all.swing.LineBorderSegment;
import org.docx4all.swing.RightLineBorderSegment;
import org.docx4all.swing.TopLineBorderSegment;
import org.docx4all.swing.LineBorderSegment.Side;

/**
 *	@author Jojada Tirtowidjojo - 30/09/2008
 */
public class TblBorders {
	org.docx4j.wml.TblBorders tblBorders;
	Color autoColor;
	
	public TblBorders(org.docx4j.wml.TblBorders tblBorders) {
		this.tblBorders = tblBorders;
	}
	
	public Object getDocxObject() {
		return this.tblBorders;
	}
	
	public void setAutoColor(Color c) {
		this.autoColor = c;
	}
	
	public Color getAutoColor() {
		return this.autoColor;
	}
	
	public LineBorderSegment getLineBorderSegment(Side side) {
		LineBorderSegment theLine = null;
		
		CTBorder ctb = getCTBorder(side);
		if (ctb == null) {
			;
		} else if (side == Side.LEFT) {
			theLine = new LeftLineBorderSegment(ctb);
		} else if (side == Side.RIGHT) {
			theLine = new RightLineBorderSegment(ctb);
		} else if (side == Side.TOP) {
			theLine = new TopLineBorderSegment(ctb);
		} else if (side == Side.BOTTOM) {
			theLine = new BottomLineBorderSegment(ctb);
		} else if (side == Side.INSIDE_H) {
			theLine = new InsideHLineBorderSegment(ctb);
		} else if (side == Side.INSIDE_V) {
			theLine = new InsideVLineBorderSegment(ctb);
		}
		
		return theLine;
	}
	
	public CTBorder getCTBorder(Side side) {
		CTBorder theBorder = null;
		
		if (side == Side.LEFT) {
			theBorder = getLeftCTBorder();
		} else if (side == Side.RIGHT) {
			theBorder = getRightCTBorder();
		} else if (side == Side.TOP) {
			theBorder = getTopCTBorder();
		} else if (side == Side.BOTTOM) {
			theBorder = getBottomCTBorder();
		} else if (side == Side.INSIDE_H) {
			theBorder = getInsideH_CTBorder();
		} else if (side == Side.INSIDE_V) {
			theBorder = getInsideV_CTBorder();
		}
		
		return theBorder;
	}
	
	private CTBorder getLeftCTBorder() {
		if (tblBorders.getLeft() != null) {
			CTBorder border = new CTBorder(tblBorders.getLeft());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
	private CTBorder getTopCTBorder() {
		if (tblBorders.getTop() != null) {
			CTBorder border = new CTBorder(tblBorders.getTop());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
	private CTBorder getRightCTBorder() {
		if (tblBorders.getRight() != null) {
			CTBorder border = new CTBorder(tblBorders.getRight());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
	private CTBorder getBottomCTBorder() {
		if (tblBorders.getBottom() != null) {
			CTBorder border = new CTBorder(tblBorders.getBottom());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
	private CTBorder getInsideV_CTBorder() {
		if (tblBorders.getInsideV() != null) {
			CTBorder border = new CTBorder(tblBorders.getInsideV());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
	private CTBorder getInsideH_CTBorder() {
		if (tblBorders.getInsideV() != null) {
			CTBorder border = new CTBorder(tblBorders.getInsideH());
			border.setAutoColor(this.autoColor);
			return border;
		}
		return null;
	}
	
}// TblBorders class



















