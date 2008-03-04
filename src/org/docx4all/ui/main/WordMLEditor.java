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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
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
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import org.apache.log4j.Logger;
import org.docx4all.datatransfer.TransferHandler;
import org.docx4all.script.FxScriptUIHelper;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLDocumentFilter;
import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.menu.EditMenu;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.ui.menu.FormatMenu;
import org.docx4all.ui.menu.HelpMenu;
import org.docx4all.ui.menu.ViewMenu;
import org.docx4all.ui.menu.WindowMenu;
import org.docx4all.util.DocUtil;
import org.docx4all.util.SwingUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

/**
 *	@author Jojada Tirtowidjojo - 13/11/2007
 */
public class WordMLEditor extends SingleFrameApplication {
	private static Logger log = Logger.getLogger(WordMLEditor.class);
	
	private JDesktopPane _desktop;
	private Map<String, JInternalFrame> _iframeMap;
	private InternalFrameListener _internalFrameListener;
	private ToolBarStates _toolbarStates;
	
	public static void main(String[] args) {
        launch(WordMLEditor.class, args);
	}

    @Override protected void startup() {
    	_iframeMap = new HashMap<String, JInternalFrame>();
    	_internalFrameListener = new InternalFrameListener();
    	_toolbarStates = new ToolBarStates();
    	
    	addExitListener(new WmlExitListener());
    	
    	getMainFrame().setJMenuBar(createMenuBar());
    	
        show(createMainPanel());
    }
    
    public void closeAllInternalFrames() { 
    	
    	List<JInternalFrame> list = getAllInternalFrames();
    	
    	//Start from current editor's frame
    	JInternalFrame currentFrame = getCurrentInternalFrame();
    	list.remove(currentFrame);
    	list.add(0, currentFrame);
    	
    	for (final JInternalFrame iframe: list) {
    		final Runnable disposeRunnable = new Runnable() {
    			public void run() {
    				iframe.dispose();
    			}
    		};
    		
    		if (getToolbarStates().isDocumentDirty(iframe)) {
    			try {
    				iframe.setSelected(true);
    				iframe.setIcon(false);
    			} catch (PropertyVetoException exc) {
    				;//ignore
    			}
    			
    			int answer = showConfirmClosingInternalFrame(iframe, "internalframe.close");
    			if (answer == JOptionPane.CANCEL_OPTION) {
    				break;
    			}
    		}
    		
    		SwingUtilities.invokeLater(disposeRunnable);
    	}
    }
    
    public void closeInternalFrame(JInternalFrame iframe) {
    	boolean canClose = true;
    	
		if (getToolbarStates().isDocumentDirty(iframe)) {
			try {
				iframe.setSelected(true);
				iframe.setIcon(false);
			} catch (PropertyVetoException exc) {
				;//ignore
			}
			
			int answer = showConfirmClosingInternalFrame(iframe, "internalframe.close");
			canClose = (answer != JOptionPane.CANCEL_OPTION); 
		}
		
		if (canClose) {
			iframe.dispose();
		}
    }
    
    public void createInternalFrame(File f) {
    	if (f == null) {
    		return;
    	}
    	
    	JInternalFrame iframe = _iframeMap.get(f.getAbsolutePath());
        if (iframe != null) {
        	iframe.setVisible(true);
        	
        } else {
        	iframe = new JInternalFrame(f.getName(), true, true, true, true);
        	iframe.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        	iframe.addInternalFrameListener(_internalFrameListener);
        	iframe.addInternalFrameListener(_toolbarStates);
        	iframe.addPropertyChangeListener(WindowMenu.getInstance());
        	
        	JEditorPane editorView = createEditorView(f);
        	JPanel panel = FxScriptUIHelper.getInstance().createEditorPanel(editorView);
        	
        	iframe.getContentPane().add(panel);
        	iframe.pack();
        	_desktop.add(iframe);
        	
        	editorView.requestFocusInWindow();
        	editorView.select(0,0);
        	
        	String filePath = f.getAbsolutePath();
        	iframe.putClientProperty(WordMLDocument.FILE_PATH_PROPERTY, filePath);
        	_iframeMap.put(filePath, iframe);
        	
           	iframe.show();
        }
        
    	try {
    		iframe.setSelected(true);
			iframe.setIcon(false);
			iframe.setMaximum(true);
		} catch (PropertyVetoException exc) {
			// do nothing
		}
    }
    
