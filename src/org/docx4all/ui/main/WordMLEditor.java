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

package org.docx4all.ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.Document;

import org.docx4all.script.FxScriptUIHelper;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.menu.EditMenu;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.ui.menu.FormatMenu;
import org.docx4all.ui.menu.HelpMenu;
import org.docx4all.ui.menu.WindowMenu;
import org.docx4all.util.SwingUtil;
import org.jdesktop.application.SingleFrameApplication;

/**
 *	@author Jojada Tirtowidjojo - 13/11/2007
 */
public class WordMLEditor extends SingleFrameApplication {
	private JDesktopPane _desktop;
	private Map<String, JEditorPane> _editorMap;
	private InternalFrameListener _internalFrameListener;
	private ToolBarStates _toolbarStates;
	
	public static void main(String[] args) {
        launch(WordMLEditor.class, args);
	}

    @Override protected void startup() {
    	_editorMap = new HashMap<String, JEditorPane>();
    	_internalFrameListener = new InternalFrameListener();
    	_toolbarStates = new ToolBarStates();
    	
    	getMainFrame().setJMenuBar(createMenuBar());
    	
        show(createMainPanel());
    }
    
    public void createInternalFrame(File f) {
    	if (f == null) {
    		return;
    	}
    	
    	JInternalFrame iframe = null;
    	
    	JEditorPane editor = _editorMap.get(f.getAbsolutePath());
        if (editor != null) {
        	iframe = 
        		(JInternalFrame) SwingUtilities.getAncestorOfClass(
        				JInternalFrame.class, 
        				editor);
        	
        } else {
        	iframe = new JInternalFrame(f.getName(), true, true, true, true);
        	iframe.addInternalFrameListener(_internalFrameListener);
        	iframe.addInternalFrameListener(_toolbarStates);
        	
        	editor = createEditor(f);
        	JPanel panel = FxScriptUIHelper.getInstance().createEditorPanel(editor);
        	
        	iframe.getContentPane().add(panel);
        	iframe.pack();
        	_desktop.add(iframe);
        	
        	editor.requestFocusInWindow();
        	editor.select(0,0);
        	
        	_editorMap.put(f.getAbsolutePath(), editor);
        }
        
		try {
        	iframe.show();
			iframe.setIcon(false);
			iframe.setMaximum(true);
			iframe.setSelected(true);
		} catch (PropertyVetoException exc) {
			// do nothing
		}
    }
    
    public JDesktopPane getDesktopPane() {
    	return _desktop;
    }
    
    public ToolBarStates getToolbarStates() {
    	return _toolbarStates;
    }
    
    public JEditorPane getCurrentEditor() {
    	return _toolbarStates.getCurrentEditor();
    }
    
    public void showMessageDialog(String title, String message, int type) {
    	JOptionPane.showMessageDialog(getMainFrame(), message, title, type);
    }
    
    private JEditorPane createEditor(File f) {
    	JEditorPane editor = new JTextPane();
    	editor.addFocusListener(_toolbarStates);
    	
    	WordMLEditorKit editorKit = new WordMLEditorKit();
    	editor.setEditorKit(editorKit);
    	
    	Document doc = null;
    	if (f.exists()) {
    		try {
    			doc = editorKit.read(f);
    			doc.putProperty(WordMLDocument.FILE_PATH_PROPERTY, f.getAbsoluteFile());
    			
    		} catch (IOException exc) {
    			exc.printStackTrace();
    			showMessageDialog(
    				"Error reading file " + f.getName(),
    				"I/O Error",
    				JOptionPane.ERROR_MESSAGE);
    		}
    	} else {
    		doc = editorKit.createDefaultDocument();
    	}
    	
    	doc.addDocumentListener(_toolbarStates);
    	editor.setDocument(doc);
    	
    	return editor;
    }
    
    private JComponent createMainPanel() {
    	_desktop = new JDesktopPane();
    	_desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    	_desktop.setBackground(Color.LIGHT_GRAY);
    	
    	JPanel toolbar = FxScriptUIHelper.getInstance().createToolBar();
    	
    	JPanel panel = new JPanel(new BorderLayout());
    	panel.add(toolbar, BorderLayout.NORTH);
    	panel.add(_desktop, BorderLayout.CENTER);
    	
    	panel.setBorder(new EmptyBorder(0, 2, 2, 2)); // top, left, bottom, right
    	panel.setPreferredSize(new Dimension(640, 480));
    	
    	return panel;
    }
    
    private JMenuBar createMenuBar() {
    	JMenuBar menubar = new JMenuBar();
    	
    	JMenu fileMenu = FileMenu.getInstance().createJMenu();
    	JMenu editMenu = EditMenu.getInstance().createJMenu();
    	JMenu formatMenu = FormatMenu.getInstance().createJMenu();
    	JMenu windowMenu = WindowMenu.getInstance().createJMenu();
    	JMenu helpMenu = HelpMenu.getInstance().createJMenu();
    	
    	menubar.add(fileMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(editMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(formatMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(windowMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(helpMenu);
    	
    	return menubar;
    }
    
    private class InternalFrameListener extends InternalFrameAdapter {
    	
        public void internalFrameIconified(InternalFrameEvent e) {
			// Sets JInternalFrame's maximum property to false.
			// 
			// When a user clicks the minimize/maximize button of
			// JInternalFrame, its maximum property value remains 
        	// unchanged. This subsequently causes 
        	// JInternalFrame.setMaximum() not working.
			JInternalFrame frame = (JInternalFrame) e.getSource();
			try {
				frame.setMaximum(false);
			} catch (PropertyVetoException exc) {
				;// do nothing
			}
		}

        public void internalFrameDeiconified(InternalFrameEvent e) {
			// Sets JInternalFrame's maximum property to false.
			// 
			// When a user clicks the minimize/maximize button of
			// JInternalFrame, its maximum property value remains 
        	// unchanged. This subsequently causes 
        	// JInternalFrame.setMaximum() not working.
        	JInternalFrame frame = (JInternalFrame) e.getSource();
        	try {
        		frame.setMaximum(true);
        	} catch (PropertyVetoException exc) {
        		;//do nothing
        	}
        }
        
        public void internalFrameOpened(InternalFrameEvent e) {
			JInternalFrame frame = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().addWindowMenuItem(frame);
        }

        public void internalFrameClosed(InternalFrameEvent e) {
			JInternalFrame frame = (JInternalFrame) e.getSource();
			
			JEditorPane editor = SwingUtil.getJEditorPane(frame);
			if (editor != null) {
				String filepath = 
					(String) editor.getDocument().getProperty(
						WordMLDocument.FILE_PATH_PROPERTY);
				_editorMap.remove(filepath);
			}
			
			WindowMenu.getInstance().removeWindowMenuItem(frame);
        	_desktop.remove(frame) ;
        }

        public void internalFrameActivated(InternalFrameEvent e) {
			JInternalFrame frame = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().selectWindowMenuItem(frame);
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
			JInternalFrame frame = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().unSelectWindowMenuItem(frame);
        }

    }//InternalFrameListener inner class

}// WordMLEditor class



















