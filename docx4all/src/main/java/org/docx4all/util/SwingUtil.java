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

package org.docx4all.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.ImpliedParagraphView;
import org.docx4all.swing.text.SdtBlockView;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class SwingUtil {
	private static Logger log = Logger.getLogger(SwingUtil.class);
	
	public final static JMenuItem getMenuItem(JMenu menu, String menuItemText) {
		JMenuItem theItem = null;
		
		int idx = getMenuItemIndex(menu, menuItemText);
		if (idx > -1) {
			theItem = (JMenuItem) menu.getMenuComponent(idx);
		}
		
		return theItem;
	}
	
	public final static int getMenuItemIndex(JMenu menu, String menuItemText) {
		int theIdx = -1;
		
		for (int i=0; i < menu.getMenuComponentCount(); i++) {
			Component c = menu.getMenuComponent(i);
			if (c instanceof JMenuItem) {
				JMenuItem mi = (JMenuItem) c;
				if (mi.getText().equals(menuItemText)) {
					theIdx = i;
					i = menu.getMenuComponentCount();// break
				}
			}
		}
		
		return theIdx;
	}
	
	public final static JMenuItem createCheckBoxMenuItem(final JInternalFrame f) {
        final JMenuItem theItem = new JCheckBoxMenuItem(f.getTitle());
        theItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                	f.setSelected(true);
                    f.setIcon(false);
                } catch (PropertyVetoException exc) {
                    ;//do nothing
                }
                f.show();
                
                if (!theItem.isSelected()) {
                	theItem.setSelected(true);
                }
            }        	
        });
        return theItem;
	}
	
	public final static int countCheckBoxMenuItem(JMenu menu) {
		int count = 0;
		for (int i=0; i < menu.getMenuComponentCount(); i++) {
			Component c = menu.getMenuComponent(i);
			if (c instanceof JCheckBoxMenuItem) {
				count++;
			}
		}
		return count;
	}
	
	public final static JMenu getJMenu(JMenuBar menubar, String menuName) {
		JMenu theMenu = null;
		
		for (int i=0; i < menubar.getMenuCount(); i++) {
			JMenu m = menubar.getMenu(i);
			//menubar may have components that are not JMenu.
			//Therefore, m may be NULL. See: JMenubar.getMenu()
			if (m != null 
				&& m.getName().equals(menuName)) {
				theMenu = m;
				i = menubar.getMenuCount();//break
			}
		}
	
		return theMenu;
	}
	
	public final static WordMLTextPane getWordMLTextPane(JInternalFrame iframe) {
		return (WordMLTextPane) getDescendantOfClass(WordMLTextPane.class, iframe.getContentPane(), true);
	}
	
	public final static JEditorPane getSourceEditor(JInternalFrame iframe) {
		return (JEditorPane) getDescendantOfClass(JEditorPane.class, iframe.getContentPane(), true);
	}
	
    public final static Component getDescendantOfClass(Class<?> c, Container comp, boolean exactInstance) {
    	Component theObject = null;
    	
    	if (c != null && comp != null) {
			Component[] carray = comp.getComponents();
			if (carray != null) {
				for (int i = 0; i < carray.length && theObject == null; i++) {
					if (exactInstance && carray[i].getClass() == c) {
						theObject = carray[i];
					} else if (!exactInstance && c.isInstance(carray[i])) {
						theObject = carray[i];
					} else if (carray[i] instanceof Container) {
						theObject = getDescendantOfClass(c, (Container) carray[i], exactInstance);
					}
				}
			}
		}
    	
		return theObject;
    }
    
    public final static Rectangle getBounds(Shape s) {
    	//Prevent from constructing a new Rectangle object
    	//if Shape s is already a Rectangle
    	return (s instanceof Rectangle) ? (Rectangle) s : s.getBounds();
    }
    
    public final static List<View> getParagraphRowViews(View v) {
    	List<View> theList = new ArrayList<View>();
    	
    	boolean isRow = (v instanceof ImpliedParagraphView);
    	for (int i=0; i < v.getViewCount(); i++) {
    		View temp = v.getView(i);
    		if (isRow) {
    			theList.add(temp);
    		} else {
    			theList.addAll(getParagraphRowViews(temp));
    		}
    	}
    	
    	return theList;
    }
    
    public final static SdtBlockView getSdtBlockView(WordMLTextPane editor, int offset) {
		BasicTextUI ui = (BasicTextUI) editor.getUI();
		View v = ui.getRootView(editor).getView(0); //root
		int idx = v.getViewIndex(offset, Position.Bias.Forward);
		v = v.getView(idx);
		return (v instanceof SdtBlockView) ? (SdtBlockView) v : null;
    }

	private SwingUtil() {
		;//uninstantiable
	}
	
}// SwingUtil class



