    public void updateInternalFrame(File oldFile, File newFile) {
    	if (oldFile.equals(newFile)) {
    		return;
    	}
    	
    	String filePath = oldFile.getAbsolutePath();
    	JInternalFrame iframe = _iframeMap.remove(filePath);
    	if (iframe != null) {
    		filePath = newFile.getAbsolutePath();
    		iframe.putClientProperty(WordMLDocument.FILE_PATH_PROPERTY, filePath);
			iframe.setTitle(newFile.getName());   		
    	}    	
    }
    
    public JDesktopPane getDesktopPane() {
    	return _desktop;
    }
    
    public ToolBarStates getToolbarStates() {
    	return _toolbarStates;
    }
    
    public JInternalFrame getCurrentInternalFrame() {
    	return _toolbarStates.getCurrentInternalFrame();
	}
    
    public JEditorPane getCurrentEditor() {
    	return _toolbarStates.getCurrentEditor();
    }
    
    public List<JInternalFrame> getAllInternalFrames() {
    	return new ArrayList<JInternalFrame>(_iframeMap.values());
    }
    
    public String getEditorViewTabTitle() {
        ResourceMap rm = getContext().getResourceMap(WordMLEditor.class);
        return rm.getString(Constants.EDITOR_VIEW_TAB_TITLE);
    }
    
    public String getSourceViewTabTitle() {
        ResourceMap rm = getContext().getResourceMap(WordMLEditor.class);
        return rm.getString(Constants.SOURCE_VIEW_TAB_TITLE);
    }
    
    public String getUntitledFileName() {
        ResourceMap rm = getContext().getResourceMap(WordMLEditor.class);
        String filename = rm.getString(Constants.UNTITLED_FILE_NAME);
        if (filename == null || filename.length() == 0) {
        	filename = "Untitled";
        }
        return filename;
    }
    
    public int showConfirmDialog(
    	String title, 
    	String message, 
    	int optionType, 
    	int messageType) {
    	return JOptionPane.showConfirmDialog(
    			getMainFrame(), message, title, optionType, messageType);
    }
    
    public int showConfirmDialog(
    	String title,
    	String message,
    	int optionType,
    	int messageType,
    	Object[] options,
    	Object initialValue) {
    		
    	return JOptionPane.showOptionDialog(
    			getMainFrame(), message, title, optionType, messageType,
                null, options, initialValue);
    }
            
    public void showMessageDialog(String title, String message, int optionType) {
    	JOptionPane.showMessageDialog(getMainFrame(), message, title, optionType);
    }
    
    public void createSourceViewTab() {
		JEditorPane editorView = getCurrentEditor();
		if (editorView == null || !(editorView instanceof WordMLTextPane)) {
			return;
		}
		JInternalFrame iframe = getCurrentInternalFrame();
		Rectangle bounds = iframe.getBounds();
		iframe.getContentPane().removeAll();

		JEditorPane sourceView = createSourceView((WordMLTextPane) editorView);
		JTabbedPane tabbedPane = 
			FxScriptUIHelper.getInstance().createEditorTabbedPanel(
				(WordMLTextPane) editorView, 
				getEditorViewTabTitle(),
				sourceView,
				getSourceViewTabTitle());
		
    	iframe.getContentPane().add(tabbedPane);
    	iframe.validate();
    	iframe.setBounds(bounds);
    	
    	int idx = tabbedPane.indexOfTab(getSourceViewTabTitle());
    	tabbedPane.setSelectedIndex(idx);
    	
		tabbedPane.addChangeListener(new ViewChangeListener());
	}
    
