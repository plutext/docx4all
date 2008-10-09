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

package org.docx4all.swing;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.plaf.basic.BasicGraphicsUtils;

import org.docx4all.xml.type.CTBorder;

/**
 *	@author Jojada Tirtowidjojo - 30/09/2008
 */
public class InsideHLineBorderSegment extends LineBorderSegment {
	public InsideHLineBorderSegment(CTBorder border) {
		this(border.getColor(), border.getStyle(), border.getSizeInPixels());
	}
	
	public InsideHLineBorderSegment(Color c, Style style, int thickness) {
		super(c, Side.INSIDE_H, style, thickness);
	}
	
	public void drawLine(Graphics g, int x1, int y1, Direction dir, int length) {
		if (dir == Direction.UP || dir == Direction.DOWN) {
			throw new IllegalArgumentException("Direction=" + dir.getDeclaringClass().getName());
		}
		
		if (thickness == 0) {
			return;
		}
		
		if (dir == Direction.LEFT) {
			x1 -= length;
		}
		
		//The line drawn at (x1, y1) is the center line.
		//Therefore, (thickness - 1) is shared between top side and bottom side.
		//If thickness is an odd number then topThickness == bottomThickness.
		//If thickness is an even number then topThickness == bottomThickness + 1;
		int topThickness = Math.round((thickness - 1) / 2);
		int bottomThickness = (thickness - 1) - topThickness;
		
		y1 -= topThickness;
		for(int i = 0; i < topThickness + 1; i++)  {
        	if (style == Style.SOLID) {
        		g.drawRect(x1, y1 + i, 0, length);
        	} else {
        		//Style.DASHED
        		BasicGraphicsUtils.drawDashedRect(g, x1, y1 + i, 0, length);
        	}
        }
		
		x1 += (topThickness + 1);
		for(int i = 0; i < bottomThickness; i++)  {
        	if (style == Style.SOLID) {
        		g.drawRect(x1, y1 + i, 0, length);
        	} else {
        		//Style.DASHED
        		BasicGraphicsUtils.drawDashedRect(g, x1, y1 + i, 0, length);
        	}
        }
	}
}// InsideHLineBorderSegment class



















