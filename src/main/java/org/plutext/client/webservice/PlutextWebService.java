/**
 * PlutextWebService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.plutext.client.webservice;

public interface PlutextWebService extends java.rmi.Remote {
    public java.lang.String[] getTransforms(java.lang.String docID, long firstSequenceNumber) throws java.rmi.RemoteException;
    public java.lang.String getSkeletonDocument(java.lang.String docID) throws java.rmi.RemoteException;
    public java.lang.String[] putMainDocumentPart(java.lang.String docID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String checkinWithComment(java.lang.String docID, java.lang.String chunkID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String deleteChunk(java.lang.String docID, java.lang.String chunkID) throws java.rmi.RemoteException;
    public java.lang.String getVersionHistory(java.lang.String docID, java.lang.String chunkID) throws java.rmi.RemoteException;
    public java.lang.String compareVersions(java.lang.String docID, java.lang.String chunkID, java.lang.String left, java.lang.String right) throws java.rmi.RemoteException;
    public java.lang.String[] transform(java.lang.String docID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String getChunk(java.lang.String docID, java.lang.String chunkID, java.lang.String currentClientVersion) throws java.rmi.RemoteException;
}
