/**
 * PlutextWebService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.plutext.client;

public interface PlutextWebService extends java.rmi.Remote {
    public java.lang.String[] getTransforms(java.lang.String docID, long firstSequenceNumber) throws java.rmi.RemoteException;
    public java.lang.String getSkeletonDocument(java.lang.String docID) throws java.rmi.RemoteException;
    public java.lang.String[] checkinWithComment(java.lang.String docID, java.lang.String chunkID, java.lang.String xml, java.lang.String message) throws java.rmi.RemoteException;
    public java.lang.String[] deleteChunk(java.lang.String docID, java.lang.String chunkID) throws java.rmi.RemoteException;
    public java.lang.String getVersionHistory(java.lang.String docID, java.lang.String chunkID) throws java.rmi.RemoteException;
    public java.lang.String[] style(java.lang.String docID, java.lang.String styleDefinitions) throws java.rmi.RemoteException;
    public java.lang.String getChunk(java.lang.String docID, java.lang.String chunkID, java.lang.String currentClientVersion) throws java.rmi.RemoteException;
}