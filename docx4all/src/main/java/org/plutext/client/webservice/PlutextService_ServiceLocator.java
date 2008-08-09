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

public class PlutextService_ServiceLocator extends org.apache.axis.client.Service implements org.plutext.client.webservice.PlutextService_Service {

    public PlutextService_ServiceLocator() {
    }


    public PlutextService_ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public PlutextService_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for PlutextService
    private java.lang.String PlutextService_address = "http://plutext.org/alfresco/api/PlutextService";

    public java.lang.String getPlutextServiceAddress() {
        return PlutextService_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String PlutextServiceWSDDServiceName = "PlutextService";

    public java.lang.String getPlutextServiceWSDDServiceName() {
        return PlutextServiceWSDDServiceName;
    }

    public void setPlutextServiceWSDDServiceName(java.lang.String name) {
        PlutextServiceWSDDServiceName = name;
    }

    public org.plutext.client.webservice.PlutextWebService getPlutextService() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(PlutextService_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getPlutextService(endpoint);
    }

    public org.plutext.client.webservice.PlutextWebService getPlutextService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.plutext.client.webservice.PlutextServiceSoapBindingStub _stub = new org.plutext.client.webservice.PlutextServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getPlutextServiceWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setPlutextServiceEndpointAddress(java.lang.String address) {
        PlutextService_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.plutext.client.webservice.PlutextWebService.class.isAssignableFrom(serviceEndpointInterface)) {
                org.plutext.client.webservice.PlutextServiceSoapBindingStub _stub = new org.plutext.client.webservice.PlutextServiceSoapBindingStub(new java.net.URL(PlutextService_address), this);
                _stub.setPortName(getPlutextServiceWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("PlutextService".equals(inputPortName)) {
            return getPlutextService();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://server.plutext.org/", "PlutextService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://server.plutext.org/", "PlutextService"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("PlutextService".equals(portName)) {
            setPlutextServiceEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
