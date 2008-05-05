package org.plutext.transforms;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.docx4j.XmlUtils;

public class Test {
	
	// To test properly, make sure logging is enabled:
	//    -Dlog4j.configuration=conf/log4j.properties

	public final static String filename="/home/dev/workspace/docx4all/src/org/plutext/transforms/transforms-sample.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
        org.plutext.transforms.Transforms transformsObj = null;
		try {
			// Unmarshall
			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());					
			transformsObj = (org.plutext.transforms.Transforms)u.unmarshal( new java.io.File(filename) );
			System.out.println("Success!");
			
			// Manipulate
			for (Transforms.T t : transformsObj.t) {
				System.out.println(t.getSnum() + " : " + t.getOp() );
			}
			
			// Marshall again
			boolean suppressDeclaration = true;
			System.out.println( XmlUtils.marshaltoString(transformsObj, suppressDeclaration, Context.jcTransforms) ); 
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
}
