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

import java.awt.Color;
import java.util.BitSet;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;

import org.docx4all.xml.ElementML;
import org.docx4all.xml.HyperlinkML;
import org.docx4all.xml.RunDelML;
import org.docx4all.xml.RunInsML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 28/08/2008
 */
public class LabelView extends javax.swing.text.LabelView {
	private boolean impliedUnderline;
	private boolean impliedStrikethrough;
	private Color foreground;
    private JustificationInfo justificationInfo = null;
	
    public LabelView(Element elem) {
    	super(elem);
    	
    	impliedUnderline = impliedStrikethrough = false;
    	foreground = null;
    	
    	ElementML parent = 
    		((DocumentElement) getElement().getParentElement()).getElementML();
    	if (parent instanceof RunML) {
    		if (parent.getParent() instanceof RunInsML) {
    			foreground = Color.RED;
    			impliedUnderline = true;
    		} else if (parent.getParent() instanceof RunDelML) {
    			foreground = Color.RED;
    			impliedStrikethrough = true;
    		} else if (parent.getParent() instanceof HyperlinkML) {
    			foreground = Color.BLUE;
    			impliedUnderline = true;
    		}
    	}
    }

    public boolean isImpliedUnderline() {
    	return impliedUnderline;
    }
    
    public boolean isImpliedStrikethrough() {
    	return impliedStrikethrough;
    }
    
    public Color getForeground() {
    	return (foreground == null) ? super.getForeground() : foreground;
    }
    
    public Segment getTextSegment(int p0, int p1) {
        Segment text = new Segment();
        try {
            Document doc = getDocument();
            doc.getText(p0, p1 - p0, text);
        } catch (BadLocationException bl) {
            throw new IllegalStateException("LabelView: Stale view: " + bl);
        }
        return text;
    }

    JustificationInfo getJustificationInfo(int rowStartOffset) {
        if (justificationInfo != null) {
            return justificationInfo;
        }
        //states for the parsing
        final int TRAILING = 0;
        final int CONTENT  = 1;
        final int SPACES   = 2;
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();
        Segment segment = getTextSegment(startOffset, endOffset);
        int txtOffset = segment.offset;
        int txtEnd = segment.offset + segment.count - 1;
        int startContentPosition = txtEnd + 1;
        int endContentPosition = txtOffset - 1;
        int lastTabPosition = txtOffset - 1;
        int trailingSpaces = 0;
        int contentSpaces = 0;
        int leadingSpaces = 0;
        boolean hasTab = false;
        BitSet spaceMap = new BitSet(endOffset - startOffset + 1);

        //we parse conent to the right of the rightmost TAB only.
        //we are looking for the trailing and leading spaces.
        //position after the leading spaces (startContentPosition)
        //position before the trailing spaces (endContentPosition)
        for (int i = txtEnd, state = TRAILING; i >= txtOffset; i--) {
            if (' ' == segment.array[i]) {
                spaceMap.set(i - txtOffset);
                if (state == TRAILING) {
                    trailingSpaces++;
                } else if (state == CONTENT) {
                    state = SPACES;
                    leadingSpaces = 1;
                } else if (state == SPACES) {
                    leadingSpaces++;
                }
            } else if ('\t' == segment.array[i]) {
                hasTab = true;
                break;
            } else {
                if (state == TRAILING) {
                    if ('\n' != segment.array[i]
                          && '\r' != segment.array[i]) {
                        state = CONTENT;
                        endContentPosition = i;
                    }
                } else if (state == CONTENT) {
                    //do nothing
                } else if (state == SPACES) {
                    contentSpaces += leadingSpaces;
                    leadingSpaces = 0;
                }
                startContentPosition = i;
            }
        }

        int startJustifiableContent = -1;
        if (startContentPosition < txtEnd) {
            startJustifiableContent = 
                startContentPosition - txtOffset;
        }
        int endJustifiableContent = -1;
        if (endContentPosition > txtOffset) {
            endJustifiableContent = 
                endContentPosition - txtOffset;
        }
        justificationInfo = 
            new JustificationInfo(startJustifiableContent,
                                  endJustifiableContent,
                                  leadingSpaces,
                                  contentSpaces,
                                  trailingSpaces,
                                  hasTab,
                                  spaceMap);
        return justificationInfo;
    }

    protected void setPropertiesFromAttributes() {
    	super.setPropertiesFromAttributes();
    	if (isImpliedUnderline()) {
    		setUnderline(true);
    	}
    	if (isImpliedStrikethrough()) {
    		setStrikeThrough(true);
    	}
    }
    
    /**
     * Class to hold data needed to justify this GlyphView in a PargraphView.Row
     */
    static class JustificationInfo {
        //justifiable content start
        final int start;
        //justifiable content end
        final int end;
        final int leadingSpaces;
        final int contentSpaces;
        final int trailingSpaces;
        final boolean hasTab;
        final BitSet spaceMap;
        JustificationInfo(int start, int end,
                          int leadingSpaces, 
                          int contentSpaces,
                          int trailingSpaces,
                          boolean hasTab,
                          BitSet spaceMap) {
            this.start = start;
            this.end = end;
            this.leadingSpaces = leadingSpaces;
            this.contentSpaces = contentSpaces;
            this.trailingSpaces = trailingSpaces;
            this.hasTab = hasTab;
            this.spaceMap = spaceMap;
        }
    }
}// LabelView class



















