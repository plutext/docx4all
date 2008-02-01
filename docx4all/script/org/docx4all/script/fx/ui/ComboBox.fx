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

import org.docx4all.ui.main.ToolBarStates;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javafx.ui.ComboBox as JFXComboBox;

public class ComboBox extends JFXComboBox {
    attribute propertyNameToListen: String?;
    attribute swingAction:<<javax.swing.Action>>?;
    
    private operation setSwingAction(action:<<javax.swing.Action>>);
    private attribute stateListener: <<java.beans.PropertyChangeListener>>;
    private attribute firingActionEvent: Boolean;
}

trigger on ComboBox.swingAction = newAction {
    var self = this;
    ((<<javax.swing.JComboBox>>) getComponent()).addActionListener(
        new ActionListener() {
            operation actionPerformed(e: ActionEvent) {
                if (self.firingActionEvent) {
                    var selectedText = self.cells[self.selection].text;
                    var evt = 
                        new ActionEvent(e.getSource(), e.getID(), selectedText, e.getWhen(), e.getModifiers());
                    newAction.actionPerformed(evt);
                } else {
                    self.firingActionEvent = true;
                }
            }        
        }
    );
}

trigger on ComboBox.propertyNameToListen[oldName] = newName {
    //Java objects passed by ScriptEngine into scripting environment
    var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
    
    if (oldName <> null) {
        toolBarStates.removePropertyChangeListener((String) oldName, stateListener);
    }
    if (newName <> null) {
        toolBarStates.addPropertyChangeListener((String) newName, stateListener);
    }
}

trigger on (new ComboBox) {
    var self = this;
    this.firingActionEvent = true;
    this.stateListener =  new PropertyChangeListener {
        operation propertyChange(evt:PropertyChangeEvent) {
            var newSelectionText = (String) evt.getNewValue();
            var idx = 
                select indexof item from item in self.cells
                    where item.text == newSelectionText;
            self.firingActionEvent = false;                    
            self.selection = idx;
        }
    };// stateListener
}

























