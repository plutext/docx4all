/*
 *  Copyright 2008, Plutext Pty Ltd.
 *   
 *  This file is part of Plutext-Server.

    Plutext-Server is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU Affero General Public License 
    as published by the Free Software Foundation.

    Plutext-Server is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License   
    along with Plutext-Server.  If not, see <http://www.fsf.org/licensing/licenses/>.
    
 */

package org.plutext.transforms;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.plutext.transforms package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.plutext.transforms
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Transforms }
     * 
     */
    public Transforms createTransforms() {
        return new Transforms();
    }

    /**
     * Create an instance of {@link Updates }
     * 
     */
    public Updates createUpdates() {
        return new Updates();
    }

    /**
     * Create an instance of {@link Changesets }
     * 
     */
    public Changesets createChangesets() {
        return new Changesets();
    }

    /**
     * Create an instance of {@link Transforms.T }
     * 
     */
    public Transforms.T createTransformsT() {
        return new Transforms.T();
    }

    /**
     * Create an instance of {@link Changesets.Changeset }
     * 
     */
    public Changesets.Changeset createChangesetsChangeset() {
        return new Changesets.Changeset();
    }

}
