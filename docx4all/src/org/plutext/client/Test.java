package org.plutext.client;

import org.alfresco.webservice.util.AuthenticationUtils;

public class Test {

    protected static final String USERNAME = "admin";
    protected static final String PASSWORD = "admin";
	
    protected static final String docId = "/alfresco/plutextwebdav/User Homes/jharrop/Sunday13A.docx";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
        try {
        	
			// Start a new session
			AuthenticationUtils.startSession(USERNAME, PASSWORD);

			PlutextService_ServiceLocator locator = new PlutextService_ServiceLocator( AuthenticationUtils.getEngineConfiguration() );
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
