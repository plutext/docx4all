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

package org.docx4all.script;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.docx4all.swing.WordMLTextPane;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.EditMenu;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.ui.menu.FormatMenu;

/**
 *	@author Jojada Tirtowidjojo - 28/11/2007
 */
public class FxScriptUIHelper {
	private final static FxScriptUIHelper _instance = new FxScriptUIHelper();
	
	public final static FxScriptUIHelper getInstance() {
		return _instance;
	}
	
	private final FxScriptEngine _fxEngine = new FxScriptEngine();
	
	private FxScriptUIHelper() {
		;//singleton
	}
	
    public JPanel createToolBar() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        
    	Map<String, Object> menus = new HashMap<String, Object>();
    	menus.put(ToolBarStates.SCRIPT_BINDING_KEY, editor.getToolbarStates());
    	menus.put(FileMenu.SCRIPT_BINDING_KEY, FileMenu.getInstance());
    	menus.put(EditMenu.SCRIPT_BINDING_KEY, EditMenu.getInstance());
    	menus.put(FormatMenu.SCRIPT_BINDING_KEY, FormatMenu.getInstance());
    	
    	JPanel toolbar = (JPanel) _fxEngine.run(FxScript.TOOLBAR_FX, menus);
    	return toolbar;
    }

    public JPanel createEditorPanel(JEditorPane editor) {
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("editorPane:javax.swing.JEditorPane", editor);
    	
    	JPanel panel = (JPanel) _fxEngine.run(FxScript.CREATE_EDITOR_PANEL_FX, params);
    	return panel;
    }
    
    public JTabbedPane createEditorTabbedPanel(WordMLTextPane editor, JEditorPane sourceEditor) {
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("wmlTextPane:org.docx4all.swing.WordMLTextPane", editor);
    	params.put("xmlSourceEditor:javax.swing.JEditorPane", sourceEditor);
    	
    	JTabbedPane panel = (JTabbedPane) _fxEngine.run(FxScript.CREATE_EDITOR_TABBED_PANEL_FX, params);
    	return panel;
    }

}// FxScriptFactory class



















