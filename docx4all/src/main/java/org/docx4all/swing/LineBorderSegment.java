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

/**
 *	@author Jojada Tirtowidjojo - 23/09/2008
 */
public abstract class LineBorderSegment {
	protected Color color;
	protected Side side;
	protected Style style;
	protected int thickness;
	
	public LineBorderSegment(Color c, Side side, Style style, int thickness) {
		this.color = c;
		this.side = side;
		this.thickness = thickness;
		this.style = style;
	}
	
	public void paint(Graphics g, int x1, int y1, Direction dir, int length) {
        Color oldColor = g.getColor();

        if (this.color != null) {
        	g.setColor(this.color);
        } else {
        	g.setColor(Color.GRAY.brighter());
        }
        
        drawLine(g, x1, y1, dir, length);
        
        g.setColor(oldColor);

	}
	
	public abstract void drawLine(Graphics g, int x1, int y1, Direction dir, int length);
	
	public static enum Side {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM,
		INSIDE_V,
		INSIDE_H
	}
	
	public static enum Style {
		SOLID,
		DASHED
	}
	
	public static enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
}// LineBorderSegment class



















