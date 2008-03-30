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

package org.docx4all.vfs;

import javax.swing.filechooser.FileView;

import net.sf.vfsjfilechooser.filechooser.VFSFileFilter;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;

/**
 *	@author Jojada Tirtowidjojo - 25/03/2008
 */
public class FileNameExtensionFilter extends VFSFileFilter {
	private final String filterDesc;
	private final String[] filteredFileExtensions;
	
	private final String[] lowerCaseExtensions;
	
	public FileNameExtensionFilter(String filterDesc, String... filteredFileExtensions) {
        if (filteredFileExtensions == null || filteredFileExtensions.length == 0) {
            throw new IllegalArgumentException(
                    "FilteredFileExtensions must NOT be null or empty");
        }
        
        this.filterDesc = filterDesc;
        this.filteredFileExtensions = new String[filteredFileExtensions.length];
        this.lowerCaseExtensions = new String[filteredFileExtensions.length];
        
        for (int i = 0; i < filteredFileExtensions.length; i++) {
            if (filteredFileExtensions[i] == null || filteredFileExtensions[i].length() == 0) {
                throw new IllegalArgumentException(
                    "Each FilteredFileExtension must NOT be null or empty");
            }
            this.filteredFileExtensions[i] = filteredFileExtensions[i];
            lowerCaseExtensions[i] = filteredFileExtensions[i].toLowerCase();
        }
    }
    
    /**
     * Whether the given file is accepted by this filter.
     * @param f
     * @return
     */
    public boolean accept(FileObject f) {
    	boolean isAccepted = false;
    	
        if (f != null) {
			if (VFSUtils.isDirectory(f)) {
				isAccepted = true;

			} else {
				String fext = f.getName().getExtension().toLowerCase();
				for (String extension : lowerCaseExtensions) {
					if (fext.equals(extension)) {
						isAccepted = true;
						break;
					}
				}
			}
		}
        
		return isAccepted;
    }

    /**
	 * The description of this filter. For example: "JPG and GIF Images"
	 * 
	 * @return
	 * @see FileView#getName
	 */
    public String getDescription() {
    	return this.filterDesc;
    }

    public String toString() {
    	return getDescription();
    }
    
}// FileNameExtensionFilter class



















