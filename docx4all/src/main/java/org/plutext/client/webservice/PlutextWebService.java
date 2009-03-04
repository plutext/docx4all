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

public interface PlutextWebService extends java.rmi.Remote {
    public java.lang.String[] transform(java.lang.String docID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String[][] getParts(java.lang.String docID, java.lang.String[] partNames) throws java.rmi.RemoteException;
    public java.lang.String[] getTransforms(java.lang.String docID, long firstSequenceNumber) throws java.rmi.RemoteException;
    public java.lang.String getSkeletonDocument(java.lang.String docID) throws java.rmi.RemoteException;
    public java.lang.String[] putMainDocumentPart(java.lang.String docID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String reportRecentChanges(java.lang.String docID) throws java.rmi.RemoteException;
    public java.lang.String reportVersionHistory(java.lang.String docID, java.lang.String chunkID) throws java.rmi.RemoteException;
    public java.lang.String injectPart(java.lang.String docID, java.lang.String partName, java.lang.String version, java.lang.String contentType, java.lang.String content) throws java.rmi.RemoteException;
    public boolean removePart(java.lang.String docID, java.lang.String partName, java.lang.String version) throws java.rmi.RemoteException;
}
