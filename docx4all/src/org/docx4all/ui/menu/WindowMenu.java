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

package org.docx4all.ui.menu;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.util.SwingUtil;
import org.jdesktop.application.Action;

/**
 *	@author Jojada Tirtowidjojo - 29/11/2007
 */
public class WindowMenu extends UIMenu {
	private final static WindowMenu _instance = new WindowMenu();
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure Window Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Window menu in the menu bar
	 */
	public final static String WINDOW_MENU_NAME = "windowMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Spring Application Framework
	
	/**
	 * The action name of Window Tiled menu
	 */
	public final static String WINDOW_TILED_ACTION_NAME = "windowTiled";
	
	/**
	 * The action name of Window Cascaded menu
	 */
	public final static String WINDOW_CASCADED_ACTION_NAME = "windowCascaded";
	
	private static final String[] _menuItemActionNames = {
		WINDOW_CASCADED_ACTION_NAME,
		WINDOW_TILED_ACTION_NAME
	};
	
	public static WindowMenu getInstance() {
		return _instance;
	}
	
	private WindowMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return WINDOW_MENU_NAME;
	}
	
	public void addWindowMenuItem(JInternalFrame iframe) {
		JMenuItem item = SwingUtil.createCheckBoxMenuItem(iframe);
		
		JMenu wmenu = getWindowJMenu();
		if (SwingUtil.countCheckBoxMenuItem(wmenu) == 0) {
			wmenu.add(new JSeparator());
		}
		wmenu.add(item);
	}
	
	public void removeWindowMenuItem(JInternalFrame iframe) {
		JMenu wmenu = getWindowJMenu();
		
		int idx = SwingUtil.getMenuItemIndex(wmenu, iframe.getTitle());
		if (idx > -1) {
			JMenuItem mi = (JMenuItem) wmenu.getMenuComponent(idx);
			wmenu.remove(mi);
			
			if (SwingUtil.countCheckBoxMenuItem(wmenu) == 0) {
				Component c = wmenu.getMenuComponent(idx - 1);
				if (c instanceof JSeparator) {
					wmenu.remove(c);
				}
			}
		}
	}
	
	public void selectWindowMenuItem(JInternalFrame iframe) {
		JMenu wmenu = getWindowJMenu();
		JMenuItem item = SwingUtil.getMenuItem(wmenu, iframe.getTitle());
		item.setSelected(true);
	}
	
	public void unSelectWindowMenuItem(JInternalFrame iframe) {
		JMenu wmenu = getWindowJMenu();
		JMenuItem item = SwingUtil.getMenuItem(wmenu, iframe.getTitle());
		item.setSelected(false);
	}
	
	private JMenu getWindowJMenu() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		JMenuBar menubar = editor.getMainFrame().getJMenuBar();
		return SwingUtil.getJMenu(menubar, WindowMenu.getInstance().getMenuName());
	}
	
	@Action public void windowTiled() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        JDesktopPane desktop = editor.getDesktopPane();
		
        JInternalFrame[] allFrames = desktop.getAllFrames();
        int rows = (int) Math.sqrt(allFrames.length);
        int cols = rows;
        
        if (rows * cols < allFrames.length) {
            cols += 1;
            if (rows * cols < allFrames.length) {
                rows += 1;
            }
        }
        
        Dimension size = desktop.getSize();
        
        int w = (int) (size.width / cols);
        int h = (int) (size.height/ rows);
        int x = 0;
        int y = 0;
        
        for (int i=0; i < rows; i++) {
        	for (int j=0; j < cols; j++) {
                int cell = (i * cols) + j;
                if (cell >= allFrames.length) {
                    break;
                }
                
                JInternalFrame iframe = allFrames[cell];
                try {
                	iframe.setVisible(true);
                	iframe.setIcon(false);
                	iframe.setMaximum(false);
                } catch (PropertyVetoException exc) {
                	;//do nothing
                }
                desktop.getDesktopManager().setBoundsForFrame(iframe, x, y, w, h);
                x += w;
            }
            y += h;
            x = 0;
        }
	}
	
	@Action public void windowCascaded() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        JDesktopPane desktop = editor.getDesktopPane();
        
        JInternalFrame[] allFrames = desktop.getAllFrames();
        int x = 0;
        int y = 0;
        int w = 0;
        int h = desktop.getHeight();
        
        int d = (int) (h / allFrames.length);
        if (d < 15) {
            d = 15;
        }
        
        for (int i = allFrames.length - 1; 0 <= i; i--) {
            try {
                allFrames[i].setVisible(true);
                allFrames[i].setIcon(false);
                allFrames[i].setMaximum(false);
            } catch (PropertyVetoException exc) {
            	;//do nothing
            }
            
            Dimension dim = allFrames[i].getSize();
            w = dim.width;
            h = dim.height;
            desktop.getDesktopManager().setBoundsForFrame(allFrames[i], x, y, w, h);
            x += d;
            y += d;
        }
	}

 }// WindowMenu class



