    public void closeSourceViewTab() {
		JEditorPane editor = getCurrentEditor();
		if (editor == null || (editor instanceof WordMLTextPane)) {
			return;
		}
		
		JInternalFrame iframe = getCurrentInternalFrame();
		Rectangle bounds = iframe.getBounds();
		
		editor = SwingUtil.getWordMLTextPane(iframe);
		iframe.getContentPane().removeAll();
		
    	JPanel panel = FxScriptUIHelper.getInstance().createEditorPanel(editor);	
    	iframe.getContentPane().add(panel);
    	iframe.validate();
    	iframe.setBounds(bounds);
    	
    	editor.requestFocusInWindow();
    }
    
    private JEditorPane createSourceView(WordMLTextPane editorView) {
    	WordMLEditorKit kit = (WordMLEditorKit) editorView.getEditorKit();
    	kit.saveCaretText();
    	
    	DocumentElement elem = 
    		(DocumentElement) editorView.getDocument().getDefaultRootElement();
    	WordprocessingMLPackage wmlPackage =
    		((DocumentML) elem.getElementML()).getWordprocessingMLPackage();
    	String filePath = 
    		(String) editorView.getDocument().getProperty(WordMLDocument.FILE_PATH_PROPERTY);
    	
    	//Create the Source View
    	JEditorPane sourceView = new JTextPane();
    	
		//Do not include the last paragraph.
		elem = (DocumentElement) elem.getElement(elem.getElementCount() - 1);
		ElementML paraML = elem.getElementML();
		ElementML bodyML = paraML.getParent();
		paraML.delete();

    	Document doc = DocUtil.read(sourceView.getEditorKit(), wmlPackage);
    	
        //Remember to put 'paraML' as last paragraph
        bodyML.addChild(paraML);

		doc.putProperty(WordMLDocument.FILE_PATH_PROPERTY, filePath);
		doc.putProperty(WordMLDocument.WML_PACKAGE_PROPERTY, wmlPackage);
    	doc.addDocumentListener(getToolbarStates());
    	
    	sourceView.addFocusListener(getToolbarStates());
    	sourceView.setDocument(doc);
    	sourceView.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.TRUE);
    	
