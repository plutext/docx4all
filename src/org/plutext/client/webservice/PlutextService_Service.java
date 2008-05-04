/**
 * PlutextService_Service.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.plutext.client.webservice;

public interface PlutextService_Service extends javax.xml.rpc.Service {
    public java.lang.String getPlutextServiceAddress();

    public org.plutext.client.webservice.PlutextWebService getPlutextService() throws javax.xml.rpc.ServiceException;

    public org.plutext.client.webservice.PlutextWebService getPlutextService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
