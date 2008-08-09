/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client.webservice;

public interface PlutextService_Service extends javax.xml.rpc.Service {
    public java.lang.String getPlutextServiceAddress();

    public org.plutext.client.webservice.PlutextWebService getPlutextService() throws javax.xml.rpc.ServiceException;

    public org.plutext.client.webservice.PlutextWebService getPlutextService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
