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

package org.docx4all.script.fx.ui;

import org.docx4all.script.fx.ui.widget.WordMLEditorPane;

import java.awt.Dimension;

import java.io.File;
import java.lang.IllegalArgumentException;
import java.lang.System;

import javafx.ui.BevelBorder;
import javafx.ui.BevelType;
import javafx.ui.BorderPanel;
import javafx.ui.Column;
import javafx.ui.EmptyBorder;
import javafx.ui.GroupPanel;
import javafx.ui.HorizontalScrollBarPolicy;
import javafx.ui.Label;
import javafx.ui.Panel;
import javafx.ui.Row;
import javafx.ui.ScrollPane;

import javax.swing.JEditorPane;


public class EditorPanel extends BorderPanel {
	private attribute file: File;
	private attribute wordML: WordMLEditorPane;
	private attribute statusPanel1: Panel;
	private attribute statusPanel2: Panel;
	private attribute statusPanel3: Panel;
}

trigger on new EditorPanel {
    var jEditorPane = editorPane:<<javax.swing.JEditorPane>>;
    
    center = WordMLEditorPane {
        editor: jEditorPane
        horizontalScrollBarPolicy: NEVER:HorizontalScrollBarPolicy
        attribute: wordML
    };
    
	bottom = GroupPanel {
		autoCreateContainerGaps: false
		border: EmptyBorder {
			left:2
			top:2
			right:2
			bottom:2
		}
			
		var h1row = new Row()
		var v1col = new Column{resizable:true}
		
		rows: h1row
		columns: v1col
		
		content: GroupPanel {
			autoCreateContainerGaps: false
			row: h1row
			column: v1col
			
			var h2row = new Row()
			var s1col = new Column()
			var s2col = new Column {resizable:true}
			var s3col = new Column()
			
			rows: h2row
			columns: [s1col, s2col, s3col]
			
			content: [
				Panel {
					border: BevelBorder {
						style: LOWERED:BevelType
					}
					preferredSize: {width:100, height:30}
					row: h2row
					column: s1col
					attribute: statusPanel1
				},
				Panel {
					border: BevelBorder {
						style: LOWERED:BevelType
					}
					preferredSize: new Dimension(200,30)
					row:h2row
					column: s2col
					attribute: statusPanel2
				},
				Panel {
					border: BevelBorder {
						style: LOWERED:BevelType
					}
					preferredSize: {width:100, height:30}
					row: h2row
					column: s3col
					attribute: statusPanel3
				}				
			]
		}
	};	
}

return (new EditorPanel()).getComponent();

