    	return sourceView;
    }
    
    private JEditorPane createEditorView(File f) {
    	JEditorPane editorView = new WordMLTextPane();
    	editorView.addFocusListener(_toolbarStates);
    	editorView.setTransferHandler(new TransferHandler());
    	
		WordMLEditorKit editorKit = (WordMLEditorKit) editorView.getEditorKit();
		editorKit.addInputAttributeListener(_toolbarStates);
		
    	AbstractDocument doc = null;
    	
    	if (f.exists()) {
    		try {
    			doc = editorKit.read(f);
    			
    		} catch (IOException exc) {
    			exc.printStackTrace();
    			showMessageDialog(
    				"Error reading file " + f.getName(),
    				"I/O Error",
    				JOptionPane.ERROR_MESSAGE);
    			doc = null;
    		}
    	}
    	
    	if (doc == null) {
    		doc = (AbstractDocument) editorKit.createDefaultDocument();
    	}
    	
		doc.putProperty(WordMLDocument.FILE_PATH_PROPERTY, f.getAbsolutePath());
    	doc.addDocumentListener(_toolbarStates);
    	doc.setDocumentFilter(new WordMLDocumentFilter());
    	editorView.setDocument(doc);
    	editorView.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.TRUE);
    	
    	return editorView;
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
    
    private int showConfirmClosingInternalFrame(JInternalFrame iframe, String resourceKeyPrefix) {
    	int answer = JOptionPane.CANCEL_OPTION;
    	
		String filePath = 
			(String) iframe.getClientProperty(WordMLDocument.FILE_PATH_PROPERTY);
			
		ResourceMap rm = getContext().getResourceMap();
		String title = 
			rm.getString(resourceKeyPrefix + ".dialog.title")
			+ " " 
			+ filePath.substring(filePath.lastIndexOf(File.separator) + 1);
		String message = 
			filePath 
			+ "\n"
			+ rm.getString(resourceKeyPrefix + ".confirmMessage");
		Object[] options = {
			rm.getString(resourceKeyPrefix + ".confirm.saveNow"),
			rm.getString(resourceKeyPrefix + ".confirm.dontSave"),
			rm.getString(resourceKeyPrefix + ".confirm.cancel")
		};
		answer = showConfirmDialog(title, message,
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				options,
				options[0]);
		if (answer == JOptionPane.CANCEL_OPTION) {
			;
		} else if (answer == JOptionPane.YES_OPTION) {
			boolean success = FileMenu.getInstance().save(iframe, null,
					FileMenu.SAVE_FILE_ACTION_NAME);
			if (success) {
				getToolbarStates().setDocumentDirty(iframe, false);
			}
		} else {
			//getToolbarStates().setDocumentDirty(iframe, false);
		}
		
		return answer;
    }
    
    private JMenuBar createMenuBar() {
    	JMenuBar menubar = new JMenuBar();
    	
    	JMenu fileMenu = FileMenu.getInstance().createJMenu();
    	JMenu editMenu = EditMenu.getInstance().createJMenu();
    	JMenu formatMenu = FormatMenu.getInstance().createJMenu();
    	JMenu viewMenu = ViewMenu.getInstance().createJMenu();
    	JMenu windowMenu = WindowMenu.getInstance().createJMenu();
    	JMenu helpMenu = HelpMenu.getInstance().createJMenu();
    	
    	menubar.add(fileMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(editMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(formatMenu);
    	menubar.add(Box.createRigidArea(new Dimension(10, 0)));
    	menubar.add(viewMenu);
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
			JInternalFrame iframe = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().addWindowMenuItem(iframe);
        }

        public void internalFrameClosing(InternalFrameEvent e) {
			JInternalFrame iframe = (JInternalFrame) e.getSource();
			if (getToolbarStates().isDocumentDirty(iframe)) {
				int answer = showConfirmClosingInternalFrame(iframe, "internalframe.close");
				if (answer != JOptionPane.CANCEL_OPTION) {
					iframe.dispose();
				}
			} else {
				iframe.dispose();
			}
        }
        
        public void internalFrameClosed(InternalFrameEvent e) {
			JInternalFrame iframe = (JInternalFrame) e.getSource();
			String filePath = (String) iframe.getClientProperty(WordMLDocument.FILE_PATH_PROPERTY);
			_iframeMap.remove(filePath);
			
			WindowMenu.getInstance().removeWindowMenuItem(iframe);
        	_desktop.remove(iframe) ;
        }

        public void internalFrameActivated(InternalFrameEvent e) {
			JInternalFrame iframe = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().selectWindowMenuItem(iframe);
        }

        public void internalFrameDeactivated(InternalFrameEvent e) {
			JInternalFrame iframe = (JInternalFrame) e.getSource();
			WindowMenu.getInstance().unSelectWindowMenuItem(iframe);
        }

    } //InternalFrameListener inner class

    private class ViewChangeListener implements ChangeListener {
    	public void stateChanged(ChangeEvent event) {
    		JTabbedPane pane = (JTabbedPane) event.getSource();
    		WordMLTextPane editorView = 
    			(WordMLTextPane)
    			SwingUtil.getDescendantOfClass(WordMLTextPane.class, pane, true);
    		JEditorPane sourceView = 
    			(JEditorPane)
    			SwingUtil.getDescendantOfClass(JTextPane.class, pane, true);
    		JEditorPane selectedView = 
    			(JEditorPane)
    			SwingUtil.getDescendantOfClass(
    				JEditorPane.class, (Container) pane.getSelectedComponent(), false);
    		if (selectedView == editorView) {
    			Boolean isSynched = (Boolean) sourceView.getClientProperty(Constants.SYNCHRONIZED_FLAG);
    			if (!isSynched.booleanValue()) {
    				//means that source view has been edited and
    				//editorView has to be synchronised.
    				//Firstly, save source view content into WordprocessingMLPackage
    				EditorKit kit = sourceView.getEditorKit();
    				Document sourceDoc = sourceView.getDocument();
    				WordprocessingMLPackage wmlPackage = 
    					(WordprocessingMLPackage)
    						sourceDoc.getProperty(
    							WordMLDocument.WML_PACKAGE_PROPERTY);
    				DocUtil.write(kit, sourceDoc, wmlPackage);
    				sourceDoc = null;
    				
    				int caretPos = editorView.getCaretPosition();
    				
    				//Now the editor view content becomes invalid because
    				//its WordprocessingMLPackage's main document part has been
    				//updated by the saving action above.
    				//Need to create a new document for editor view.
    				WordMLDocument oldDoc = (WordMLDocument) editorView.getDocument();
    		    	String filePath = 
    		    		(String) oldDoc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
    		    	
    				kit = editorView.getEditorKit();
    				WordMLDocument newDoc = 
    					((WordMLEditorKit) kit).read(new DocumentML(wmlPackage));
    		    	newDoc.putProperty(WordMLDocument.FILE_PATH_PROPERTY, filePath);
    		    	newDoc.addDocumentListener(getToolbarStates());
    		    	newDoc.setDocumentFilter(oldDoc.getDocumentFilter());
    		    	
    		    	log.debug("stateChanged(): NEW Document Structure...");
    		    	DocUtil.displayStructure(newDoc);
    		    	
    		    	editorView.setDocument(newDoc);
    		    	editorView.validate();
    		    	editorView.repaint();
    		    	editorView.setCaretPosition(caretPos);
    		    	
    		    	//reset SYNCHRONIZED_FLAG of source view
    		    	sourceView.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.TRUE);
    			}
    		} else if (selectedView == sourceView) {
    			Boolean isSynched = (Boolean) editorView.getClientProperty(Constants.SYNCHRONIZED_FLAG);
    			if (!isSynched.booleanValue()) {
    				int caretPos = sourceView.getCaretPosition();
    				
    				JEditorPane newView = createSourceView(editorView);
    				Document newDoc = newView.getDocument();
    				
    				sourceView.setDocument(newDoc);
    				sourceView.validate();
    				sourceView.repaint();
    				sourceView.setCaretPosition(caretPos);
    				
    				//reset SYNCHRONIZED_FLAG of editor view
    				editorView.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.TRUE);
    			}    			
    		}
    	}
    } //ViewChangeListener inner class
    
    private class WmlExitListener implements ExitListener {
    	public boolean canExit(EventObject event) {
    		boolean cancelExit = false;
    		
    		if (getToolbarStates().isAnyDocumentDirty()) {
    	    	List<JInternalFrame> list = getAllInternalFrames();
    	    	
    	    	//Start from current editor's frame
    	    	JInternalFrame currentFrame = getCurrentInternalFrame();
    	    	list.remove(currentFrame);
    	    	list.add(0, currentFrame);
    	    	
    	    	for (JInternalFrame iframe: list) {
    				if (getToolbarStates().isDocumentDirty(iframe)) {
    					try {
    						iframe.setSelected(true);
    						iframe.setIcon(false);
    					} catch (PropertyVetoException exc) {
    						;//ignore
    					}
        			
    					int answer = 
    						showConfirmClosingInternalFrame(
    							iframe, 
    							"Application.exit.saveFirst");
    					if (answer == JOptionPane.CANCEL_OPTION) {
    						cancelExit = true;
    						break;
    					}
    				}    	    		
    	    	} //for (iframe) loop
    		} //if (getToolbarStates().isAnyDocumentDirty()

    		boolean canExit = false;
    		if (!cancelExit) {
            	ResourceMap rm = getContext().getResourceMap();
                String title = 
                	rm.getString("Application.exit.dialog.title");
    			String message = 
                	rm.getString("Application.exit.confirmMessage");
        		int answer = 
        			showConfirmDialog(
        				title, 
        				message, 
        				JOptionPane.YES_NO_OPTION, 
        				JOptionPane.QUESTION_MESSAGE);
                canExit = (answer == JOptionPane.YES_OPTION);
    		}//if (!canExit)
    		
            return canExit;
    	} //canExit()
    	
    	public void willExit(EventObject event) {
    		;//not implemented
    	}	
    }//WMLExitListener inner class
    
}// WordMLEditor class



















