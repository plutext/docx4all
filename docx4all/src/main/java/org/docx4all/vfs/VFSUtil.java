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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.vfsjfilechooser.accessories.bookmarks.Bookmarks;
import net.sf.vfsjfilechooser.accessories.bookmarks.TitledURLEntry;
import net.sf.vfsjfilechooser.accessories.connection.Protocol;
import net.sf.vfsjfilechooser.utils.VFSURIParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *	@author Jojada Tirtowidjojo - 10/12/2008
 */
public class VFSUtil {
	private static Logger log = LoggerFactory.getLogger(VFSUtil.class);

	/**
	 * This method searches for all username and password pairs in bookmark entries
	 * that match uri parameter as well as that contained in default bookmark entry
	 * marked by defaultBookmarkEntryTitle parameter.
	 *  
	 * A bookmark entry matches uri parameter if its
	 * - protocol is the same as uri's
	 * - hostname is the same as uri's
	 * - path is the ancestor of uri's
	 * 
	 * @param uri
	 * @param defaultBookmarkEntryTitle
	 * @return a list of String objects in username:password syntax format.
	 */
	public final static List<String> collectUserCredentialsFromBookmark(
		VFSURIParser uri,
		String defaultBookmarkEntryTitle) {
		
		//The returned list of user credentials in username:password format.
		List<String> theList = new ArrayList<String>();
		
		//A temporary set used to ensure single entry.
		//We need this so that we can guarantee that the username and password
		//of default bookmark entry be the last entry in the returned list.
		Set<String> tempSet = new HashSet<String>();
				
		VFSURIParser defaultEntry = null;
		Bookmarks book = new Bookmarks();
				
		for (int i=0; i < book.getSize(); i++) {
			TitledURLEntry entry = book.getEntry(i);
			VFSURIParser temp = new VFSURIParser(entry.getURL(), false);
			if (uri.getProtocol() == temp.getProtocol()
				&& uri.getHostname().equals(temp.getHostname())
				&& uri.getPath().startsWith(temp.getPath())) {
				//The bookmark entry matches uri.
				//Put username:password in the top list.
				StringBuilder sb = new StringBuilder();
				sb.append(temp.getUsername());
				sb.append(":");
				sb.append(temp.getPassword());
				
				if (!tempSet.contains(sb.toString())) {
					tempSet.add(sb.toString());
					theList.add(sb.toString());
				}
			} else if (defaultBookmarkEntryTitle.equals(entry.getTitle())) {
				//identify this default bookmark entry
				//and add its username:password last in theList.
				defaultEntry = temp;
			}
		}

		if (defaultEntry != null) {
			//Put the username:password of default bookmark entry
			//as last entry in theList.
			StringBuilder sb = new StringBuilder();
			sb.append(defaultEntry.getUsername());
			sb.append(":");
			sb.append(defaultEntry.getPassword());
			if (!tempSet.contains(sb.toString())) {
				theList.add(sb.toString());
			}
		}
		
		return theList;
	}

	/**
	 * A Bookmark entry is a TitledURLEntry object.
	 * It consists of a title and uri location.
	 * 
	 * This method constructs a new TitledURLEntry from
	 * the passed parameters and adds it as a new entry if
	 * there has no entry with the same title. Existing entry
	 * with the same title will has its uri location replaced.
	 * 
	 * @param title
	 * @param uri
	 */
	public final static void addBookmarkEntry(String title, VFSURIParser uri) {
		Bookmarks book = new Bookmarks();
		TitledURLEntry tue = null;
		for (int i=0; i < book.getSize(); i++) {
			tue = book.getEntry(i);
			if (title.equalsIgnoreCase(tue.getTitle())) {
				i = book.getSize(); //break
			} else {
				tue = null;
			}
		}
	
		String entryPath = getVFSUrlPath(uri);
		if (tue == null) {
			book.add(new TitledURLEntry(title, entryPath));
			book.save();
		} else if (entryPath.equals(tue.getURL())){
			;//no change
		} else {
			tue.setURL(entryPath);
			book.save();
		}
	}
	
	/**
	 * Constructs a complete VFS Url path from the given uri parameter.
	 * 
	 * @param uri
	 * @return a complete VFS Url path.
	 */
	public final static String getVFSUrlPath(VFSURIParser uri) {
		StringBuilder thePath = new StringBuilder();
		
		if (uri.getProtocol() == Protocol.FILE) {
		    String os = System.getProperty("os.name").toLowerCase();
		    if (os.startsWith("windows")) {
		    	thePath.append("file:///");
		    } else {
		    	thePath.append("file://");
		    }
		    thePath.append(uri.getPath());
		} else {
			thePath.append(uri.getProtocol().getName().toLowerCase());
			thePath.append("://");
			if (uri.getUsername() != null && uri.getUsername().length() > 0) {
				thePath.append(uri.getUsername());
				if (uri.getPassword() != null && uri.getPassword().length() > 0) {
					thePath.append(":");
					thePath.append(uri.getPassword());
				}
			}
			thePath.append("@");
			thePath.append(uri.getHostname());
			if (uri.getPortnumber() != null && uri.getPortnumber().length() > 0) {
				thePath.append(":");
				thePath.append(uri.getPortnumber());
			}
			thePath.append(uri.getPath());
		}
		
		return thePath.toString();
	}
	
	private VFSUtil() {
		;//uninstantiable
	}
	
}// VFSUtil class



















