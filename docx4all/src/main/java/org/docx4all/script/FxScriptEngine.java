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

import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 14/11/2007
 */
public class FxScriptEngine {
	private static Logger log = LoggerFactory.getLogger(FxScriptEngine.class);
	
	private String _scriptDir;

	public FxScriptEngine() {
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
    	ResourceMap rm = editor.getContext().getResourceMap(FxScript.class);
    	//Init Script Directory
    	setScriptDirectory(rm.getString(Constants.FXSCRIPT_LOCATION));
	}
	
	/**
	 * Sets the directory of script files.
	 * Script files will be resolved under this directory.
	 * 
	 * @param dir The directory of script files
	 */
	public void setScriptDirectory(String dir) {
		if (dir != null && dir.length() > 0) {
			if (dir.startsWith("/")) {
				_scriptDir = new String(dir);
			} else {
				_scriptDir = "/" + dir;
			}
			if (!_scriptDir.endsWith("/")) {
				_scriptDir = dir + "/";
			}
		} else {
			_scriptDir = "/";
		}
	}
	
	/**
	 * Gets the directory under which script files will be resolved.
	 * 
	 * @return The directory of script files
	 */
	public String getScriptDirectory() {
		return _scriptDir;
	}
	
    /**
     * Executes the specified script file.
     * Call to this method is the same as call to 
     * <code>run(FxScript.Path path, (Map) null)</code> 
     * 
     * @see run(FxScript.Path path, Map params)
     */
	public Object run(FxScript.Path path) {
		return run(path, null);
	}
	
    /**
     * Puts key/value pairs specified in 'params' into scripting environment 
     * and executes the specified script file
     * 
     * @param filepath The path to script file. 
     * This path should be relative to script directory.  
     * @param params A Map object whose key-value pairs will be put 
     * into scripting environment
     * @return The value returned from the execution of the script 
     * 
     * @throws RuntimeException if error occurs in script.
     * @throws NullPointerException if the <code>path</code> argument is null.
     * @throws IllegalArgumentException if <code>path</code> argument 
     * does not resolve to a .fx file.
     */
	public Object run(FxScript.Path path, Map<String, Object> params) {
		Object theObject = null;
		
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
    	ResourceMap rm = editor.getContext().getResourceMap(FxScript.class);
    	String filepath = rm.getString(path.toString());
    	filepath = resolvePath(filepath);
		
		if (log.isDebugEnabled()) {
			log.debug("filepath = " + filepath);
		}
		
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByExtension("fx");
        setBindings(engine, params);
        
        InputStreamReader reader = 
            new InputStreamReader(FxScriptEngine.class.getResourceAsStream(filepath));
        try {
        	theObject = engine.eval(reader);
        } catch (ScriptException exc) {
        	throw new RuntimeException(exc);
        }
        return theObject;
	}
	
	private String resolvePath(String s) {
		String thePath = null;
		
		if (s.startsWith("/")) {
			thePath = s.substring(1);
		} else {
			thePath = new String(s);
		}
		
        if (!thePath.endsWith(".fx")) {
        	throw new IllegalArgumentException("Not a .fx file");
        }
		
        return getScriptDirectory() + thePath;
	}
    
	protected void setBindings(ScriptEngine engine, Map<String,?> params) {
		if (params != null && params.size() > 0) {
			for (Iterator<String> i = params.keySet().iterator(); i.hasNext(); ) {
				String key = i.next();
				Object value = params.get(key) ;
				
				if (log.isDebugEnabled()) {
					log.debug("Binding key=" + key);
					log.debug("Binding value=" + value);
				}
				
				engine.put(key, value) ;
			}
		}
	}

}// FxScriptEngine class



















