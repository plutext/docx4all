package org.plutext.client;

import org.alfresco.webservice.util.AuthenticationUtils;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;

public class Test {

    protected static final String USERNAME = "admin";
    protected static final String PASSWORD = "admin";
	
    protected static final String docId = "/alfresco/plutextwebdav/User Homes/jharrop/Sunday13A.docx";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
        try {
			System.out.println("Using endpoint address: " + org.alfresco.webservice.util.WebServiceFactory.getEndpointAddress()  );
        	
			// Start a new session
			AuthenticationUtils.startSession(USERNAME, PASSWORD);

			PlutextService_ServiceLocator locator = new PlutextService_ServiceLocator( AuthenticationUtils.getEngineConfiguration() );
			locator.setPlutextServiceEndpointAddress(org.alfresco.webservice.util.WebServiceFactory.getEndpointAddress() + "/" + locator.getPlutextServiceWSDDServiceName() );			
			PlutextWebService service  = locator.getPlutextService();
			System.out.println(
					service.getSkeletonDocument(docId) );			
			
			// End the current session
			AuthenticationUtils.endSession();
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}

	}	

}
