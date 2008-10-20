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

package org.docx4all.ui.menu.enabler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JMenuItem;
import javax.swing.event.CaretEvent;

/**
 *	@author Jojada Tirtowidjojo - 21/10/2008
 */
public abstract class CaretUpdateEnabler implements PropertyChangeListener {
	protected JMenuItem _menuItem;
	
	public CaretUpdateEnabler(JMenuItem item) {
		_menuItem = item;
	}

    public void propertyChange(PropertyChangeEvent evt) {
    	boolean b = isMenuEnabled((CaretEvent) evt.getNewValue());
    	this._menuItem.setEnabled(b);
    }

	protected abstract boolean isMenuEnabled(CaretEvent caretEvent);
	
}// CaretUpdateEnabler class



















