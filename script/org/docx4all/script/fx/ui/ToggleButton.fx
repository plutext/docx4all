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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.text.StyleConstants;

import javafx.ui.ToggleButton as JFXToggleButton;
import javafx.ui.Image;

public class ToggleButton extends JFXToggleButton {
    attribute enabledPropertyName: String?;
    attribute swingAction:<<javax.swing.Action>>?;

    private operation setSwingAction(action:<<javax.swing.Action>>);
    private attribute stateListener: <<java.beans.PropertyChangeListener>>;
}

operation ToggleButton.setSwingAction(action:<<javax.swing.Action>>) {
    if (action <> null) {
        var actionIcon = (<<javax.swing.ImageIcon>>) action.getValue(action.SMALL_ICON);
        var actionSelectedIcon = 
            (<<javax.swing.ImageIcon>>) action.getValue("Selected{action.SMALL_ICON}");
        var actionTooltipText = (String) action.getValue(action.SHORT_DESCRIPTION);
    
        icon = Image {
            url: actionIcon.getDescription()
        };
    
        selectedIcon= Image {
            url: actionSelectedIcon.getDescription()
        };
        
        toolTipText = actionTooltipText;

        ((<<javax.swing.JToggleButton>>) getComponent()).setAction(action);
        ((<<javax.swing.JToggleButton>>) getComponent()).setHideActionText(true);
    }
}

trigger on ToggleButton.swingAction = newAction {
    setSwingAction(newAction);
}

trigger on ToggleButton.enabledPropertyName[oldName] = newName {
    //Java objects passed by ScriptEngine into scripting environment
    var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
    
    if (oldName <> null) {
        toolBarStates.removePropertyChangeListener((String) oldName, stateListener);
    }
    if (newName <> null) {
        toolBarStates.addPropertyChangeListener((String) newName, stateListener);
    }
}

trigger on new ToggleButton {
    setSwingAction(swingAction);
    
    var self = this;
    this.stateListener =  new PropertyChangeListener {
        operation propertyChange(evt:PropertyChangeEvent) {
            //Java objects passed by ScriptEngine into scripting environment
            var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;

            if (toolBarStates.ALIGNMENT_PROPERTY_NAME == evt.getPropertyName()) {
                var newValue = ((Integer) evt.getNewValue()).intValue();
            
                //Java objects passed by ScriptEngine into scripting environment
                var fm = formatMenu:<<org.docx4all.ui.menu.FormatMenu>>;
            
                if (fm.getAction(fm.ALIGN_LEFT_ACTION_NAME) == self.swingAction
                    and newValue == StyleConstants.ALIGN_LEFT) {
                    self.selected = true;
                
                } else if (fm.getAction(fm.ALIGN_CENTER_ACTION_NAME) == self.swingAction
                        and newValue == StyleConstants.ALIGN_CENTER) {
                    self.selected = true;
                
                } else if (fm.getAction(fm.ALIGN_RIGHT_ACTION_NAME) == self.swingAction
                        and newValue == StyleConstants.ALIGN_RIGHT) {
                    self.selected = true;
                    
                } else if (fm.getAction(fm.ALIGN_JUSTIFIED_ACTION_NAME) == self.swingAction
                        and newValue == StyleConstants.ALIGN_JUSTIFIED) {
                    self.selected = true;
                }
            } else {
                self.selected = ((Boolean) evt.getNewValue()).booleanValue();
            }
        }
    };// stateListener
    
    if (enabledPropertyName <> null) {
        //Java objects passed by ScriptEngine into scripting environment
        var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
        toolBarStates.addPropertyChangeListener((String) enabledPropertyName, this.stateListener);
    }
    
}















