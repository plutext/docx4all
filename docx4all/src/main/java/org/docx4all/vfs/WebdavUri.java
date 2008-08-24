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

/**
 *	@author Jojada Tirtowidjojo - 25/08/2008
 */
public class WebdavUri {
	public final static String WEBDAV_SCHEME = "webdav";
	
	private String _uri;
	private String _username;
	private String _password;
	private String _host;
	private String _port;
	private String _absolutePath;
	
	public WebdavUri(String vfsWebdavUri) {
		//The VFS Webdav Uri format is as follows:
		//webdav://[ username [: password ]@] hostname [: port ][ absolute-path ]
		if (!vfsWebdavUri.startsWith(WEBDAV_SCHEME + "://")) {
			throw new IllegalArgumentException("Not a webdav uri");
		}
		parse(vfsWebdavUri);
	}
	
	public String getScheme() {
		return WEBDAV_SCHEME;
	}
	
	public String getUri() {
		return _uri;
	}
	
	public String getUsername() {
		return _username;
	}
	
	public String getPassword() {
		return _password;
	}
	
	public String getHost() {
		return _host;
	}
	
	public String getPort() {
		return _port;
	}
	
	public String getAbsolutePath() {
		return _absolutePath;
	}
	
	private void parse(String vfsWebdavUri) {
		//The VFS Webdav Uri format is as follows:
		//webdav://[ username [: password ]@] hostname [: port ][ absolute-path ]
		_uri = vfsWebdavUri;
		
		int idx = _uri.lastIndexOf('@');
		if (idx == -1) {
			//if no username and no password
			//Seek port number.
			parseHostPortAbsolutePath(_uri.substring(9));
		} else {
			String right = _uri.substring(idx + 1);
			String left = _uri.substring(9, idx);
			
			parseUsernamePassword(left);
			parseHostPortAbsolutePath(right);
		}
	}
	
	private void parseUsernamePassword(String s) {
		int idx = s.indexOf(':');
		if (idx == -1) {
			//has no password
			_username = s;
		} else {
			_username = s.substring(0, idx);
			_password = s.substring(idx + 1);
		}
	}
	
	private void parseHostPortAbsolutePath(String s) {
		//Argument s syntax is hostname [: port ][ absolute-path ]
		int idx = s.indexOf(':');
		if (idx == -1) {
			//if no port number
			int slash = s.indexOf('/');
			if (slash == -1) {
				//no absolute path
				_host = s;
			} else {
				_host = s.substring(0, slash);
				_absolutePath = s.substring(slash);
			}
		} else {
			//has port number
			_host = s.substring(0, idx);
			
			int slash = s.indexOf('/', idx);
			if (slash == -1) {
				//no absolute path
				_port = s.substring(idx + 1);
			} else {
				_port = s.substring(idx + 1, slash);
				_absolutePath = s.substring(slash);
			}
		}
	}
	
}// WebdavUri class



















