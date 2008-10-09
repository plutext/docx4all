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
public class InsideVLineBorderSegment extends LineBorderSegment {
	public InsideVLineBorderSegment(CTBorder border) {
		this(border.getColor(), border.getStyle(), border.getSizeInPixels());
	}
	
	public InsideVLineBorderSegment(Color c, Style style, int thickness) {
		super(c, Side.INSIDE_V, style, thickness);
	}
	
	public void drawLine(Graphics g, int x1, int y1, Direction dir, int length) {
		if (dir == Direction.LEFT || dir == Direction.RIGHT) {
			throw new IllegalArgumentException("Direction=" + dir.getDeclaringClass().getName());
		}
		
		if (thickness == 0) {
			return;
		}
		
		if (dir == Direction.UP) {
			y1 -= length;
		}
		
		//The line drawn at (x1, y1) is the center line.
		//Therefore, (thickness - 1) is shared between left side and right side.
		//If thickness is an odd number then leftThickness == rightThickness.
		//If thickness is an even number then leftThickness == rightThickness + 1;
		int leftThickness = Math.round((thickness - 1) / 2);
		int rightThickness = (thickness - 1) - leftThickness;
		
		x1 -= leftThickness;
		for(int i = 0; i < leftThickness + 1; i++)  {
        	if (style == Style.SOLID) {
        		g.drawRect(x1+i, y1, 0, length);
        	} else {
        		//Style.DASHED
        		BasicGraphicsUtils.drawDashedRect(g, x1+i, y1, 0, length);
        	}
        }
		
		x1 += (leftThickness + 1);
		for(int i = 0; i < rightThickness; i++)  {
        	if (style == Style.SOLID) {
        		g.drawRect(x1+i, y1, 0, length);
        	} else {
        		//Style.DASHED
        		BasicGraphicsUtils.drawDashedRect(g, x1+i, y1, 0, length);
        	}
        }
	}

}// InsideVLineSegment class



















