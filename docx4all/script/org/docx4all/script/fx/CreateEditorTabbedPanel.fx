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

package org.docx4all.script.fx;

import org.docx4all.swing.WordMLTextPane;

import org.docx4all.script.fx.ui.EditorPanel;
import org.docx4all.script.fx.ui.widget.ScrollableEditorPane;

import javafx.ui.HorizontalScrollBarPolicy;
import javafx.ui.Tab;
import javafx.ui.TabbedPane;

var editorView = editorView:<<org.docx4all.swing.WordMLTextPane>>;
var editorViewTabTitle = editorViewTabTitle:<<java.lang.String>>;
var sourceView = sourceView:<<javax.swing.JEditorPane>>;
var sourceViewTabTitle = sourceViewTabTitle:<<java.lang.String>>;

var tabbedPane = TabbedPane {
    tabs: [
        Tab {
            var editorView = ScrollableEditorPane {
                        editor: editorView
                        horizontalScrollBarPolicy: NEVER:HorizontalScrollBarPolicy
            }
            content: EditorPanel {editorPane: editorView}
            title: editorViewTabTitle
        },
        Tab {
            var sourceView = ScrollableEditorPane {
                        editor: sourceView
                        horizontalScrollBarPolicy: NEVER:HorizontalScrollBarPolicy
            }
            content: EditorPanel {editorPane: sourceView}
            title: sourceViewTabTitle
        }
    ]
};

return tabbedPane.getComponent();



















